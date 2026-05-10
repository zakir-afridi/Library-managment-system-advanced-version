package com.library.controller;

import com.library.LibraCoreApp;
import com.library.config.AppConfig;
import com.library.config.ThemeManager;
import com.library.security.SessionManager;
import com.library.util.ToastNotification;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SettingsController {

    // ── General ───────────────────────────────────────────────────────────────
    @FXML private Button   backBtn;
    @FXML private Button   saveBtn;
    @FXML private TextField libraryNameField;
    @FXML private TextField libraryAddressField;
    @FXML private TextField libraryPhoneField;
    @FXML private TextField libraryEmailField;
    @FXML private ComboBox<String> currencyCombo;
    @FXML private ComboBox<String> dateFormatCombo;
    @FXML private ComboBox<String> itemsPerPageCombo;
    @FXML private ComboBox<String> defaultLimitCombo;

    // ── Book settings ─────────────────────────────────────────────────────────
    @FXML private Spinner<Integer> loanDaysSpinner;
    @FXML private Spinner<Double>  fineRateSpinner;
    @FXML private Spinner<Integer> gracePeriodSpinner;
    @FXML private Spinner<Integer> maxBooksSpinner;

    // ── Appearance ────────────────────────────────────────────────────────────
    @FXML private ToggleButton themeToggle;
    @FXML private Label        themeStatusLabel;

    // ── Notifications ─────────────────────────────────────────────────────────
    @FXML private CheckBox     overdueAlertCheck;
    @FXML private Spinner<Integer> dueSoonDaysSpinner;

    // ── Backup ────────────────────────────────────────────────────────────────
    @FXML private Label backupStatusLabel;

    private final AppConfig config = AppConfig.getInstance();

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        populateCombos();
        loadCurrentValues();
        updateThemeUI();
    }

    private void populateCombos() {
        currencyCombo.getItems().addAll("PKR", "USD", "EUR", "GBP", "INR", "SAR");
        dateFormatCombo.getItems().addAll("dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd");
        itemsPerPageCombo.getItems().addAll("10", "25", "50", "100", "200");
        defaultLimitCombo.getItems().addAll("10", "25", "50", "100");

        loanDaysSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 90, 14));
        fineRateSpinner.setValueFactory(
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1000.0, 5.0, 0.5));
        gracePeriodSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 14, 2));
        maxBooksSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 5));
        dueSoonDaysSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 7, 2));
    }

    private void loadCurrentValues() {
        libraryNameField   .setText(config.getLibraryName());
        libraryAddressField.setText(config.get(AppConfig.KEY_LIBRARY_ADDRESS));
        libraryPhoneField  .setText(config.get(AppConfig.KEY_LIBRARY_PHONE));
        libraryEmailField  .setText(config.get(AppConfig.KEY_LIBRARY_EMAIL));

        currencyCombo    .setValue(config.getCurrency());
        dateFormatCombo  .setValue(config.getDateFormat());
        itemsPerPageCombo.setValue(String.valueOf(config.getItemsPerPage()));
        defaultLimitCombo.setValue(String.valueOf(config.getDefaultLimit()));

        loanDaysSpinner   .getValueFactory().setValue(config.getLoanDays());
        fineRateSpinner   .getValueFactory().setValue(config.getFineRate());
        gracePeriodSpinner.getValueFactory().setValue(config.getGracePeriod());
        maxBooksSpinner   .getValueFactory().setValue(config.getMaxBooks());

        overdueAlertCheck.setSelected(config.isOverdueAlert());
        dueSoonDaysSpinner.getValueFactory().setValue(config.getDueSoonDays());
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    @FXML
    private void saveSettings() {
        config.set(AppConfig.KEY_LIBRARY_NAME,    libraryNameField.getText().trim());
        config.set(AppConfig.KEY_LIBRARY_ADDRESS, libraryAddressField.getText().trim());
        config.set(AppConfig.KEY_LIBRARY_PHONE,   libraryPhoneField.getText().trim());
        config.set(AppConfig.KEY_LIBRARY_EMAIL,   libraryEmailField.getText().trim());
        config.set(AppConfig.KEY_CURRENCY,        currencyCombo.getValue());
        config.set(AppConfig.KEY_DATE_FORMAT,     dateFormatCombo.getValue());
        config.set(AppConfig.KEY_ITEMS_PER_PAGE,  itemsPerPageCombo.getValue());
        config.set(AppConfig.KEY_DEFAULT_LIMIT,   defaultLimitCombo.getValue());
        config.set(AppConfig.KEY_LOAN_DAYS,       String.valueOf(loanDaysSpinner.getValue()));
        config.set(AppConfig.KEY_FINE_RATE,       String.valueOf(fineRateSpinner.getValue()));
        config.set(AppConfig.KEY_GRACE_PERIOD,    String.valueOf(gracePeriodSpinner.getValue()));
        config.set(AppConfig.KEY_MAX_BOOKS,       String.valueOf(maxBooksSpinner.getValue()));
        config.set(AppConfig.KEY_OVERDUE_ALERT,   String.valueOf(overdueAlertCheck.isSelected()));
        config.set(AppConfig.KEY_DUE_SOON_DAYS,   String.valueOf(dueSoonDaysSpinner.getValue()));
        config.save();

        ToastNotification.success(backBtn.getScene(), "Settings saved successfully.");
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    @FXML
    private void toggleTheme() {
        ThemeManager.getInstance().toggle(backBtn.getScene());
        updateThemeUI();
    }

    private void updateThemeUI() {
        boolean dark = ThemeManager.getInstance().isDark();
        themeToggle.setSelected(dark);
        themeToggle.setText(dark ? "☀ Light Mode" : "🌙 Dark Mode");
        themeStatusLabel.setText("Current: " + (dark ? "Dark Mode" : "Light Mode"));
    }

    // ── Backup & Restore ──────────────────────────────────────────────────────

    @FXML
    private void backupDatabase() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Backup Location");
        File dir = chooser.showDialog(backBtn.getScene().getWindow());
        if (dir == null) return;

        try {
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path src  = Paths.get("library.db");
            Path dest = dir.toPath().resolve("library_backup_" + timestamp + ".db");
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            backupStatusLabel.setText("Backup saved: " + dest.getFileName());
            ToastNotification.success(backBtn.getScene(),
                    "Database backed up to: " + dest.getFileName());
        } catch (IOException e) {
            ToastNotification.error(backBtn.getScene(), "Backup failed: " + e.getMessage());
        }
    }

    @FXML
    private void restoreDatabase() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Backup File");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("SQLite DB", "*.db"));
        File file = chooser.showOpenDialog(backBtn.getScene().getWindow());
        if (file == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Restore Database");
        confirm.setHeaderText("This will replace the current database.");
        confirm.setContentText("Are you sure you want to restore from: " + file.getName() + "?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    Files.copy(file.toPath(), Paths.get("library.db"),
                            StandardCopyOption.REPLACE_EXISTING);
                    ToastNotification.success(backBtn.getScene(),
                            "Database restored. Please restart the application.");
                } catch (IOException e) {
                    ToastNotification.error(backBtn.getScene(),
                            "Restore failed: " + e.getMessage());
                }
            }
        });
    }

    // ── Navigation ────────────────────────────────────────────────────────────

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
