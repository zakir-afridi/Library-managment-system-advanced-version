package com.library.service;

import com.library.config.AppConfig;
import com.library.model.Employee;
import com.library.model.Member;
import com.library.model.Transaction;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;

import java.awt.Color;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * A4 PDF print service for members, employees, books, and fee slips.
 */
public class PrintService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TransactionService txService = new TransactionService();

    // ── Member Profile Print ──────────────────────────────────────────────────

    public void printMemberProfile(Member m, OutputStream out) throws DocumentException {
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, out);
        doc.open();

        addLetterhead(doc, "MEMBER PROFILE");

        // Profile info table
        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(100);
        info.setSpacingBefore(10);
        try { info.setWidths(new float[]{1.5f, 2.5f}); } catch (Exception ignored) {}

        addInfoRow(info, "Member Code",    nvl(m.getStudentId()));
        addInfoRow(info, "Full Name",      m.getName());
        addInfoRow(info, "Father's Name",  nvl(m.getFname()));
        addInfoRow(info, "Gender",         nvl(m.getGender()));
        addInfoRow(info, "Contact",        nvl(m.getContact()));
        addInfoRow(info, "Email",          nvl(m.getEmail()));
        addInfoRow(info, "Department",     nvl(m.getDepartment()));
        addInfoRow(info, "Program",        nvl(m.getProgram()));
        addInfoRow(info, "Semester",       nvl(m.getSemester()));
        addInfoRow(info, "Status",         nvl(m.getStatus()));
        addInfoRow(info, "Join Date",      m.getRegistrationDate() != null ? m.getRegistrationDate().format(FMT) : "");
        addInfoRow(info, "Fine Balance",   AppConfig.getInstance().getCurrency() + " " + String.format("%.2f", m.getFineBalance()));
        doc.add(info);

        // Transaction history
        doc.add(new Paragraph(" "));
        Font secFont = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(5, 150, 105));
        doc.add(new Paragraph("Issue / Return History", secFont));
        doc.add(new Paragraph(" "));

        List<Transaction> history = txService.getMemberTransactions(m.getStdId());

        PdfPTable txTable = new PdfPTable(5);
        txTable.setWidthPercentage(100);
        try { txTable.setWidths(new float[]{2.5f, 1.2f, 1.2f, 1.2f, 1f}); } catch (Exception ignored) {}

        addTableHeader(txTable, new String[]{"Book", "Issue Date", "Due Date", "Return Date", "Fine"});
        String currency = AppConfig.getInstance().getCurrency();
        for (Transaction t : history) {
            addTableRow(txTable, new String[]{
                nvl(t.getBookName()),
                t.getIssueDate()  != null ? t.getIssueDate().format(FMT)  : "",
                t.getDueDate()    != null ? t.getDueDate().format(FMT)    : "",
                t.getReturnDate() != null ? t.getReturnDate().format(FMT) : "—",
                t.getFineAmount() > 0 ? currency + " " + String.format("%.2f", t.getFineAmount()) : "—"
            });
        }
        doc.add(txTable);

        // Summary
        doc.add(new Paragraph(" "));
        Font bold = new Font(Font.HELVETICA, 10, Font.BOLD);
        doc.add(new Paragraph("Total Books Issued: " + history.size(), bold));
        long active = history.stream().filter(t -> Transaction.STATUS_ISSUED.equals(t.getStatus())).count();
        doc.add(new Paragraph("Currently Issued: " + active, bold));

        addFooter(doc);
        doc.close();
    }

    // ── Employee Profile Print ────────────────────────────────────────────────

    public void printEmployeeProfile(Employee e, OutputStream out) throws DocumentException {
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, out);
        doc.open();

        addLetterhead(doc, "EMPLOYEE PROFILE");

        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(100);
        info.setSpacingBefore(10);
        try { info.setWidths(new float[]{1.5f, 2.5f}); } catch (Exception ignored) {}

        addInfoRow(info, "Employee Code", nvl(e.getEmployeeCode()));
        addInfoRow(info, "Full Name",     e.getName());
        addInfoRow(info, "Designation",   nvl(e.getDesignation()));
        addInfoRow(info, "Department",    nvl(e.getDepartment()));
        addInfoRow(info, "Contact",       nvl(e.getContact()));
        addInfoRow(info, "Email",         nvl(e.getEmail()));
        addInfoRow(info, "CNIC",          nvl(e.getCnic()));
        addInfoRow(info, "Address",       nvl(e.getAddress()));
        addInfoRow(info, "Status",        nvl(e.getStatus()));
        addInfoRow(info, "Join Date",     e.getJoinDate() != null ? e.getJoinDate().format(FMT) : "");
        doc.add(info);

        addFooter(doc);
        doc.close();
    }

    // ── Fee / Fine Slip ───────────────────────────────────────────────────────

    public void printFineSlip(Member m, double amount, String reason, OutputStream out) throws DocumentException {
        Document doc = new Document(PageSize.A5);
        PdfWriter.getInstance(doc, out);
        doc.open();

        addLetterhead(doc, "PAYMENT RECEIPT / FINE SLIP");

        Font normalFont = new Font(Font.HELVETICA, 10);
        Paragraph date = new Paragraph("Date: " + LocalDate.now().format(FMT), normalFont);
        doc.add(date);
        doc.add(new Paragraph(" "));

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        try { t.setWidths(new float[]{1.5f, 2.5f}); } catch (Exception ignored) {}

        addInfoRow(t, "Receipt No.",  "RCP-" + System.currentTimeMillis() % 1000000);
        addInfoRow(t, "Member Name",  m.getName());
        addInfoRow(t, "Member ID",    nvl(m.getStudentId()));
        addInfoRow(t, "Department",   nvl(m.getDepartment()));
        addInfoRow(t, "Description",  reason);
        addInfoRow(t, "Amount",      AppConfig.getInstance().getCurrency() + " " + String.format("%.2f", amount));
        doc.add(t);

        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("Authorized Signature: ___________________", normalFont));
        doc.close();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void addLetterhead(Document doc, String title) throws DocumentException {
        LibraryInfo libInfo = LibraryInfoService.getInstance().getLibraryInfo();
        Font libFont   = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(5, 150, 105));
        Font instFont  = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(30, 41, 59));
        Font subFont   = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);
        Font titleFont = new Font(Font.HELVETICA, 13, Font.BOLD, new Color(6, 95, 70));

        Paragraph lib = new Paragraph(libInfo.getLibraryName(), libFont);
        lib.setAlignment(Element.ALIGN_CENTER);
        doc.add(lib);

        if (libInfo.getInstitutionName() != null && !libInfo.getInstitutionName().isBlank()) {
            Paragraph inst = new Paragraph(libInfo.getInstitutionName(), instFont);
            inst.setAlignment(Element.ALIGN_CENTER);
            doc.add(inst);
        }

        String contactLine = libInfo.getAddress() + (libInfo.getContactNumber().isBlank() ? "" : " | Tel: " + libInfo.getContactNumber()) + (libInfo.getEmail().isBlank() ? "" : " | " + libInfo.getEmail());
        Paragraph addr = new Paragraph(contactLine, subFont);
        addr.setAlignment(Element.ALIGN_CENTER);
        doc.add(addr);

        doc.add(new Paragraph(" "));
        Paragraph t = new Paragraph(title, titleFont);
        t.setAlignment(Element.ALIGN_CENTER);
        doc.add(t);

        LineSeparator line = new LineSeparator();
        line.setLineColor(new Color(5, 150, 105));
        doc.add(new Chunk(line));
        doc.add(new Paragraph(" "));
    }

    private void addInfoRow(PdfPTable table, String label, String value) {
        Font lf = new Font(Font.HELVETICA, 9, Font.BOLD, Color.DARK_GRAY);
        Font vf = new Font(Font.HELVETICA, 9);
        PdfPCell lc = new PdfPCell(new Phrase(label, lf));
        PdfPCell vc = new PdfPCell(new Phrase(value != null ? value : "", vf));
        lc.setBorder(Rectangle.BOTTOM); lc.setPadding(5);
        vc.setBorder(Rectangle.BOTTOM); vc.setPadding(5);
        table.addCell(lc);
        table.addCell(vc);
    }

    private void addTableHeader(PdfPTable table, String[] headers) {
        Font hf = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, hf));
            cell.setBackgroundColor(new Color(5, 150, 105));
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, String[] values) {
        Font rf = new Font(Font.HELVETICA, 8);
        for (String v : values) {
            PdfPCell cell = new PdfPCell(new Phrase(v != null ? v : "", rf));
            cell.setPadding(4);
            table.addCell(cell);
        }
    }

    private void addFooter(Document doc) throws DocumentException {
        doc.add(new Paragraph(" "));
        Font f = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY);
        Paragraph p = new Paragraph("Generated by LibraCore Pro v3.0.0 — " + LocalDate.now().format(FMT), f);
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
