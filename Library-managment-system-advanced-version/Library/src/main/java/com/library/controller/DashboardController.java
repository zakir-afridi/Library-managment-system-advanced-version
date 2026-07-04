package com.library.controller;

import com.library.LibraCoreApp;
import com.library.api.WeatherClient;
import com.library.cache.DashboardCache;
import com.library.cache.DashboardStats;
import com.library.config.AppConfig;
import com.library.config.ThemeManager;
import com.library.model.ActivityRecord;
import com.library.model.Transaction;
import com.library.model.User;
import com.library.model.WeatherInfo;
import com.library.security.SessionManager;
import com.library.service.TransactionService;
import com.library.util.AsyncRunner;
import com.library.util.ToastNotification;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class DashboardController {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardController.class);

    // ── Root for center-swap navigation ──────────────────────────────────────
    @FXML private BorderPane rootPane;  // fx:id on the root BorderPane
    private Node dashboardCenter;       // cached dashboard content

    // ── Top bar ───────────────────────────────────────────────────────────────
    @FXML private Text   appTitleText;
    @FXML private Text   moduleText;
    @FXML private Label  userLabel;
    @FXML private Label  overdueAlertLabel;
    @FXML private Button refreshBtn;
    @FXML private Button logoutBtn;
    @FXML private Button themeToggleBtn;

    // ── Sidebar ───────────────────────────────────────────────────────────────
    @FXML private Button dashboardBtn;
    @FXML private Button booksBtn;
    @FXML private Button membersBtn;
    @FXML private Button issueReturnBtn;
    @FXML private Button employeesBtn;
    @FXML private Button archiveBtn;
    @FXML private Button reportsBtn;
    @FXML private Button settingsBtn;
    @FXML private Button sidebarLogoutBtn;

    // ── Weather widget (v3) ───────────────────────────────────────────────────
    @FXML private Label weatherCityLabel;
    @FXML private Label weatherTempLabel;
    @FXML private Label weatherDescLabel;
    @FXML private Label weatherIconLabel;

    // ── KPI cards ─────────────────────────────────────────────────────────────
    @FXML private Text totalBooksText;
    @FXML private Text totalMembersText;
    @FXML private Text issuedBooksText;
    @FXML private Text overdueBooksText;
    @FXML private Text availableBooksText;
    @FXML private Text booksTrendText;
    @FXML private Text membersTrendText;
    @FXML private Text issuedTodayText;
    @FXML private Text finesCollectedText;
    @FXML private Text pendingFinesText;

    // ── Charts ────────────────────────────────────────────────────────────────
    @FXML private PieChart              bookStatusChart;
    @FXML private BarChart<String, Number> monthlyBarChart;
    @FXML private CategoryAxis          barMonthAxis;
    @FXML private NumberAxis            barCountAxis;

    // ── Activity table ────────────────────────────────────────────────────────
    @FXML private TableView<ActivityRecord>          activityTable;
    @FXML private TableColumn<ActivityRecord, String> memberColumn;
    @FXML private TableColumn<ActivityRecord, String> bookColumn;
    @FXML private TableColumn<ActivityRecord, String> actionColumn;
    @FXML private TableColumn<ActivityRecord, String> dateColumn;
    @FXML private TableColumn<ActivityRecord, String> statusColumn;
    @FXML private TextField searchField;

    private final TransactionService txService = new TransactionService();
    private final ObservableList<ActivityRecord> activityData = FXCollections.observableArrayList();
    private FilteredList<ActivityRecord> filteredActivity;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        setupTable();
        setupSearch();
        setActiveButton(dashboardBtn);
        loadDashboardAsync();
        if (AppConfig.getInstance().getBoolean(AppConfig.KEY_WEATHER_ENABLED, true)) {
            loadWeatherAsync();
        }
        // cache the dashboard center so we can restore it on showDashboard()
        if (rootPane != null) dashboardCenter = rootPane.getCenter();
    }

    /** Called by LoginController after successful auth. */
    public void initSession(User user) {
        if (userLabel != null)
            userLabel.setText("Welcome, " + user.getUsername()
                    + "  [" + user.getRole() + "]");
        if (appTitleText != null)
            appTitleText.setText(LibraCoreApp.APP_NAME);

        // Hide write-only controls for VIEWER role
        if (User.ROLE_VIEWER.equals(user.getRole())) {
            issueReturnBtn.setDisable(true);
        }
    }

    // ── Table setup ───────────────────────────────────────────────────────────

    private void setupTable() {
        memberColumn.setCellValueFactory(new PropertyValueFactory<>("memberName"));
        bookColumn  .setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        dateColumn  .setCellValueFactory(new PropertyValueFactory<>("date"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Colour-coded status column
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle(switch (s.toLowerCase()) {
                    case "issued"   -> "-fx-text-fill:#f57c00; -fx-font-weight:bold;";
                    case "returned" -> "-fx-text-fill:#388e3c; -fx-font-weight:bold;";
                    case "overdue"  -> "-fx-text-fill:#d32f2f; -fx-font-weight:bold;";
                    default         -> "-fx-text-fill:#424242;";
                });
            }
        });

        filteredActivity = new FilteredList<>(activityData, p -> true);
        activityTable.setItems(filteredActivity);
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, old, val) ->
            filteredActivity.setPredicate(r -> {
                if (val == null || val.isBlank()) return true;
                String lower = val.toLowerCase();
                return r.getMemberName().toLowerCase().contains(lower)
                    || r.getBookTitle().toLowerCase().contains(lower)
                    || r.getAction().toLowerCase().contains(lower)
                    || r.getStatus().toLowerCase().contains(lower);
            })
        );
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    @FXML
    private void handleRefresh() {
        DashboardCache.getInstance().invalidate();
        refreshBtn.setDisable(true);
        refreshBtn.setText("⏳");
        loadDashboardAsync();
        if (AppConfig.getInstance().getBoolean(AppConfig.KEY_WEATHER_ENABLED, true)) {
            loadWeatherAsync();
        }
    }

    private void loadDashboardAsync() {
        AsyncRunner.run(
            () -> DashboardCache.getInstance().getStats(),
            s -> {
                updateKpiCards(s);
                loadCharts(s);
                loadActivityTable();
                refreshBtn.setDisable(false);
                refreshBtn.setText("🔄 Refresh");
            },
            err -> {
                LOG.error("Dashboard load error", err);
                refreshBtn.setDisable(false);
                refreshBtn.setText("🔄 Refresh");
                ToastNotification.error(dashboardBtn.getScene(),
                    "Dashboard refresh failed: " + err.getMessage());
            }
        );
    }

    /** Load weather data asynchronously using virtual threads. */
    private void loadWeatherAsync() {
        String city = AppConfig.getInstance().get(AppConfig.KEY_WEATHER_CITY);
        if (city == null || city.isBlank()) city = "Peshawar";
        final String finalCity = city;
        AsyncRunner.run(
            () -> WeatherClient.getWeather(finalCity),
            optInfo -> optInfo.ifPresent(this::updateWeatherWidget),
            err -> LOG.warn("Weather load failed: {}", err.getMessage())
        );
    }

    private void updateWeatherWidget(WeatherInfo info) {
        if (weatherCityLabel != null) weatherCityLabel.setText(info.getCity());
        if (weatherTempLabel != null) weatherTempLabel.setText(
            String.format("%.0f°C", info.getTemperature()));
        if (weatherDescLabel != null) weatherDescLabel.setText(info.getDescription());
        if (weatherIconLabel != null) weatherIconLabel.setText(info.getIcon());
    }

    // ── KPI cards ─────────────────────────────────────────────────────────────

    private void updateKpiCards(DashboardStats s) {
        String currency = AppConfig.getInstance().getCurrency();

        if (totalBooksText    != null) totalBooksText   .setText(String.valueOf(s.totalBooks));
        if (totalMembersText  != null) totalMembersText .setText(String.valueOf(s.activeMembers));
        if (issuedBooksText   != null) issuedBooksText  .setText(String.valueOf(s.issuedBooks));
        if (overdueBooksText  != null) overdueBooksText .setText(String.valueOf(s.overdueBooks));
        if (availableBooksText!= null) availableBooksText.setText(String.valueOf(s.availableBooks));
        if (issuedTodayText   != null) issuedTodayText  .setText(s.issuedToday + " today");
        if (finesCollectedText!= null) finesCollectedText.setText(currency + " " + String.format("%.2f", s.totalFinesCollected));
        if (pendingFinesText  != null) pendingFinesText .setText(currency + " " + String.format("%.2f", s.pendingFines));

        // Overdue alert badge
        if (overdueAlertLabel != null) {
            if (s.overdueBooks > 0) {
                overdueAlertLabel.setText("! " + s.overdueBooks + " Overdue");
                overdueAlertLabel.setVisible(true);
                overdueAlertLabel.setManaged(true);
            } else {
                overdueAlertLabel.setVisible(false);
                overdueAlertLabel.setManaged(false);
            }
        }

        // Trend indicators
        if (booksTrendText != null) {
            booksTrendText.setText(s.availableBooks > s.issuedBooks ? "up" : "down");
            booksTrendText.setStyle(s.availableBooks > s.issuedBooks
                    ? "-fx-fill:#388e3c; -fx-font-weight:bold;"
                    : "-fx-fill:#d32f2f; -fx-font-weight:bold;");
        }
        if (membersTrendText != null)
            membersTrendText.setText(s.activeMembers > 0 ? "up" : "stable");
    }

    // ── Charts ────────────────────────────────────────────────────────────────

    private void loadCharts(DashboardStats s) {
        // Pie chart — book status distribution
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
            new PieChart.Data("Available (" + s.availableBooks + ")", Math.max(s.availableBooks, 1)),
            new PieChart.Data("Issued ("    + s.issuedBooks    + ")", Math.max(s.issuedBooks, 1)),
            new PieChart.Data("Overdue ("   + s.overdueBooks   + ")", Math.max(s.overdueBooks, 1)),
            new PieChart.Data("Reserved ("  + s.reservations   + ")", Math.max(s.reservations, 1))
        );
        bookStatusChart.setData(pieData);
        bookStatusChart.setLegendVisible(true);

        // Bar chart — monthly issued (last 12 months)
        Map<String, Integer> monthly = txService.getMonthlyIssuedStats(12);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Books Issued");
        monthly.forEach((month, count) ->
            series.getData().add(new XYChart.Data<>(month, count)));
        monthlyBarChart.getData().clear();
        monthlyBarChart.getData().add(series);
    }

    // ── Activity table ────────────────────────────────────────────────────────

    private void loadActivityTable() {
        List<Transaction> recent = txService.getRecentActivity(20);
        activityData.clear();

        if (recent.isEmpty()) {
            // Load from DB directly if stack is empty (first load)
            txService.getActiveTransactions().stream().limit(20).forEach(tx -> {
                String date = tx.getIssueDate() != null
                        ? tx.getIssueDate().format(DATE_FMT) : "";
                String status = tx.isOverdue() ? "Overdue" : tx.getStatus();
                activityData.add(new ActivityRecord(
                        tx.getMemberName() != null ? tx.getMemberName() : "—",
                        tx.getBookName()   != null ? tx.getBookName()   : "—",
                        "Issued", date, status));
            });
        } else {
            recent.forEach(tx -> {
                String date = tx.getIssueDate() != null
                        ? tx.getIssueDate().format(DATE_FMT) : "";
                String status = tx.isOverdue() ? "Overdue" : tx.getStatus();
                activityData.add(new ActivityRecord(
                        tx.getMemberName() != null ? tx.getMemberName() : "—",
                        tx.getBookName()   != null ? tx.getBookName()   : "—",
                        tx.getStatus(), date, status));
            });
        }
    }

    // ── Theme toggle ──────────────────────────────────────────────────────────

    @FXML
    private void toggleTheme() {
        ThemeManager.getInstance().toggle(dashboardBtn.getScene());
        themeToggleBtn.setText(ThemeManager.getInstance().isDark() ? "☀" : "🌙");
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void showDashboard() {
        setActiveButton(dashboardBtn);
        if (moduleText != null) moduleText.setText("— Dashboard");
        // Restore dashboard center content
        if (rootPane != null && dashboardCenter != null) {
            rootPane.setCenter(dashboardCenter);
            DashboardCache.getInstance().invalidate();
            loadDashboardAsync();
        }
        Stage stage = (Stage) dashboardBtn.getScene().getWindow();
        stage.setTitle(LibraCoreApp.APP_NAME + " " + LibraCoreApp.APP_VERSION + " — Dashboard");
    }

    @FXML private void showBooks()       { setActiveButton(booksBtn);       navigateCenter("/com/library/ui/AddBookForm.fxml",          "— Books"); }
    @FXML private void showMembers()     { setActiveButton(membersBtn);     navigateCenter("/com/library/ui/AddMemberForm.fxml",         "— Members"); }
    @FXML private void showIssueReturn() { setActiveButton(issueReturnBtn); navigateCenter("/com/library/ui/IssueReturnBooksForm.fxml",  "— Issue/Return"); }
    @FXML private void showEmployees()   { setActiveButton(employeesBtn);   navigateCenter("/com/library/ui/EmployeeForm.fxml",          "— Employees"); }
    @FXML private void showArchive()     { setActiveButton(archiveBtn);     navigateCenter("/com/library/ui/ArchiveView.fxml",           "— Archive"); }
    @FXML private void showReports()     { setActiveButton(reportsBtn);     navigateCenter("/com/library/ui/ReportsView.fxml",           "— Reports"); }
    @FXML private void showSettings()    { setActiveButton(settingsBtn);    navigateCenter("/com/library/ui/Settings.fxml",              "— Settings"); }

    // ── Quick actions ─────────────────────────────────────────────────────────

    @FXML private void addNewBook()     { setActiveButton(booksBtn);       navigateCenter("/com/library/ui/AddBookForm.fxml",         "— Add Book"); }
    @FXML private void addNewMember()   { setActiveButton(membersBtn);     navigateCenter("/com/library/ui/AddMemberForm.fxml",        "— Add Member"); }
    @FXML private void issueBook()      { setActiveButton(issueReturnBtn); navigateCenter("/com/library/ui/IssueReturnBooksForm.fxml", "— Issue Book"); }
    @FXML private void returnBook()     { setActiveButton(issueReturnBtn); navigateCenter("/com/library/ui/IssueReturnBooksForm.fxml", "— Return Book"); }
    @FXML private void generateReport() { setActiveButton(reportsBtn);     navigateCenter("/com/library/ui/ReportsView.fxml",          "— Reports"); }

    // ── Logout ────────────────────────────────────────────────────────────────

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/library/ui/LoginPage.fxml"));
            Stage stage = (Stage) logoutBtn.getScene().getWindow();
            stage.setMaximized(false);
            stage.setWidth(1000);
            stage.setHeight(650);
            Scene scene = logoutBtn.getScene();
            scene.setRoot(loader.load());
            ThemeManager.getInstance().applyTheme(scene);
            stage.setTitle(LibraCoreApp.APP_NAME + " " + LibraCoreApp.APP_VERSION + " — Login");
            stage.centerOnScreen();
        } catch (IOException e) {
            showToast("Logout error: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Loads an FXML and places it in the dashboard's center pane,
     * keeping the sidebar and top-bar intact.
     * Sub-module "← Dashboard" buttons call goBackToDashboard() which
     * triggers showDashboard() on this controller via the scene's userData.
     */
    private void navigateCenter(String fxml, String module) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            if (loader.getLocation() == null) {
                showToast("Cannot find: " + fxml);
                return;
            }
            Node content = loader.load();
            // Strip the embedded top-bar from the loaded BorderPane so we don't show double headers
            if (content instanceof BorderPane bp && bp.getTop() != null) {
                bp.setTop(null);
            }
            if (rootPane != null) {
                rootPane.setCenter(content);
            }
            if (moduleText != null) moduleText.setText(module);
            Stage stage = (Stage) dashboardBtn.getScene().getWindow();
            stage.setTitle(LibraCoreApp.APP_NAME + " " + LibraCoreApp.APP_VERSION + " " + module);
            // Store reference so sub-controllers can call back
            dashboardBtn.getScene().setUserData(this);
        } catch (IOException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            showToast("Cannot load " + module + ": " + cause.getMessage());
            LOG.error("Navigation error loading {}", fxml, cause);
        }
    }

    /** Called by sub-controllers when they want to go back to the dashboard. */
    public void goBackToDashboard() {
        showDashboard();
    }

    private void setActiveButton(Button active) {
        Button[] navBtns = {dashboardBtn, booksBtn, membersBtn,
                issueReturnBtn, employeesBtn, archiveBtn, reportsBtn, settingsBtn};
        for (Button b : navBtns) if (b != null) b.getStyleClass().remove("active");
        if (active != null && !active.getStyleClass().contains("active"))
            active.getStyleClass().add("active");
    }

    private void showToast(String msg) {
        ToastNotification.info(dashboardBtn.getScene(), msg);
    }
}
