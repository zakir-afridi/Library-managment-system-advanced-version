package com.library.service;

import com.library.config.AppConfig;
import com.library.database.DatabaseConnection;
import com.library.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background service that sends overdue reminder emails.
 *
 * Runs every hour. Queries all overdue transactions (status='Issued', due_date < today)
 * and sends a reminder email to each member who has an email address on file.
 *
 * Prevents duplicate sends by tracking sent notifications in the activity_log.
 *
 * Usage:
 *   OverdueNotificationService.getInstance().start();
 *   OverdueNotificationService.getInstance().stop();
 */
public class OverdueNotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(OverdueNotificationService.class);

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "overdue-notifier");
            t.setDaemon(true);
            return t;
        });

    private static OverdueNotificationService instance;

    private OverdueNotificationService() {}

    public static synchronized OverdueNotificationService getInstance() {
        if (instance == null) instance = new OverdueNotificationService();
        return instance;
    }

    /** Start the hourly overdue notification scheduler. */
    public void start() {
        if (!AppConfig.getInstance().isOverdueAlert()) {
            LOG.info("Overdue notifications disabled in config.");
            return;
        }
        if (!EmailService.getInstance().isConfigured()) {
            LOG.info("Email not configured — overdue notifications disabled.");
            return;
        }
        // Run 2 min after startup, then every hour
        scheduler.scheduleAtFixedRate(
            this::processOverdue,
            2, 60, TimeUnit.MINUTES
        );
        LOG.info("OverdueNotificationService started — runs every 60 minutes.");
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    // ── Core logic ────────────────────────────────────────────────────────────

    private void processOverdue() {
        LOG.info("OverdueNotificationService: checking for overdue books...");
        String sql = """
            SELECT t.transaction_id, t.due_date, t.fine_amount,
                   b.book_name,
                   m.name as member_name, m.email
            FROM transactions t
            JOIN books b   ON t.book_id   = b.book_id
            JOIN members m ON t.member_id = m.std_id
            WHERE t.status = 'Issued'
              AND t.due_date < date('now')
              AND m.email IS NOT NULL
              AND m.email != ''
              AND t.transaction_id NOT IN (
                  SELECT CAST(details AS INTEGER) FROM activity_log
                  WHERE action = 'OVERDUE_EMAIL'
                    AND timestamp >= date('now', '-1 day')
              )
        """;

        int sent = 0;
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {

            AppConfig cfg = AppConfig.getInstance();

            while (rs.next()) {
                int    txId       = rs.getInt("transaction_id");
                String dueDate    = rs.getString("due_date");
                double fine       = rs.getDouble("fine_amount");
                String bookTitle  = rs.getString("book_name");
                String memberName = rs.getString("member_name");
                String email      = rs.getString("email");

                EmailService.getInstance()
                    .sendOverdueReminder(email, memberName, bookTitle, dueDate, fine);

                logEmailSent(c, txId);
                sent++;
            }
        } catch (SQLException e) {
            LOG.error("OverdueNotificationService error: {}", e.getMessage());
        }

        if (sent > 0) LOG.info("Overdue reminders sent: {}", sent);
    }

    private void logEmailSent(Connection c, int txId) {
        String sql = "INSERT INTO activity_log (action, details) VALUES ('OVERDUE_EMAIL', ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(txId));
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warn("Failed to log email notification: {}", e.getMessage());
        }
    }
}
