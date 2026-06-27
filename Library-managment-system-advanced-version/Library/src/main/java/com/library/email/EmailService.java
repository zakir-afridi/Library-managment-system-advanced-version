package com.library.email;

import com.library.config.AppConfig;
import com.library.database.HikariConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Email service using Gmail SMTP (or any SMTP server).
 *
 * Configuration is stored in AppConfig / libra_config.properties:
 *   email.smtp.host     = smtp.gmail.com
 *   email.smtp.port     = 587
 *   email.smtp.user     = you@gmail.com
 *   email.smtp.password = app-password
 *   email.from.name     = LibraCore Pro Library
 *
 * Outgoing emails are queued in the email_queue SQLite table.
 * A virtual-thread background processor sends them with retries (max 3).
 *
 * If credentials are not configured, all calls silently return false.
 */
public class EmailService {

    private static final Logger LOG = LoggerFactory.getLogger(EmailService.class);

    /** Virtual-thread executor for async sending. */
    private final ExecutorService executor =
        Executors.newVirtualThreadPerTaskExecutor();

    private static EmailService instance;

    private EmailService() {}

    public static synchronized EmailService getInstance() {
        if (instance == null) instance = new EmailService();
        return instance;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Queue an email for delivery. Returns true if successfully queued.
     * The email will be sent asynchronously via a virtual thread.
     */
    public boolean sendAsync(String to, String subject, String htmlBody) {
        if (!isConfigured()) {
            LOG.warn("Email not configured — skipping email to {}", to);
            return false;
        }
        enqueue(to, subject, htmlBody);
        executor.submit(this::processQueue);
        return true;
    }

    /**
     * Send an overdue reminder email to a member.
     */
    public void sendOverdueReminder(String memberEmail, String memberName,
                                     String bookTitle, String dueDate, double fine) {
        if (memberEmail == null || memberEmail.isBlank()) return;
        String subject = "Overdue Book Reminder — " + bookTitle;
        String body = EmailTemplates.overdueReminder(memberName, bookTitle, dueDate, fine,
                AppConfig.getInstance().getCurrency(),
                AppConfig.getInstance().getLibraryName());
        sendAsync(memberEmail, subject, body);
    }

    /**
     * Send a welcome email to a newly registered member.
     */
    public void sendWelcome(String memberEmail, String memberName, String memberId) {
        if (memberEmail == null || memberEmail.isBlank()) return;
        String subject = "Welcome to " + AppConfig.getInstance().getLibraryName();
        String body = EmailTemplates.welcome(memberName, memberId,
                AppConfig.getInstance().getLibraryName());
        sendAsync(memberEmail, subject, body);
    }

    // ── Configuration check ───────────────────────────────────────────────────

    public boolean isConfigured() {
        AppConfig cfg = AppConfig.getInstance();
        String user = cfg.get("email.smtp.user");
        String pass = cfg.get("email.smtp.password");
        return user != null && !user.isBlank() &&
               pass != null && !pass.isBlank();
    }

    // ── Queue management ──────────────────────────────────────────────────────

    private void enqueue(String to, String subject, String htmlBody) {
        String sql = """
            INSERT INTO email_queue (recipient, subject, body, status, attempts, created_at)
            VALUES (?, ?, ?, 'PENDING', 0, datetime('now'))
        """;
        try (Connection c = HikariConnectionPool.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, to);
            ps.setString(2, subject);
            ps.setString(3, htmlBody);
            ps.executeUpdate();
        } catch (Exception e) {
            LOG.error("Failed to enqueue email: {}", e.getMessage());
        }
    }

    private void processQueue() {
        String sql = """
            SELECT queue_id, recipient, subject, body, attempts
            FROM email_queue
            WHERE status = 'PENDING' AND attempts < 3
            ORDER BY created_at ASC
            LIMIT 10
        """;
        try (Connection c = HikariConnectionPool.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int    id        = rs.getInt("queue_id");
                String to        = rs.getString("recipient");
                String subject   = rs.getString("subject");
                String body      = rs.getString("body");
                int    attempts  = rs.getInt("attempts");
                boolean ok = trySend(to, subject, body);
                updateQueueStatus(c, id, ok ? "SENT" : (attempts + 1 >= 3 ? "FAILED" : "PENDING"), attempts + 1);
            }
        } catch (Exception e) {
            LOG.error("Email queue processing error: {}", e.getMessage());
        }
    }

    private void updateQueueStatus(Connection c, int id, String status, int attempts) {
        String sql = "UPDATE email_queue SET status=?, attempts=?, updated_at=datetime('now') WHERE queue_id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, attempts);
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (Exception e) {
            LOG.error("Failed to update email queue: {}", e.getMessage());
        }
    }

    // ── SMTP sending ──────────────────────────────────────────────────────────

    private boolean trySend(String to, String subject, String htmlBody) {
        AppConfig cfg = AppConfig.getInstance();
        String host  = cfg.get("email.smtp.host");
        String port  = cfg.get("email.smtp.port");
        String user  = cfg.get("email.smtp.user");
        String pass  = cfg.get("email.smtp.password");
        String name  = cfg.get("email.from.name");

        if (host == null) host = "smtp.gmail.com";
        if (port == null) port = "587";
        if (name == null) name = AppConfig.getInstance().getLibraryName();

        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            host);
        props.put("mail.smtp.port",            port);
        props.put("mail.smtp.ssl.trust",       "*");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout",           "5000");

        final String finalUser = user;
        final String finalPass = pass;

        try {
            Session session = Session.getInstance(props, new Authenticator() {
                @Override protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(finalUser, finalPass);
                }
            });

            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(user, name));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            msg.setSubject(subject, "UTF-8");

            // HTML + plain-text multipart
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");

            MimeMultipart multipart = new MimeMultipart("alternative");
            multipart.addBodyPart(htmlPart);
            msg.setContent(multipart);

            Transport.send(msg);
            LOG.info("Email sent to {}: {}", to, subject);
            return true;

        } catch (Exception e) {
            LOG.warn("Email send failed to {}: {}", to, e.getMessage());
            return false;
        }
    }
}
