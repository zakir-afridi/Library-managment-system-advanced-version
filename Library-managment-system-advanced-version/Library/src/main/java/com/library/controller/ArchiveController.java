package com.library.controller;

import com.library.LibraCoreApp;
import com.library.config.ThemeManager;
import com.library.model.Book;
import com.library.model.Employee;
import com.library.model.Member;
import com.library.security.SessionManager;
import com.library.service.BookService;
import com.library.service.EmployeeService;
import com.library.service.GlobalSearchService;
import com.library.service.MemberService;
import com.library.util.ToastNotification;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class ArchiveController {

    @FXML private Button  backBtn;
    @FXML private Button  themeBtn;

    // Global search
    @FXML private TextField globalSearchField;
    @FXML private CheckBox  includeActiveCheck;
    @FXML private VBox      globalResultsPane;
    @FXML private TableView<GlobalSearchService.SearchResult> globalResultsTable;
    @FXML private TableColumn<GlobalSearchService.SearchResult, String> grType;
    @FXML private TableColumn<GlobalSearchService.SearchResult, String> grCode;
    @FXML private TableColumn<GlobalSearchService.SearchResult, String> grName;
    @FXML private TableColumn<GlobalSearchService.SearchResult, String> grStatus;
    @FXML private TableColumn<GlobalSearchService.SearchResult, String> grExtra;

    // Member archive tab
    @FXML private TableView<Member>              memberArchiveTable;
    @FXML private TableColumn<Member, String>    amId;
    @FXML private TableColumn<Member, String>    amCode;
    @FXML private TableColumn<Member, String>    amName;
    @FXML private TableColumn<Member, String>    amDept;
    @FXML private TableColumn<Member, String>    amDate;
    @FXML private TableColumn<Member, String>    amAction;

    // Book archive tab
    @FXML private TableView<Book>                bookArchiveTable;
    @FXML private TableColumn<Book, String>      abId;
    @FXML private TableColumn<Book, String>      abCode;
    @FXML private TableColumn<Book, String>      abTitle;
    @FXML private TableColumn<Book, String>      abAuthor;
    @FXML private TableColumn<Book, String>      abDate;
    @FXML private TableColumn<Book, String>      abAction;

    // Employee archive tab
    @FXML private TableView<Employee>            empArchiveTable;
    @FXML private TableColumn<Employee, String>  aeId;
    @FXML private TableColumn<Employee, String>  aeCode;
    @FXML private TableColumn<Employee, String>  aeName;
    @FXML private TableColumn<Employee, String>  aeDesig;
    @FXML private TableColumn<Employee, String>  aeDate;
    @FXML private TableColumn<Employee, String>  aeAction;

    private final MemberService       memberService  = new MemberService();
    private final BookService         bookService    = new BookService();
    private final EmployeeService     empService     = new EmployeeService();
    private final GlobalSearchService searchService  = new GlobalSearchService();

    @FXML
    public void initialize() {
        setupGlobalSearch();
        setupMemberArchiveTable();
        setupBookArchiveTable();
        setupEmpArchiveTable();
        loadAllArchives();
    }

    // ── Global search ─────────────────────────────────────────────────────────

    private void setupGlobalSearch() {
        grType  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().type));
        grCode  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().code));
        grName  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name));
        grStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status));
        grExtra .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().extra));

        grStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("Archived".equals(s)
                    ? "-fx-text-fill:#9e9e9e; -fx-font-style:italic;"
                    : "-fx-text-fill:#388e3c; -fx-font-weight:bold;");
            }
        });
    }

    @FXML
    private void handleGlobalSearch() {
        String q = globalSearchField.getText().trim();
        if (q.isBlank()) { clearSearch(); return; }
        boolean includeActive = includeActiveCheck.isSelected();
        List<GlobalSearchService.SearchResult> results = searchService.search(q, includeActive);
        globalResultsTable.setItems(FXCollections.observableArrayList(results));
        globalResultsPane.setVisible(true);
        globalResultsPane.setManaged(true);
    }

    @FXML
    private void clearSearch() {
        globalSearchField.clear();
        globalResultsPane.setVisible(false);
        globalResultsPane.setManaged(false);
        globalResultsTable.getItems().clear();
    }

    // ── Member archive table ──────────────────────────────────────────────────

    private void setupMemberArchiveTable() {
        amId  .setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getStdId())));
        amCode.setCellValueFactory(d -> new SimpleStringProperty(nvl(d.getValue().getStudentId())));
        amName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        amDept.setCellValueFactory(d -> new SimpleStringProperty(nvl(d.getValue().getDepartment())));
        amDate.setCellValueFactory(d -> {
            Member m = d.getValue();
            // archived_date not in model yet — show registration date as fallback
            return new SimpleStringProperty(m.getRegistrationDate() != null
                    ? m.getRegistrationDate().toString() : "");
        });
        amAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("♻ Restore");
            { btn.setStyle("-fx-background-color:#388e3c; -fx-text-fill:white; -fx-background-radius:5; -fx-cursor:hand; -fx-font-size:11px;");
              btn.setOnAction(e -> restoreMember(getTableView().getItems().get(getIndex()))); }
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty); setGraphic(empty ? null : btn); }
        });
    }

    private void restoreMember(Member m) {
        if (memberService.unarchiveMember(m.getStdId())) {
            ToastNotification.success(backBtn.getScene(), "Member restored: " + m.getName());
            loadAllArchives();
        }
    }

    // ── Book archive table ────────────────────────────────────────────────────

    private void setupBookArchiveTable() {
        abId    .setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getBookId())));
        abCode  .setCellValueFactory(d -> new SimpleStringProperty(nvl(d.getValue().getIsbn())));
        abTitle .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookName()));
        abAuthor.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAuthor()));
        abDate  .setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().toString() : ""));
        abAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("♻ Restore");
            { btn.setStyle("-fx-background-color:#388e3c; -fx-text-fill:white; -fx-background-radius:5; -fx-cursor:hand; -fx-font-size:11px;");
              btn.setOnAction(e -> restoreBook(getTableView().getItems().get(getIndex()))); }
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty); setGraphic(empty ? null : btn); }
        });
    }

    private void restoreBook(Book b) {
        if (bookService.unarchiveBook(b.getBookId())) {
            ToastNotification.success(backBtn.getScene(), "Book restored: " + b.getBookName());
            loadAllArchives();
        }
    }

    // ── Employee archive table ────────────────────────────────────────────────

    private void setupEmpArchiveTable() {
        aeId   .setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getEmpId())));
        aeCode .setCellValueFactory(d -> new SimpleStringProperty(nvl(d.getValue().getEmployeeCode())));
        aeName .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        aeDesig.setCellValueFactory(d -> new SimpleStringProperty(nvl(d.getValue().getDesignation())));
        aeDate .setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getArchivedDate() != null ? d.getValue().getArchivedDate().toString() : ""));
        aeAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("♻ Restore");
            { btn.setStyle("-fx-background-color:#388e3c; -fx-text-fill:white; -fx-background-radius:5; -fx-cursor:hand; -fx-font-size:11px;");
              btn.setOnAction(e -> restoreEmployee(getTableView().getItems().get(getIndex()))); }
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty); setGraphic(empty ? null : btn); }
        });
    }

    private void restoreEmployee(Employee e) {
        if (empService.unarchiveEmployee(e.getEmpId())) {
            ToastNotification.success(backBtn.getScene(), "Employee restored: " + e.getName());
            loadAllArchives();
        }
    }

    // ── Load all archives ─────────────────────────────────────────────────────

    private void loadAllArchives() {
        memberArchiveTable.setItems(FXCollections.observableArrayList(
                memberService.getArchivedMembers(1, 500)));
        bookArchiveTable.setItems(FXCollections.observableArrayList(
                bookService.getArchivedBooks()));
        empArchiveTable.setItems(FXCollections.observableArrayList(
                empService.getArchivedEmployees(1, 500)));
    }

    // ── Navigation ────────────────────────────────────────────────────────────

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

    private String nvl(String s) { return s != null ? s : ""; }
}
