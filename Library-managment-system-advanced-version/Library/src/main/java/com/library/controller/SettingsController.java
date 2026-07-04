package com.library.controller;

import com.library.LibraCoreApp;
import com.library.api.ExchangeRateClient;
import com.library.config.AppConfig;
import com.library.config.ThemeManager;
import com.library.email.EmailService;
import com.library.security.SessionManager;
import com.library.service.BackupScheduler;
import com.library.util.AsyncRunner;
import com.library.util.ToastNotification;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class SettingsController {

    private static final Logger LOG = LoggerFactory.getLogger(SettingsController.class);

    // General
    @FXML private Button    backBtn;
    @FXML private Button    saveBtn;
    @FXML private TextField libraryNameField;
    @FXML private TextField libraryAddressField;
    @FXML private TextField libraryPhoneField;
    @FXML private TextField libraryEmailField;
    @FXML private ComboBox<String> currencyCombo;
    @FXML private ComboBox<String> dateFormatCombo;
    @FXML private ComboBox<String> itemsPerPageCombo;
    @FXML private ComboBox<String> defaultLimitCombo;

    // Book settings
    @FXML private Spinner<Integer> loanDaysSpinner;
    @FXML private Spinner<Double>  fineRateSpinner;
    @FXML private Spinner<Integer> gracePeriodSpinner;
    @FXML private Spinner<Integer> maxBooksSpinner;

    // Appearance
    @FXML private ToggleButton themeToggle;
    @FXML private Label        themeStatusLabel;

    // Notifications
    @FXML private CheckBox         overdueAlertCheck;
    @FXML private Spinner<Integer> dueSoonDaysSpinner;

    // Email (v3)
    @FXML private TextField     smtpHostField;
    @FXML private TextField     smtpPortField;
    @FXML private TextField     smtpUserField;
    @FXML private PasswordField smtpPasswordField;
    @FXML private TextField     emailFromNameField;
    @FXML private Button        testEmailBtn;
    @FXML private Label         emailStatusLabel;

    // Weather (v3)
    @FXML private TextField weatherCityField;
    @FXML private CheckBox  weatherEnabledCheck;

    // Backup
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
        if (currencyCombo != null)
            currencyCombo.getItems().addAll("PKR", "USD", "EUR", "GBP", "INR", "SAR", "AED");
        if (dateFormatCombo != null)
            dateFormatCombo.getItems().addAll("dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd");
        if (itemsPerPageCombo != null)
            itemsPerPageCombo.getItems().addAll("10", "25", "50", "100", "200");
        if (defaultLimitCombo != null)
            defaultLimitCombo.getItems().addAll("10", "25", "50", "100");

        if (loanDaysSpinner != null)
            loanDaysSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 90, 14));
        if (fineRateSpinner != null)
            fineRateSpinner.setValueFactory(
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 1000.0, 5.0, 0.5));
        if (gracePeriodSpinner != null)
            gracePeriodSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 14, 2));
        if (maxBooksSpinner != null)
            maxBooksSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 5));
        if (dueSoonDaysSpinner != null)
            dueSoonDaysSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 7, 2));
    }

    private void loadCurrentValues() {
        if (libraryNameField    != null) libraryNameField   .setText(nvl(config.getLibraryName()));
        if (libraryAddressField != null) libraryAddressField.setText(nvl(config.get(AppConfig.KEY_LIBRARY_ADDRESS)));
        if (libraryPhoneField   != null) libraryPhoneField  .setText(nvl(config.get(AppConfig.KEY_LIBRARY_PHONE)));
        if (libraryEmailField   != null) libraryEmailField  .setText(nvl(config.get(AppConfig.KEY_LIBRARY_EMAIL)));

        if (currencyCombo     != null) currencyCombo    .setValue(config.getCurrency());
        if (dateFormatCombo   != null) dateFormatCombo  .setValue(config.getDateFormat());
        if (itemsPerPageCombo != null) itemsPerPageCombo.setValue(String.valueOf(config.getItemsPerPage()));
        if (defaultLimitCombo != null) defaultLimitCombo.setValue(String.valueOf(config.getDefaultLimit()));

        if (loanDaysSpinner    != null) loanDaysSpinner   .getValueFactory().setValue(config.getLoanDays());
        if (fineRateSpinner    != null) fineRateSpinner   .getValueFactory().setValue(config.getFineRate());
        if (gracePeriodSpinner != null) gracePeriodSpinner.getValueFactory().setValue(config.getGracePeriod());
        if (maxBooksSpinner    != null) maxBooksSpinner   .getValueFactory().setValue(config.getMaxBooks());

        if (overdueAlertCheck  != null) overdueAlertCheck.setSelected(config.isOverdueAlert());
        if (dueSoonDaysSpinner != null) dueSoonDaysSpinner.getValueFactory().setValue(config.getDueSoonDays());

        if (smtpHostField      != null) smtpHostField    .setText(nvl(config.get(AppConfig.KEY_EMAIL_HOST)));
        if (smtpPortField      != null) smtpPortField    .setText(nvl(config.get(AppConfig.KEY_EMAIL_PORT)));
        if (smtpUserField      != null) smtpUserField    .setText(nvl(config.get(AppConfig.KEY_EMAIL_USER)));
        if (smtpPasswordField  != null) smtpPasswordField.setText(nvl(config.get(AppConfig.KEY_EMAIL_PASSWORD)));
        if (emailFromNameField != null) emailFromNameField.setText(nvl(config.get(AppConfig.KEY_EMAIL_FROM_NAME)));

        if (weatherCityField    != null) weatherCityField  .setText(nvl(config.get(AppConfig.KEY_WEATHER_CITY)));
        if (weatherEnabledCheck != null) weatherEnabledCheck.setSelected(
            config.getBoolean(AppConfig.KEY_WEATHER_ENABLED, true));
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    @FXML
    private void saveSettings() {
        if (libraryNameField    != null) config.set(AppConfig.KEY_LIBRARY_NAME,    libraryNameField.getText().trim());
        if (libraryAddressField != null) config.set(AppConfig.KEY_LIBRARY_ADDRESS, libraryAddressField.getText().trim());
        if (libraryPhoneField   != null) config.set(AppConfig.KEY_LIBRARY_PHONE,   libraryPhoneField.getText().trim());
        if (libraryEmailField   != null) config.set(AppConfig.KEY_LIBRARY_EMAIL,   libraryEmailField.getText().trim());

        if (currencyCombo     != null && currencyCombo.getValue()     != null) config.set(AppConfig.KEY_CURRENCY,       currencyCombo.getValue());
        if (dateFormatCombo   != null && dateFormatCombo.getValue()   != null) config.set(AppConfig.KEY_DATE_FORMAT,    dateFormatCombo.getValue());
        if (itemsPerPageCombo != null && itemsPerPageCombo.getValue() != null) config.set(AppConfig.KEY_ITEMS_PER_PAGE, itemsPerPageCombo.getValue());
        if (defaultLimitCombo != null && defaultLimitCombo.getValue() != null) config.set(AppConfig.KEY_DEFAULT_LIMIT,  defaultLimitCombo.getValue());

        if (loanDaysSpinner    != null) config.set(AppConfig.KEY_LOAN_DAYS,    String.valueOf(loanDaysSpinner.getValue()));
        if (fineRateSpinner    != null) config.set(AppConfig.KEY_FINE_RATE,    String.valueOf(fineRateSpinner.getValue()));
        if (gracePeriodSpinner != null) config.set(AppConfig.KEY_GRACE_PERIOD, String.valueOf(gracePeriodSpinner.getValue()));
        if (maxBooksSpinner    != null) config.set(AppConfig.KEY_MAX_BOOKS,    String.valueOf(maxBooksSpinner.getValue()));

        if (overdueAlertCheck  != null) config.set(AppConfig.KEY_OVERDUE_ALERT, String.valueOf(overdueAlertCheck.isSelected()));
        if (dueSoonDaysSpinner != null) config.set(AppConfig.KEY_DUE_SOON_DAYS, String.valueOf(dueSoonDaysSpinner.getValue()));

        if (smtpHostField      != null) config.set(AppConfig.KEY_EMAIL_HOST,      smtpHostField.getText().trim());
        if (smtpPortField      != null) config.set(AppConfig.KEY_EMAIL_PORT,      smtpPortField.getText().trim());
        if (smtpUserField      != null) config.set(AppConfig.KEY_EMAIL_USER,      smtpUserField.getText().trim());
        if (smtpPasswordField  != null) config.set(AppConfig.KEY_EMAIL_PASSWORD,  smtpPasswordField.getText().trim());
        if (emailFromNameField != null) config.set(AppConfig.KEY_EMAIL_FROM_NAME, emailFromNameField.getText().trim());

        if (weatherCityField    != null) config.set(AppConfig.KEY_WEATHER_CITY,    weatherCityField.getText().trim());
        if (weatherEnabledCheck != null) config.set(AppConfig.KEY_WEATHER_ENABLED, String.valueOf(weatherEnabledCheck.isSelected()));

        config.save();
        ToastNotification.success(getScene(), "Settings saved successfully.");
        LOG.info("Settings saved by user: {}", SessionManager.getInstance().getUsername());
    }

    // ── Email test ────────────────────────────────────────────────────────────

    @FXML
    private void testEmailConnection() {
        if (smtpUserField == null || smtpUserField.getText().isBlank()) {
            ToastNotification.warning(getScene(), "Enter SMTP username first.");
            return;
        }
        saveSettings();
        if (emailStatusLabel != null) emailStatusLabel.setText("Testing...");
        AsyncRunner.run(
            () -> EmailService.getInstance().isConfigured(),
            configured -> {
                if (emailStatusLabel != null)
                    emailStatusLabel.setText(configured ? "Email configured" : "Not configured");
                if (configured)
                    ToastNotification.success(getScene(), "Email configuration looks valid.");
                else
                    ToastNotification.warning(getScene(), "Email not configured - check SMTP settings.");
            },
            err -> {
                if (emailStatusLabel != null) emailStatusLabel.setText("Test failed");
                ToastNotification.error(getScene(), "Email test failed: " + err.getMessage());
            }
        );
    }

    // ── Exchange rates ────────────────────────────────────────────────────────

    @FXML
    private void refreshExchangeRates() {
        ToastNotification.info(getScene(), "Fetching exchange rates...");
        AsyncRunner.run(
            () -> ExchangeRateClient.getRates(config.getCurrency()),
            rates -> {
                if (rates.isEmpty())
                    ToastNotification.warning(getScene(), "Exchange rates unavailable (offline?)");
                else
                    ToastNotification.success(getScene(), "Rates updated - " + rates.size() + " currencies loaded.");
            },
            err -> ToastNotification.error(getScene(), "Rate fetch failed: " + err.getMessage())
        );
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    @FXML
    private void toggleTheme() {
        ThemeManager.getInstance().toggle(getScene());
        updateThemeUI();
    }

    private void updateThemeUI() {
        boolean dark = ThemeManager.getInstance().isDark();
        if (themeToggle != null) {
            themeToggle.setSelected(dark);
            themeToggle.setText(dark ? "Light Mode" : "Dark Mode");
        }
        if (themeStatusLabel != null)
            themeStatusLabel.setText("Current: " + (dark ? "Dark" : "Light") + " Mode");
    }

    // ── Backup & Restore ──────────────────────────────────────────────────────

    @FXML
    private void backupDatabase() {
        ToastNotification.info(getScene(), "Creating backup...");
        AsyncRunner.run(
            () -> {
                try { return BackupScheduler.getInstance().backup(); }
                catch (Exception e) { throw new RuntimeException(e); }
            },
            path -> {
                if (backupStatusLabel != null)
                    backupStatusLabel.setText("Backup: " + path.getFileName());
                ToastNotification.success(getScene(), "Database backed up: " + path.getFileName());
            },
            err -> ToastNotification.error(getScene(), "Backup failed: " + err.getMessage())
        );
    }

    @FXML
    private void restoreDatabase() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Backup File to Restore");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("SQLite Database", "*.db"));
        try {
            Path backupDir = BackupScheduler.getInstance().getBackupDir();
            chooser.setInitialDirectory(backupDir.toFile());
        } catch (Exception ignored) {}

        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Restore Database");
        confirm.setHeaderText("Replace current database with: " + file.getName() + "?");
        confirm.setContentText("This cannot be undone. The app will need to restart after restore.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                boolean ok = BackupScheduler.getInstance().restore(file.toPath());
                if (ok)
                    ToastNotification.success(getScene(), "Restored successfully. Please restart the application.");
                else
                    ToastNotification.error(getScene(), "Restore failed - see logs.");
            }
        });
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void goBack() {
        Scene scene = backBtn.getScene();
        if (scene != null && scene.getUserData() instanceof DashboardController dc) {
            dc.goBackToDashboard();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/library/ui/ProfessionalDashboard.fxml"));
            Stage stage = (Stage) backBtn.getScene().getWindow();
            boolean wasMaximized = stage.isMaximized();
            scene = backBtn.getScene();
            scene.setRoot(loader.load());
            ThemeManager.getInstance().applyTheme(scene);
            DashboardController dc = loader.getController();
            if (SessionManager.getInstance().isLoggedIn())
                dc.initSession(SessionManager.getInstance().getCurrentUser());
            stage.setTitle(LibraCoreApp.APP_NAME + " " + LibraCoreApp.APP_VERSION + " - Dashboard");
            if (wasMaximized) stage.setMaximized(true);
        } catch (IOException e) {
            ToastNotification.error(getScene(), "Navigation error: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Scene getScene() {
        return backBtn != null ? backBtn.getScene() : null;
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
