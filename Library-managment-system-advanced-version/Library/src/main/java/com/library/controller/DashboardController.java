package com.library.controller;

import com.library.LibraCoreApp;
import com.library.cache.DashboardCache;
import com.library.cache.DashboardStats;
import com.library.config.AppConfig;
import com.library.config.ThemeManager;
import com.library.model.ActivityRecord;
import com.library.model.Transaction;
import com.library.model.User;
import com.library.security.SessionManager;
import com.library.service.TransactionService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class DashboardController {

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
            DateTimeFormatter.ofPattern(AppConfig.getInstance().getDateFormat()
                    .replace("DD","dd").replace("MM","MM").replace("YYYY","yyyy"));

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupTable();
        setupSearch();
        setActiveButton(dashboardBtn);
        loadDashboardAsync();
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
    }

    private void loadDashboardAsync() {
        Task<DashboardStats> task = new Task<>() {
            @Override protected DashboardStats call() {
                return DashboardCache.getInstance().getStats();
            }
        };
        task.setOnSucceeded(e -> {
            DashboardStats s = task.getValue();
            updateKpiCards(s);
            loadCharts(s);
            loadActivityTable();
            refreshBtn.setDisable(false);
            refreshBtn.setText("🔄 Refresh");
        });
        task.setOnFailed(e -> {
            refreshBtn.setDisable(false);
            refreshBtn.setText("🔄 Refresh");
        });
        Thread t = new Thread(task, "dashboard-loader");
        t.setDaemon(true);
        t.start();
    }

    // ── KPI cards ─────────────────────────────────────────────────────────────

    private void updateKpiCards(DashboardStats s) {
        String currency = AppConfig.getInstance().getCurrency();

        totalBooksText   .setText(String.valueOf(s.totalBooks));
        totalMembersText .setText(String.valueOf(s.activeMembers));
        issuedBooksText  .setText(String.valueOf(s.issuedBooks));
        overdueBooksText .setText(String.valueOf(s.overdueBooks));
        availableBooksText.setText(String.valueOf(s.availableBooks));
        issuedTodayText  .setText(s.issuedToday + " today");
        finesCollectedText.setText(currency + " " + String.format("%.2f", s.totalFinesCollected));
        pendingFinesText .setText(currency + " " + String.format("%.2f", s.pendingFines));

        // Overdue alert badge
        if (s.overdueBooks > 0) {
            overdueAlertLabel.setText("⚠ " + s.overdueBooks + " Overdue");
            overdueAlertLabel.setVisible(true);
            overdueAlertLabel.setManaged(true);
        } else {
            overdueAlertLabel.setVisible(false);
            overdueAlertLabel.setManaged(false);
        }

        // Trend indicators (simple: compare issued vs available)
        booksTrendText  .setText(s.availableBooks > s.issuedBooks ? "↑" : "↓");
        membersTrendText.setText(s.activeMembers > 0 ? "↑" : "→");
        booksTrendText  .setStyle(s.availableBooks > s.issuedBooks
                ? "-fx-fill:#388e3c; -fx-font-weight:bold;"
                : "-fx-fill:#d32f2f; -fx-font-weight:bold;");
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

    @FXML private void showDashboard()   { setActiveButton(dashboardBtn);  moduleText.setText("— Dashboard"); }
    @FXML private void showBooks()       { setActiveButton(booksBtn);       navigate("/com/library/ui/AddBookForm.fxml",   "— Books",   booksBtn); }
    @FXML private void showMembers()     { setActiveButton(membersBtn);     navigate("/com/library/ui/AddMemberForm.fxml", "— Members", membersBtn); }
    @FXML private void showIssueReturn() { setActiveButton(issueReturnBtn); navigate("/com/library/ui/IssueReturnBooksForm.fxml", "— Issue/Return", issueReturnBtn); }
    @FXML private void showEmployees()   { setActiveButton(employeesBtn);   navigate("/com/library/ui/EmployeeForm.fxml", "— Employees", employeesBtn); }
    @FXML private void showArchive()     { setActiveButton(archiveBtn);     navigate("/com/library/ui/ArchiveView.fxml", "— Archive", archiveBtn); }
    @FXML private void showReports()     { setActiveButton(reportsBtn);     navigate("/com/library/ui/ReportsView.fxml", "— Reports", reportsBtn); }
    @FXML private void showSettings()    { setActiveButton(settingsBtn);    navigate("/com/library/ui/Settings.fxml", "— Settings", settingsBtn); }

    // ── Quick actions ─────────────────────────────────────────────────────────

    @FXML private void addNewBook()    { navigate("/com/library/ui/AddBookForm.fxml",   "— Add Book",   booksBtn); }
    @FXML private void addNewMember()  { navigate("/com/library/ui/AddMemberForm.fxml", "— Add Member", membersBtn); }
    @FXML private void issueBook()     { navigate("/com/library/ui/IssueReturnBooksForm.fxml", "— Issue Book", issueReturnBtn); }
    @FXML private void returnBook()    { navigate("/com/library/ui/IssueReturnBooksForm.fxml", "— Return Book", issueReturnBtn); }
    @FXML private void generateReport(){ showToast("Reports module — Phase 4"); }

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

    private void navigate(String fxml, String module, Button activeBtn) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            if (loader.getLocation() == null) {
                showToast("Cannot find: " + fxml);
                return;
            }
            Stage stage = (Stage) dashboardBtn.getScene().getWindow();
            boolean wasMaximized = stage.isMaximized();
            // Reuse existing Scene — just swap root to avoid window jump/minimize
            Scene scene = dashboardBtn.getScene();
            scene.setRoot(loader.load());
            ThemeManager.getInstance().applyTheme(scene);
            stage.setTitle(LibraCoreApp.APP_NAME + " " + LibraCoreApp.APP_VERSION + " " + module);
            if (wasMaximized) stage.setMaximized(true);
        } catch (IOException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            showToast("Cannot load " + module + ":\n" + cause.getMessage());
            cause.printStackTrace();
        }
    }

    private void setActiveButton(Button active) {
        for (Button b : new Button[]{dashboardBtn, booksBtn, membersBtn,
                issueReturnBtn, employeesBtn, archiveBtn, reportsBtn, settingsBtn}) {
            b.getStyleClass().remove("active");
        }
        if (!active.getStyleClass().contains("active"))
            active.getStyleClass().add("active");
    }

    private void showToast(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("LibraCore Pro");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
