package com.library.controller;

import com.library.LibraCoreApp;
import com.library.config.ThemeManager;
import com.library.security.SessionManager;
import com.library.service.ReportService;
import com.library.util.ToastNotification;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.time.LocalDate;

public class ReportsController {

    @FXML private Button    backBtn;
    @FXML private Button    themeBtn;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private Label     statusLabel;

    private final ReportService reportService = new ReportService();

    @FXML
    public void initialize() {
        fromDate.setValue(LocalDate.now().withDayOfMonth(1));
        toDate.setValue(LocalDate.now());
    }

    @FXML private void generateOverdueReport()     { savePdf("overdue_report.pdf",      out -> reportService.generateOverdueReport(out)); }
    @FXML private void generateInventoryReport()   { savePdf("inventory_report.pdf",    out -> reportService.generateInventoryReport(out)); }
    @FXML private void generateFineReport()        { savePdf("fine_report.pdf",         out -> reportService.generateFineReport(out)); }
    @FXML private void generatePopularReport()     { savePdf("popular_books.pdf",       out -> reportService.generatePopularBooksReport(20, out)); }

    @FXML
    private void generateCirculationReport() {
        LocalDate from = fromDate.getValue();
        LocalDate to   = toDate.getValue();
        if (from == null || to == null) {
            ToastNotification.warning(backBtn.getScene(), "Please select a date range.");
            return;
        }
        savePdf("circulation_report.pdf", out -> reportService.generateCirculationReport(from, to, out));
    }

    @FXML
    private void generateMemberReport() {
        savePdf("member_activity.pdf", out -> reportService.generateCirculationReport(
                LocalDate.now().minusMonths(1), LocalDate.now(), out));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    @FunctionalInterface
    interface ReportGenerator { void generate(OutputStream out) throws Exception; }

    private void savePdf(String defaultName, ReportGenerator generator) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Report");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fc.setInitialFileName(defaultName);
        File f = fc.showSaveDialog(backBtn.getScene().getWindow());
        if (f == null) return;

        try (FileOutputStream fos = new FileOutputStream(f)) {
            generator.generate(fos);
            statusLabel.setText("✅ Report saved: " + f.getAbsolutePath());
            ToastNotification.success(backBtn.getScene(), "Report saved: " + f.getName());
        } catch (Exception e) {
            statusLabel.setText("❌ Error: " + e.getMessage());
            ToastNotification.error(backBtn.getScene(), "Report failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML private void toggleTheme() {
        ThemeManager.getInstance().toggle(backBtn.getScene());
        themeBtn.setText(ThemeManager.getInstance().isDark() ? "☀" : "🌙");
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/library/ui/ProfessionalDashboard.fxml"));
            Stage stage = (Stage) backBtn.getScene().getWindow();
            boolean wasMaximized = stage.isMaximized();
            Scene scene = backBtn.getScene();
            scene.setRoot(loader.load());
            ThemeManager.getInstance().applyTheme(scene);
            DashboardController dc = loader.getController();
            if (SessionManager.getInstance().isLoggedIn())
                dc.initSession(SessionManager.getInstance().getCurrentUser());
            stage.setTitle(LibraCoreApp.APP_NAME + " " + LibraCoreApp.APP_VERSION + " — Dashboard");
            if (wasMaximized) stage.setMaximized(true);
        } catch (IOException e) {
            ToastNotification.error(backBtn.getScene(), "Navigation error: " + e.getMessage());
        }
    }
}
