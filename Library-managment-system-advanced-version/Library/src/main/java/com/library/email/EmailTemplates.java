package com.library.email;

/**
 * Simple HTML email templates for LibraCore Pro.
 * All templates use inline CSS for maximum email client compatibility.
 */
public final class EmailTemplates {

    private EmailTemplates() {}

    /** Overdue book reminder template. */
    public static String overdueReminder(String memberName, String bookTitle,
                                          String dueDate, double fine,
                                          String currency, String libraryName) {
        return html(libraryName, "📚 Overdue Book Reminder", """
            <p style='font-size:16px;'>Dear <strong>%s</strong>,</p>
            <p>This is a friendly reminder that the following book is <strong style='color:#d32f2f;'>overdue</strong>:</p>
            <table style='border-collapse:collapse;width:100%%;margin:16px 0;'>
              <tr style='background:#f5f5f5;'>
                <td style='padding:10px;border:1px solid #ddd;'><strong>Book</strong></td>
                <td style='padding:10px;border:1px solid #ddd;'>%s</td>
              </tr>
              <tr>
                <td style='padding:10px;border:1px solid #ddd;'><strong>Due Date</strong></td>
                <td style='padding:10px;border:1px solid #ddd;color:#d32f2f;'>%s</td>
              </tr>
              <tr style='background:#fff3e0;'>
                <td style='padding:10px;border:1px solid #ddd;'><strong>Accrued Fine</strong></td>
                <td style='padding:10px;border:1px solid #ddd;color:#e65100;'><strong>%s %.2f</strong></td>
              </tr>
            </table>
            <p>Please return the book as soon as possible to avoid additional fines.</p>
            <p style='color:#666;'>If you have already returned it, please disregard this message.</p>
            """.formatted(memberName, bookTitle, dueDate, currency, fine));
    }

    /** Welcome email for new members. */
    public static String welcome(String memberName, String memberId, String libraryName) {
        return html(libraryName, "Welcome to " + libraryName, """
            <p style='font-size:16px;'>Dear <strong>%s</strong>,</p>
            <p>Welcome to <strong>%s</strong>! Your library membership has been successfully registered.</p>
            <table style='border-collapse:collapse;width:100%%;margin:16px 0;'>
              <tr style='background:#e3f2fd;'>
                <td style='padding:10px;border:1px solid #ddd;'><strong>Member ID</strong></td>
                <td style='padding:10px;border:1px solid #ddd;font-family:monospace;font-size:15px;'>%s</td>
              </tr>
            </table>
            <p>You can borrow up to 5 books at a time. Happy reading!</p>
            """.formatted(memberName, libraryName, memberId));
    }

    /** Common HTML wrapper with header and footer. */
    private static String html(String libraryName, String heading, String body) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family:Arial,sans-serif;background:#f9f9f9;margin:0;padding:0;">
              <div style="max-width:600px;margin:30px auto;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 2px 10px rgba(0,0,0,0.1);">
                <!-- Header -->
                <div style="background:#1976d2;color:#fff;padding:24px 30px;">
                  <h1 style="margin:0;font-size:22px;">%s</h1>
                  <p style="margin:4px 0 0;opacity:0.85;font-size:14px;">Library Management System</p>
                </div>
                <!-- Heading -->
                <div style="padding:24px 30px 0;">
                  <h2 style="color:#1976d2;border-bottom:2px solid #e3f2fd;padding-bottom:10px;">%s</h2>
                </div>
                <!-- Body -->
                <div style="padding:10px 30px 30px;color:#333;line-height:1.6;">
                  %s
                </div>
                <!-- Footer -->
                <div style="background:#f5f5f5;padding:16px 30px;text-align:center;color:#888;font-size:12px;border-top:1px solid #e0e0e0;">
                  This email was sent by %s &mdash; LibraCore Pro v3.0.0
                </div>
              </div>
            </body>
            </html>
            """.formatted(libraryName, heading, body, libraryName);
    }
}
