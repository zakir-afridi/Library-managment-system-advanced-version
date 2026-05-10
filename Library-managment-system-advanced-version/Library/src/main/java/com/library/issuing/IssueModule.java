package com.library.issuing;

import com.library.model.Transaction;
import com.library.service.TransactionService;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

import java.util.List;

/**
 * ISSUING BRANCH — public API.
 */
public class IssueModule {

    private static final TransactionService service = new TransactionService();

    // ── UI Navigation ─────────────────────────────────────────────────────────

    public static void showIssueView(Pane container) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    IssueModule.class.getResource("/com/library/ui/IssueReturnBooksForm.fxml"));
            Pane view = loader.load();
            container.getChildren().setAll(view);
        } catch (Exception e) {
            System.err.println("IssueModule.showIssueView: " + e.getMessage());
        }
    }

    public static void showHistory(Pane container) {
        // History is part of IssueReturnBooksForm — same view, different tab
        showIssueView(container);
    }

    // ── Data API ──────────────────────────────────────────────────────────────

    /** Issue a book. Returns empty string on success, error message on failure. */
    public static String issueBook(int bookId, int memberId, String issuedBy) {
        return service.issueBook(bookId, memberId, issuedBy);
    }

    /** Return a book. Returns fine amount, or -1 on error. */
    public static double returnBook(int transactionId, String condition, String returnedBy) {
        return service.returnBook(transactionId, condition, returnedBy);
    }

    public static List<Transaction> getActive()                    { return service.getActiveTransactions(); }
    public static List<Transaction> getOverdue()                   { return service.getOverdueTransactions(); }
    public static List<Transaction> getOverdueSorted()             { return service.getOverdueSortedByDays(); }
    public static List<Transaction> getForMember(int memberId)     { return service.getMemberTransactions(memberId); }
    public static List<Transaction> getRecentActivity(int limit)   { return service.getRecentActivity(limit); }
    public static int getActiveCount()                             { return service.getActiveTransactions().size(); }
    public static int getOverdueCount()                            { return service.getOverdueTransactions().size(); }
}
