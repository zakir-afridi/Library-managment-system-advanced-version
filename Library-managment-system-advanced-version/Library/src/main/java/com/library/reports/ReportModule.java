package com.library.reports;

import com.library.service.ReportService;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

import java.io.*;
import java.time.LocalDate;

/**
 * REPORTS BRANCH — public API.
 */
public class ReportModule {

    private static final ReportService service = new ReportService();

    // ── UI Navigation ─────────────────────────────────────────────────────────

    public static void showReports(Pane container) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    ReportModule.class.getResource("/com/library/ui/ReportsView.fxml"));
            Pane view = loader.load();
            container.getChildren().setAll(view);
        } catch (Exception e) {
            System.err.println("ReportModule.showReports: " + e.getMessage());
        }
    }

    // ── Data API ──────────────────────────────────────────────────────────────

    public static boolean exportOverduePdf(File dest) {
        try (OutputStream out = new FileOutputStream(dest)) {
            service.generateOverdueReport(out); return true;
        } catch (Exception e) { System.err.println("ReportModule: " + e.getMessage()); return false; }
    }

    public static boolean exportCirculationPdf(LocalDate from, LocalDate to, File dest) {
        try (OutputStream out = new FileOutputStream(dest)) {
            service.generateCirculationReport(from, to, out); return true;
        } catch (Exception e) { System.err.println("ReportModule: " + e.getMessage()); return false; }
    }

    public static boolean exportInventoryPdf(File dest) {
        try (OutputStream out = new FileOutputStream(dest)) {
            service.generateInventoryReport(out); return true;
        } catch (Exception e) { System.err.println("ReportModule: " + e.getMessage()); return false; }
    }

    public static boolean exportFinePdf(File dest) {
        try (OutputStream out = new FileOutputStream(dest)) {
            service.generateFineReport(out); return true;
        } catch (Exception e) { System.err.println("ReportModule: " + e.getMessage()); return false; }
    }
}
