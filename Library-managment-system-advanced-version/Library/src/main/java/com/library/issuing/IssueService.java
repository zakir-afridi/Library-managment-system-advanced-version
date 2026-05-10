package com.library.issuing;

import com.library.model.Transaction;
import com.library.service.TransactionService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * ISSUING BRANCH — service layer.
 */
public class IssueService {

    private final TransactionService delegate = new TransactionService();

    public String issueBook(int bookId, int memberId, String issuedBy) {
        return delegate.issueBook(bookId, memberId, issuedBy);
    }

    public double returnBook(int transactionId, String condition, String returnedBy) {
        return delegate.returnBook(transactionId, condition, returnedBy);
    }

    public List<Transaction> getActive()                       { return delegate.getActiveTransactions(); }
    public List<Transaction> getOverdue()                      { return delegate.getOverdueTransactions(); }
    public List<Transaction> getOverdueSorted()                { return delegate.getOverdueSortedByDays(); }
    public List<Transaction> getForMember(int memberId)        { return delegate.getMemberTransactions(memberId); }
    public List<Transaction> getRecentActivity(int limit)      { return delegate.getRecentActivity(limit); }
    public Map<String, Integer> getMonthlyStats(int months)    { return delegate.getMonthlyIssuedStats(months); }
    public Map<String, int[]> getDailyStats(int days)          { return delegate.getDailyStats(days); }

    public List<Transaction> getByDateRange(LocalDate from, LocalDate to) {
        return delegate.getTransactionsByDateRange(from, to);
    }
}
