package com.library.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.library.config.AppConfig;
import com.library.database.DatabaseConnection;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Transaction;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates PDF and CSV reports using OpenPDF.
 */
public class ReportService {
    private static final Logger LOG = LoggerFactory.getLogger(ReportService.class);

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BookService        bookService   = new BookService();
    private final MemberService      memberService = new MemberService();
    private final TransactionService txService     = new TransactionService();

    // â”€â”€ PDF: Overdue Report â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void generateOverdueReport(OutputStream out) throws DocumentException {
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, out);
        doc.open();

        addHeader(doc, "Overdue Books Report",
                "Generated: " + LocalDate.now().format(FMT));

        List<Transaction> overdue = txService.getOverdueSortedByDays();

        PdfPTable table = createTable(new String[]{
                "Member", "Student ID", "Book", "Issue Date", "Due Date", "Days Overdue", "Fine"
        }, new float[]{2f, 1.5f, 2.5f, 1.2f, 1.2f, 1.2f, 1f});

        String currency = AppConfig.getInstance().getCurrency();
        for (Transaction t : overdue) {
            addRow(table, new String[]{
                    nvl(t.getMemberName()),
                    nvl(t.getStudentId()),
                    nvl(t.getBookName()),
                    t.getIssueDate() != null ? t.getIssueDate().format(FMT) : "",
                    t.getDueDate()   != null ? t.getDueDate().format(FMT)   : "",
                    String.valueOf(t.getDaysOverdue()),
                    currency + " " + String.format("%.2f", t.calculateFine())
            }, t.getDaysOverdue() > 7 ? new Color(255, 235, 238) : Color.WHITE);
        }

        doc.add(table);
        addFooter(doc, overdue.size() + " overdue book(s) found.");
        doc.close();
    }

    // â”€â”€ PDF: Circulation Report â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void generateCirculationReport(LocalDate from, LocalDate to,
                                           OutputStream out) throws DocumentException {
        Document doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, out);
        doc.open();

        addHeader(doc, "Circulation Report",
                "Period: " + from.format(FMT) + " to " + to.format(FMT));

        List<Transaction> txList = txService.getTransactionsByDateRange(from, to);

        PdfPTable table = createTable(new String[]{
                "ID", "Member", "Book", "Issue Date", "Due Date", "Return Date", "Status", "Fine"
        }, new float[]{0.6f, 2f, 2.5f, 1.2f, 1.2f, 1.2f, 1f, 1f});

        String currency = AppConfig.getInstance().getCurrency();
        for (Transaction t : txList) {
            addRow(table, new String[]{
                    String.valueOf(t.getTransactionId()),
                    nvl(t.getMemberName()),
                    nvl(t.getBookName()),
                    t.getIssueDate()  != null ? t.getIssueDate().format(FMT)  : "",
                    t.getDueDate()    != null ? t.getDueDate().format(FMT)    : "",
                    t.getReturnDate() != null ? t.getReturnDate().format(FMT) : "â€”",
                    nvl(t.getStatus()),
                    t.getFineAmount() > 0
                            ? currency + " " + String.format("%.2f", t.getFineAmount()) : "â€”"
            }, Color.WHITE);
        }

        doc.add(table);
        addFooter(doc, txList.size() + " transaction(s) in period.");
        doc.close();
    }

    // â”€â”€ PDF: Inventory Report â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void generateInventoryReport(OutputStream out) throws DocumentException {
        Document doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, out);
        doc.open();

        addHeader(doc, "Book Inventory Report",
                "Generated: " + LocalDate.now().format(FMT));

        List<Book> books = bookService.getAllBooks(1, Integer.MAX_VALUE);

        PdfPTable table = createTable(new String[]{
                "ID", "ISBN", "Title", "Author", "Category", "Qty", "Available", "Status", "Shelf"
        }, new float[]{0.5f, 1.3f, 2.5f, 1.8f, 1.3f, 0.5f, 0.7f, 1f, 0.8f});

        for (Book b : books) {
            Color rowColor = switch (nvl(b.getStatus())) {
                case "Overdue"  -> new Color(255, 235, 238);
                case "Lost"     -> new Color(245, 245, 245);
                default         -> Color.WHITE;
            };
            addRow(table, new String[]{
                    String.valueOf(b.getBookId()),
                    nvl(b.getIsbn()),
                    b.getBookName(),
                    b.getAuthor(),
                    nvl(b.getCategory()),
                    String.valueOf(b.getQuantity()),
                    String.valueOf(b.getAvailableQty()),
                    nvl(b.getStatus()),
                    nvl(b.getShelfLocation())
            }, rowColor);
        }

        doc.add(table);
        addFooter(doc, books.size() + " book(s) in inventory.");
        doc.close();
    }

    // â”€â”€ PDF: Fine Collection Report â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void generateFineReport(OutputStream out) throws DocumentException {
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, out);
        doc.open();

        addHeader(doc, "Fine Collection Report",
                "Generated: " + LocalDate.now().format(FMT));

        List<Member> members = memberService.getMembersWithOutstandingFines();

        PdfPTable table = createTable(new String[]{
                "Student ID", "Name", "Department", "Contact", "Fine Balance"
        }, new float[]{1.5f, 2.5f, 2f, 1.5f, 1.5f});

        String currency = AppConfig.getInstance().getCurrency();
        double total = 0;
        for (Member m : members) {
            addRow(table, new String[]{
                    nvl(m.getStudentId()),
                    m.getName(),
                    nvl(m.getDepartment()),
                    nvl(m.getContact()),
                    currency + " " + String.format("%.2f", m.getFineBalance())
            }, new Color(255, 243, 224));
            total += m.getFineBalance();
        }

        doc.add(table);
        addFooter(doc, members.size() + " member(s) with outstanding fines. Total: "
                + currency + " " + String.format("%.2f", total));
        doc.close();
    }

    // â”€â”€ PDF: Popular Books â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void generatePopularBooksReport(int topN, OutputStream out) throws DocumentException {
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, out);
        doc.open();

        addHeader(doc, "Top " + topN + " Most Borrowed Books",
                "Generated: " + LocalDate.now().format(FMT));

        String sql = """
            SELECT b.book_id, b.book_name, b.author, b.category,
                   COUNT(t.transaction_id) as borrow_count
            FROM books b
            LEFT JOIN transactions t ON b.book_id = t.book_id
            GROUP BY b.book_id
            ORDER BY borrow_count DESC
            LIMIT ?
        """;

        PdfPTable table = createTable(new String[]{
                "Rank", "Title", "Author", "Category", "Times Borrowed"
        }, new float[]{0.5f, 3f, 2f, 1.5f, 1.5f});

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, topN);
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    addRow(table, new String[]{
                            String.valueOf(rank++),
                            rs.getString("book_name"),
                            rs.getString("author"),
                            nvl(rs.getString("category")),
                            String.valueOf(rs.getInt("borrow_count"))
                    }, rank % 2 == 0 ? new Color(232, 245, 233) : Color.WHITE);
                }
            }
        } catch (SQLException e) {
            LOG.error("Error generating popular books report: " + e.getMessage());
        }

        doc.add(table);
        doc.close();
    }

    // â”€â”€ PDF helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void addHeader(Document doc, String title, String subtitle)
            throws DocumentException {
        AppConfig cfg = AppConfig.getInstance();

        Font libFont  = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(25, 118, 210));
        Font subFont  = new Font(Font.HELVETICA, 10, Font.ITALIC, Color.GRAY);

        doc.add(new Paragraph(cfg.getLibraryName(), libFont));
        doc.add(new Paragraph(cfg.get(AppConfig.KEY_LIBRARY_ADDRESS), libFont));
        doc.add(new Paragraph(" "));

        Paragraph t = new Paragraph(title, titleFont);
        t.setAlignment(Element.ALIGN_CENTER);
        doc.add(t);

        Paragraph s = new Paragraph(subtitle, subFont);
        s.setAlignment(Element.ALIGN_CENTER);
        doc.add(s);
        doc.add(new Paragraph(" "));
    }

    private void addFooter(Document doc, String summary) throws DocumentException {
        doc.add(new Paragraph(" "));
        Font f = new Font(Font.HELVETICA, 10, Font.BOLD);
        doc.add(new Paragraph("Summary: " + summary, f));
        Font small = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY);
        doc.add(new Paragraph("LibraCore Pro v2.0.0 â€” " + LocalDate.now().format(FMT), small));
    }

    private PdfPTable createTable(String[] headers, float[] widths)
            throws DocumentException {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setWidths(widths);
        table.setSpacingBefore(10);

        Font hFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, hFont));
            cell.setBackgroundColor(new Color(25, 118, 210));
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
        return table;
    }

    private void addRow(PdfPTable table, String[] values, Color bg) {
        Font rowFont = new Font(Font.HELVETICA, 9);
        for (String v : values) {
            PdfPCell cell = new PdfPCell(new Phrase(nvl(v), rowFont));
            cell.setBackgroundColor(bg);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private String nvl(String s) { return s != null ? s : ""; }
}


