package com.library.controller;

import com.library.LibraCoreApp;
import com.library.cache.DashboardCache;
import com.library.cache.DashboardStats;
import com.library.config.AppConfig;
import com.library.config.ThemeManager;
import com.library.model.ActivityRecord;
import com.library.model.Transaction;
import com.library.model.User;
import com.library.model.WeatherInfo;
import com.library.api.WeatherClient;
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
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * DashboardController — Visual Analytics &amp; Business Intelligence Command Center.
 */
public class DashboardController {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardController.class);

    // ── Root & Layout ─────────────────────────────────────────────────────────
    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private Text appTitleText;
    @FXML private Text moduleText;
    @FXML private Label userLabel;
    @FXML private Label overdueAlertLabel;
    @FXML private Button themeToggleBtn;
    @FXML private Button refreshBtn;
    @FXML private Button logoutBtn;
    @FXML private Button sidebarToggleBtn;

    // ── Navigation buttons ────────────────────────────────────────────────────
    @FXML private Button dashboardBtn;
    @FXML private Button booksBtn;
    @FXML private Button membersBtn;
    @FXML private Button issueReturnBtn;
    @FXML private Button employeesBtn;
    @FXML private Button archiveBtn;
    @FXML private Button reportsBtn;
    @FXML private Button settingsBtn;
    @FXML private Button sidebarLogoutBtn;

    // ── Weather widget ────────────────────────────────────────────────────────
    @FXML private Label weatherCityLabel;
    @FXML private Label weatherTempLabel;
    @FXML private Label weatherDescLabel;
    @FXML private Label weatherIconLabel;

    // ── Filter Controls ───────────────────────────────────────────────────────
    @FXML private ComboBox<String> timeHorizonCombo;
    @FXML private Label dateRangeSummaryLabel;

    // ── 8 3D KPI cards ────────────────────────────────────────────────────────
    @FXML private Text totalBooksText;
    @FXML private Text totalCopiesText;
    @FXML private Text booksTrendText;
    @FXML private Text netStockWorthText;
    @FXML private Text netStockWorthSub;
    @FXML private Label currentDateLabel;
    @FXML private HBox overdueAlertPill;
    @FXML private Text totalMembersText;
    @FXML private Text membersTrendText;
    @FXML private Text issuedBooksText;
    @FXML private Text issuedTodayText;
    @FXML private Text memberDuesText;
    @FXML private Text memberDuesSub;
    @FXML private Text finesCollectedText;
    @FXML private Text pendingFinesText;
    @FXML private Text overdueBooksText;
    @FXML private Text overdueRiskAccountsText;
    @FXML private Text availableBooksText;
    @FXML private Text availabilityRateText;

    // ── 4 BI Charts ───────────────────────────────────────────────────────────
    @FXML private PieChart bookStatusChart;
    @FXML private BarChart<String, Number> monthlyBarChart;
    @FXML private CategoryAxis barMonthAxis;
    @FXML private NumberAxis barCountAxis;
    @FXML private BarChart<String, Number> categoryChart;
    @FXML private CategoryAxis categoryAxis;
    @FXML private NumberAxis categoryCountAxis;
    @FXML private BarChart<String, Number> revenueChart;
    @FXML private CategoryAxis revenueMonthAxis;
    @FXML private NumberAxis revenueAmountAxis;

    // ── Activity table pulse ──────────────────────────────────────────────────
    @FXML private TableView<ActivityRecord> activityTable;
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

    private Node dashboardCenter;

    @FXML
    public void initialize() {
        setupTimeHorizon();
        setupTable();
        setupSearch();
        setActiveButton(dashboardBtn);
        loadDashboardAsync();
        if (AppConfig.getInstance().getBoolean(AppConfig.KEY_WEATHER_ENABLED, true)) {
            loadWeatherAsync();
        }
        if (rootPane != null) dashboardCenter = rootPane.getCenter();
    }

    private void setupTimeHorizon() {
        if (timeHorizonCombo != null) {
            timeHorizonCombo.setItems(FXCollections.observableArrayList(
                    "All Time", "Today", "This Week", "This Month", "This Year"
            ));
            timeHorizonCombo.setValue("All Time");
        }
    }

    @FXML
    private void handleTimeframeChange() {
        String val = timeHorizonCombo != null ? timeHorizonCombo.getValue() : "All Time";
        if (dateRangeSummaryLabel != null) {
            dateRangeSummaryLabel.setText("Aggregated analytics horizon: " + val + " (Auto-synchronized)");
        }
        handleRefresh();
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
        if (memberColumn != null) memberColumn.setCellValueFactory(new PropertyValueFactory<>("memberName"));
        if (bookColumn != null)   bookColumn  .setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        if (actionColumn != null) actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        if (dateColumn != null)   dateColumn  .setCellValueFactory(new PropertyValueFactory<>("date"));
        if (statusColumn != null) {
            statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
            statusColumn.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty);
                    if (empty || s == null) { setText(null); setStyle(""); return; }
                    setText(s);
                    setStyle(switch (s.toLowerCase()) {
                        case "issued"   -> "-fx-text-fill:#f59e0b; -fx-font-weight:bold;";
                        case "returned" -> "-fx-text-fill:#10b981; -fx-font-weight:bold;";
                        case "overdue"  -> "-fx-text-fill:#ef4444; -fx-font-weight:bold;";
                        default         -> "-fx-text-fill:#64748b;";
                    });
                }
            });
        }

        filteredActivity = new FilteredList<>(activityData, p -> true);
        if (activityTable != null) activityTable.setItems(filteredActivity);
    }

    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, val) -> applyFilter(val));
        }
    }

    @FXML
    private void handleSearch() {
        if (searchField != null) {
            applyFilter(searchField.getText());
        }
    }

    private void applyFilter(String val) {
        if (filteredActivity != null) {
            filteredActivity.setPredicate(r -> {
                if (val == null || val.isBlank()) return true;
                String lower = val.toLowerCase();
                return (r.getMemberName() != null && r.getMemberName().toLowerCase().contains(lower))
                    || (r.getBookTitle() != null && r.getBookTitle().toLowerCase().contains(lower))
                    || (r.getAction() != null && r.getAction().toLowerCase().contains(lower))
                    || (r.getStatus() != null && r.getStatus().toLowerCase().contains(lower));
            });
        }
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    @FXML
    private void handleRefresh() {
        DashboardCache.getInstance().invalidate();
        if (refreshBtn != null) {
            refreshBtn.setDisable(true);
            refreshBtn.setText("⏳");
        }
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
                if (refreshBtn != null) {
                    refreshBtn.setDisable(false);
                    refreshBtn.setText("🔄 Refresh");
                }
            },
            err -> {
                LOG.error("Dashboard load error", err);
                if (refreshBtn != null) {
                    refreshBtn.setDisable(false);
                    refreshBtn.setText("🔄 Refresh");
                }
                if (dashboardBtn != null && dashboardBtn.getScene() != null) {
                    ToastNotification.error(dashboardBtn.getScene(),
                        "Dashboard refresh failed: " + err.getMessage());
                }
            }
        );
    }

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

    private void updateWeatherWidget(WeatherInfo w) {
        Platform.runLater(() -> {
            if (weatherCityLabel != null) weatherCityLabel.setText(w.getCity());
            if (weatherTempLabel != null) weatherTempLabel.setText(String.format("%.1f°C", w.getTemperature()));
            if (weatherDescLabel != null) weatherDescLabel.setText(w.getDescription());
            if (weatherIconLabel != null) weatherIconLabel.setText(w.getIcon());
        });
    }

    // ── KPI cards ─────────────────────────────────────────────────────────────

    private void updateKpiCards(DashboardStats s) {
        String currency = AppConfig.getInstance().getCurrency();

        // 1. Total Books & Trend
        if (totalBooksText != null) totalBooksText.setText(String.valueOf(s.totalBooks));
        if (totalCopiesText != null) totalCopiesText.setText(String.format("%,d", s.totalCopies));
        if (currentDateLabel != null) currentDateLabel.setText(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")));
        if (booksTrendText != null) {
            boolean inStock = s.availableBooks >= s.issuedBooks;
            booksTrendText.setText(inStock ? "+12 This Month" : "▼ High Demand");
            booksTrendText.setStyle(inStock
                    ? "-fx-fill:#34d399; -fx-font-weight:bold; -fx-font-size:10px;"
                    : "-fx-fill:#ef4444; -fx-font-weight:bold; -fx-font-size:10px;");
        }

        // 2. Net Stock Worth
        if (netStockWorthText != null)
            netStockWorthText.setText(currency + " " + String.format("%,.2f", s.estimatedStockValue));
        if (netStockWorthSub != null)
            netStockWorthSub.setText(s.totalCopies + " Total Copies Catalogued");

        // 3. Members
        if (totalMembersText != null) totalMembersText.setText(String.valueOf(s.totalMembers));
        if (membersTrendText != null) {
            membersTrendText.setText(s.activeMembers > 0 ? "▲ " + s.activeMembers + " Active Borrowers" : "● Stable");
            membersTrendText.setStyle("-fx-fill:#059669; -fx-font-weight:bold; -fx-font-size:11px;");
        }

        // 4. Issued Books
        if (issuedBooksText != null) issuedBooksText.setText(String.valueOf(s.issuedBooks));
        if (issuedTodayText != null) issuedTodayText.setText(s.issuedToday + " checkouts today");

        // 5. Customer Dues / Receivables
        if (memberDuesText != null)
            memberDuesText.setText(currency + " " + String.format("%.2f", s.totalMemberDues));
        if (memberDuesSub != null)
            memberDuesSub.setText(s.totalMemberDues > 0 ? "Pending Collectible" : "Zero Arrears");

        // 6. Realized Revenue
        if (finesCollectedText != null)
            finesCollectedText.setText(currency + " " + String.format("%.2f", s.totalFinesCollected));
        if (pendingFinesText != null)
            pendingFinesText.setText("▲ 100% Realized");

        // 7. Overdue Risk
        if (overdueBooksText != null) overdueBooksText.setText(String.valueOf(s.overdueBooks));
        if (overdueRiskAccountsText != null)
            overdueRiskAccountsText.setText(s.overdueAccountsCount + " At-Risk Accounts");

        // 8. Availability Rate
        if (availableBooksText != null) availableBooksText.setText(String.valueOf(s.availableBooks));
        if (availabilityRateText != null) {
            double rate = s.totalCopies > 0 ? ((double) s.availableBooks / s.totalCopies) * 100.0 : 100.0;
            availabilityRateText.setText(String.format("%.1f%% Ready for Issue", rate));
        }

        // Overdue Alert Badge
        if (overdueAlertLabel != null) {
            if (s.overdueBooks > 0) {
                overdueAlertLabel.setText("⚠ " + s.overdueBooks + " Overdue");
                overdueAlertLabel.setVisible(true);
                overdueAlertLabel.setManaged(true);
            } else {
                overdueAlertLabel.setVisible(false);
                overdueAlertLabel.setManaged(false);
            }
        }
    }

    // ── Charts ────────────────────────────────────────────────────────────────

    private void loadCharts(DashboardStats s) {
        // Chart 1: Pie chart — book status distribution
        if (bookStatusChart != null) {
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("Available (" + s.availableBooks + ")", Math.max(s.availableBooks, 1)),
                new PieChart.Data("Issued ("    + s.issuedBooks    + ")", Math.max(s.issuedBooks, 1)),
                new PieChart.Data("Overdue ("   + s.overdueBooks   + ")", Math.max(s.overdueBooks, 1)),
                new PieChart.Data("Reserved ("  + s.reservations   + ")", Math.max(s.reservations, 1))
            );
            bookStatusChart.setData(pieData);
            bookStatusChart.setLegendVisible(true);
        }

        // Chart 2: Bar chart — monthly issued
        if (monthlyBarChart != null) {
            Map<String, Integer> monthly = txService.getMonthlyIssuedStats(12);
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Books Issued");
            if (monthly.isEmpty()) {
                series.getData().add(new XYChart.Data<>("Current", s.issuedBooks));
            } else {
                monthly.forEach((m, cnt) -> series.getData().add(new XYChart.Data<>(m, cnt)));
            }
            monthlyBarChart.getData().clear();
            monthlyBarChart.getData().add(series);
        }

        // Chart 3: Bar chart — category demand
        if (categoryChart != null) {
            XYChart.Series<String, Number> catSeries = new XYChart.Series<>();
            catSeries.setName("Books by Category");
            if (s.categoryDistribution.isEmpty()) {
                catSeries.getData().add(new XYChart.Data<>("General", Math.max(s.totalBooks, 1)));
            } else {
                s.categoryDistribution.forEach((cat, cnt) -> catSeries.getData().add(new XYChart.Data<>(cat, cnt)));
            }
            categoryChart.getData().clear();
            categoryChart.getData().add(catSeries);
        }

        // Chart 4: Bar chart — revenue performance
        if (revenueChart != null) {
            XYChart.Series<String, Number> revSeries = new XYChart.Series<>();
            revSeries.setName("Revenue (PKR)");
            if (s.monthlyRevenue.isEmpty()) {
                revSeries.getData().add(new XYChart.Data<>("Collected", s.totalFinesCollected));
                revSeries.getData().add(new XYChart.Data<>("Receivables", s.totalMemberDues));
            } else {
                s.monthlyRevenue.forEach((m, amt) -> revSeries.getData().add(new XYChart.Data<>(m, amt)));
            }
            revenueChart.getData().clear();
            revenueChart.getData().add(revSeries);
        }
    }

    private void loadActivityTable() {
        AsyncRunner.run(
            () -> txService.getRecentTransactions(15),
            (List<Transaction> txList) -> {
                activityData.clear();
                for (Transaction tx : txList) {
                    activityData.add(new ActivityRecord(
                        tx.getMemberName() != null ? tx.getMemberName() : "Member #" + tx.getMemberId(),
                        tx.getBookName()   != null ? tx.getBookName()   : "Book #" + tx.getBookId(),
                        tx.getStatus()     != null ? tx.getStatus()     : "Transaction",
                        tx.getIssueDate()  != null ? tx.getIssueDate().format(DATE_FMT) : "—",
                        tx.getStatus()
                    ));
                }
            },
            err -> LOG.error("Activity load error: {}", err.getMessage())
        );
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    public void showDashboard() {
        setActiveButton(dashboardBtn);
        if (moduleText != null) moduleText.setText("— Visual Analytics & BI Command Center");
        if (dashboardCenter != null && rootPane != null) {
            rootPane.setCenter(dashboardCenter);
            handleRefresh();
        }
    }

    @FXML public void showBooks()       { navigateCenter("/com/library/ui/AddBookForm.fxml",          "— Book Inventory Management",   booksBtn); }
    @FXML public void showMembers()     { navigateCenter("/com/library/ui/AddMemberForm.fxml",        "— Member & Customer Directory", membersBtn); }
    @FXML public void showIssueReturn() { navigateCenter("/com/library/ui/IssueReturnBooksForm.fxml", "— Circulation & Returns",      issueReturnBtn); }
    @FXML public void showEmployees()   { navigateCenter("/com/library/ui/EmployeeForm.fxml",         "— Employee Management",         employeesBtn); }
    @FXML public void showArchive()     { navigateCenter("/com/library/ui/ArchiveView.fxml",          "— Archive Management",          archiveBtn); }
    @FXML public void showReports()     { navigateCenter("/com/library/ui/ReportsView.fxml",          "— Enterprise Reports & BI",     reportsBtn); }
    @FXML public void showSettings()    { navigateCenter("/com/library/ui/Settings.fxml",             "— System Settings & Config",    settingsBtn); }

    public void navigateCenter(String fxmlPath, String moduleTitle, Button activeBtn) {
        setActiveButton(activeBtn);
        if (moduleText != null) moduleText.setText(moduleTitle);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();
            if (content instanceof BorderPane bp) {
                bp.setTop(null);
            }
            if (rootPane != null) {
                rootPane.setCenter(content);
            }
        } catch (IOException e) {
            LOG.error("Failed to load view {}: {}", fxmlPath, e.getMessage(), e);
            if (dashboardBtn != null && dashboardBtn.getScene() != null) {
                ToastNotification.error(dashboardBtn.getScene(),
                        "Could not load " + moduleTitle + ": " + e.getMessage());
            }
        }
    }

    public void goBackToDashboard() {
        showDashboard();
    }

    private void setActiveButton(Button btn) {
        List<Button> all = List.of(dashboardBtn, booksBtn, membersBtn,
                issueReturnBtn, employeesBtn, archiveBtn, reportsBtn, settingsBtn);
        all.forEach(b -> {
            if (b != null) {
                b.getStyleClass().remove("active");
                b.setStyle("");
            }
        });
        if (btn != null) {
            if (!btn.getStyleClass().contains("active"))
                btn.getStyleClass().add("active");
        }
    }

    @FXML
    private void toggleSidebar() {
        if (sidebar == null) return;
        boolean visible = sidebar.isVisible();
        sidebar.setVisible(!visible);
        sidebar.setManaged(!visible);
        sidebarToggleBtn.setText(visible ? "☰" : "✖");
    }

    @FXML
    private void toggleTheme() {
        if (rootPane == null || rootPane.getScene() == null) return;
        ThemeManager.getInstance().toggle(rootPane.getScene());
        themeToggleBtn.setText(ThemeManager.getInstance().isDark() ? "☀" : "🌙");
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/library/ui/LoginPage.fxml"));
            Stage stage = (Stage) rootPane.getScene().getWindow();
            boolean wasMaximized = stage.isMaximized();
            Scene scene = new Scene(loader.load(), 1100, 700);
            ThemeManager.getInstance().applyTheme(scene);
            stage.setScene(scene);
            stage.setTitle(LibraCoreApp.APP_NAME + " " + LibraCoreApp.APP_VERSION + " — Login");
            if (wasMaximized) stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            LOG.error("Logout error", e);
        }
    }
}
