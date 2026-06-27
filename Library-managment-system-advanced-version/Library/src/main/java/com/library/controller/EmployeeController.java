package com.library.controller;

import com.library.LibraCoreApp;
import com.library.config.AppConfig;
import com.library.config.ThemeManager;
import com.library.model.Employee;
import com.library.security.SessionManager;
import com.library.service.EmployeeService;
import com.library.service.PrintService;
import com.library.util.IdGenerator;
import com.library.util.ToastNotification;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

public class EmployeeController {

    @FXML private Button backBtn;
    @FXML private Button themeBtn;

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label            totalLabel;
    @FXML private Label            activeLabel;

    @FXML private TableView<Employee>              empTable;
    @FXML private TableColumn<Employee, String>    colId;
    @FXML private TableColumn<Employee, String>    colCode;
    @FXML private TableColumn<Employee, String>    colName;
    @FXML private TableColumn<Employee, String>    colDesig;
    @FXML private TableColumn<Employee, String>    colDept;
    @FXML private TableColumn<Employee, String>    colStatus;
    @FXML private TableColumn<Employee, String>    colActions;

    @FXML private Button           prevPageBtn;
    @FXML private Button           nextPageBtn;
    @FXML private Label            pageLabel;
    @FXML private ComboBox<String> pageSizeCombo;

    @FXML private Text      formTitle;
    @FXML private ImageView photoView;
    @FXML private TextField codeField;
    @FXML private TextField nameField;
    @FXML private TextField designationField;
    @FXML private TextField departmentField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextField contactField;
    @FXML private TextField emailField;
    @FXML private TextField cnicField;
    @FXML private TextField salaryField;
    @FXML private TextField addressField;
    @FXML private TextArea  notesArea;
    @FXML private Label     formErrorLabel;
    @FXML private Button    saveBtn;

    private final EmployeeService empService  = new EmployeeService();
    private final PrintService    printService = new PrintService();

    private final ObservableList<Employee> allEmps = FXCollections.observableArrayList();
    private FilteredList<Employee> filtered;
    private SortedList<Employee>   sorted;

    private int      currentPage = 1;
    private int      pageSize    = AppConfig.getInstance().getItemsPerPage();
    private int      totalPages  = 1;
    private Employee editing     = null;
    private byte[]   photoBytes  = null;

    @FXML
    public void initialize() {
        setupTable();
        setupCombos();
        loadPage();
        codeField.setText(IdGenerator.peek(IdGenerator.Type.EMPLOYEE));
        nameField   .textProperty().addListener((o,v,n) -> com.library.shared.ValidationUtil.clearError(nameField));
        contactField.textProperty().addListener((o,v,n) -> com.library.shared.ValidationUtil.clearError(contactField));
    }

    private void setupTable() {
        // colId shows serial number; colCode shows EP code (EP00000001)
        colId    .setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getSerialNo() > 0 ? String.valueOf(d.getValue().getSerialNo()) : String.valueOf(d.getValue().getEmpId())));
        colCode  .setCellValueFactory(d -> new SimpleStringProperty(nvl(d.getValue().getEmployeeCode())));
        colName  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colDesig .setCellValueFactory(d -> new SimpleStringProperty(nvl(d.getValue().getDesignation())));
        colDept  .setCellValueFactory(d -> new SimpleStringProperty(nvl(d.getValue().getDepartment())));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(nvl(d.getValue().getStatus())));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle(switch (s) {
                    case "Active"   -> "-fx-text-fill:#388e3c; -fx-font-weight:bold;";
                    case "Inactive" -> "-fx-text-fill:#f57c00; -fx-font-weight:bold;";
                    case "Archived" -> "-fx-text-fill:#9e9e9e; -fx-font-style:italic;";
                    default         -> "";
                });
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn    = new Button("✏");
            private final Button archiveBtn = new Button("📦");
            private final Button printBtn   = new Button("🖨");
            private final javafx.scene.layout.HBox box =
                    new javafx.scene.layout.HBox(3, editBtn, archiveBtn, printBtn);
            {
                editBtn   .setStyle("-fx-background-color:#1976d2; -fx-text-fill:white; -fx-background-radius:4; -fx-cursor:hand; -fx-font-size:10px;");
                archiveBtn.setStyle("-fx-background-color:#757575; -fx-text-fill:white; -fx-background-radius:4; -fx-cursor:hand; -fx-font-size:10px;");
                printBtn  .setStyle("-fx-background-color:#388e3c; -fx-text-fill:white; -fx-background-radius:4; -fx-cursor:hand; -fx-font-size:10px;");
                editBtn   .setOnAction(e -> populateForm(getTableView().getItems().get(getIndex())));
                archiveBtn.setOnAction(e -> toggleArchive(getTableView().getItems().get(getIndex())));
                printBtn  .setOnAction(e -> printSingle(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setGraphic(null); return; }
                Employee emp = getTableView().getItems().get(getIndex());
                boolean archived = Employee.STATUS_ARCHIVED.equals(emp.getStatus());
                archiveBtn.setText(archived ? "♻" : "📦");
                archiveBtn.setStyle(archived
                    ? "-fx-background-color:#388e3c; -fx-text-fill:white; -fx-background-radius:4; -fx-cursor:hand; -fx-font-size:10px;"
                    : "-fx-background-color:#757575; -fx-text-fill:white; -fx-background-radius:4; -fx-cursor:hand; -fx-font-size:10px;");
                setGraphic(box);
            }
        });

        empTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, e) -> { if (e != null) populateForm(e); });

        filtered = new FilteredList<>(allEmps, e -> true);
        sorted   = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(empTable.comparatorProperty());
        empTable.setItems(sorted);
    }

    private void setupCombos() {
        pageSizeCombo.setItems(FXCollections.observableArrayList("10", "25", "50", "100", "200"));
        pageSizeCombo.setValue(String.valueOf(pageSize));
        statusCombo.setItems(FXCollections.observableArrayList(
                Employee.STATUS_ACTIVE, Employee.STATUS_INACTIVE, Employee.STATUS_ARCHIVED));
        statusCombo.setValue(Employee.STATUS_ACTIVE);
        statusFilter.setItems(FXCollections.observableArrayList(
                "All Status", "Active", "Inactive", "Archived"));
        statusFilter.setValue("All Status");
    }

    private void loadPage() {
        List<Employee> page = empService.getAllEmployees(currentPage, pageSize);
        allEmps.setAll(page);
        int total = empService.getTotalEmployees();
        totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        pageLabel.setText("Page " + currentPage + " of " + totalPages);
        prevPageBtn.setDisable(currentPage <= 1);
        nextPageBtn.setDisable(currentPage >= totalPages);
        long active = page.stream().filter(e -> "Active".equals(e.getStatus())).count();
        totalLabel .setText("Total: " + total);
        activeLabel.setText("Active: " + active);
    }

    @FXML private void handleRefresh()    { loadPage(); }
    @FXML private void prevPage()         { if (currentPage > 1) { currentPage--; loadPage(); } }
    @FXML private void nextPage()         { if (currentPage < totalPages) { currentPage++; loadPage(); } }
    @FXML private void changePageSize()   { pageSize = Integer.parseInt(pageSizeCombo.getValue()); currentPage = 1; loadPage(); }

    @FXML private void handleSearch()       { applyFilter(); }
    @FXML private void handleStatusFilter() { applyFilter(); }

    private void applyFilter() {
        String q      = searchField.getText().trim().toLowerCase();
        String status = statusFilter.getValue();
        filtered.setPredicate(e -> {
            boolean mq = q.isBlank()
                || e.getName().toLowerCase().contains(q)
                || (e.getEmployeeCode() != null && e.getEmployeeCode().toLowerCase().contains(q))
                || (e.getEmail() != null && e.getEmail().toLowerCase().contains(q));
            boolean ms = status == null || status.equals("All Status") || status.equals(e.getStatus());
            return mq && ms;
        });
    }

    private void populateForm(Employee e) {
        editing    = e;
        photoBytes = e.getProfilePic();
        formTitle.setText("Edit Employee");
        saveBtn.setText("💾 Update Employee");
        codeField       .setText(nvl(e.getEmployeeCode()));
        nameField       .setText(e.getName());
        designationField.setText(nvl(e.getDesignation()));
        departmentField .setText(nvl(e.getDepartment()));
        statusCombo     .setValue(nvl(e.getStatus()));
        contactField    .setText(nvl(e.getContact()));
        emailField      .setText(nvl(e.getEmail()));
        cnicField       .setText(nvl(e.getCnic()));
        salaryField     .setText(String.valueOf(e.getSalary()));
        addressField    .setText(nvl(e.getAddress()));
        notesArea       .setText(nvl(e.getNotes()));
        if (photoBytes != null && photoBytes.length > 0)
            photoView.setImage(new Image(new ByteArrayInputStream(photoBytes)));
        hideError();
    }

    @FXML
    private void saveEmployee() {
        boolean ok = true;
        ok &= com.library.shared.ValidationUtil.requireNonBlank(nameField);
        ok &= com.library.shared.ValidationUtil.requireNonBlank(contactField);
        if (!ok) { showError("Please fill all required fields (*) correctly."); return; }
        hideError();

        Employee e = editing != null ? editing : new Employee();
        e.setName(nameField.getText().trim());
        e.setDesignation(designationField.getText().trim());
        e.setDepartment(departmentField.getText().trim());
        e.setStatus(statusCombo.getValue());
        e.setContact(contactField.getText().trim());
        e.setEmail(emailField.getText().trim());
        e.setCnic(cnicField.getText().trim());
        e.setAddress(addressField.getText().trim());
        e.setNotes(notesArea.getText().trim());
        e.setProfilePic(photoBytes);
        try { e.setSalary(Double.parseDouble(salaryField.getText().trim())); }
        catch (Exception ignored) { e.setSalary(0); }

        boolean saved = editing != null ? empService.updateEmployee(e) : empService.addEmployee(e);
        if (saved) {
            ToastNotification.success(backBtn.getScene(),
                    (editing != null ? "Employee updated: " : "Employee added: ") + e.getName());
            clearForm();
            loadPage();
        } else {
            showError("Failed to save employee.");
        }
    }

    private void toggleArchive(Employee e) {
        boolean isArchived = Employee.STATUS_ARCHIVED.equals(e.getStatus());
        boolean ok = isArchived ? empService.unarchiveEmployee(e.getEmpId())
                                : empService.archiveEmployee(e.getEmpId());
        if (ok) {
            ToastNotification.success(backBtn.getScene(),
                    "Employee " + (isArchived ? "restored" : "archived") + ": " + e.getName());
            loadPage();
        }
    }

    private void printSingle(Employee e) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Employee Profile PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName(e.getEmployeeCode() + "_profile.pdf");
        File f = fc.showSaveDialog(backBtn.getScene().getWindow());
        if (f == null) return;
        try (FileOutputStream fos = new FileOutputStream(f)) {
            printService.printEmployeeProfile(e, fos);
            ToastNotification.success(backBtn.getScene(), "PDF saved: " + f.getName());
        } catch (Exception ex) {
            ToastNotification.error(backBtn.getScene(), "Print failed: " + ex.getMessage());
        }
    }

    @FXML
    private void printList() {
        ToastNotification.info(backBtn.getScene(),
                "Employee list PDF — use Reports module for full export.");
    }

    @FXML
    private void printEmployee() {
        if (editing == null) { showError("Select an employee first."); return; }
        printSingle(editing);
    }

    @FXML
    private void uploadPhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Photo");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File f = fc.showOpenDialog(backBtn.getScene().getWindow());
        if (f == null) return;
        try {
            photoBytes = Files.readAllBytes(f.toPath());
            photoView.setImage(new Image(new ByteArrayInputStream(photoBytes)));
        } catch (IOException e) {
            ToastNotification.error(backBtn.getScene(), "Failed to load photo.");
        }
    }

    @FXML
    private void clearForm() {
        editing = null; photoBytes = null;
        formTitle.setText("Add New Employee");
        saveBtn.setText("💾 Save Employee");
        codeField.setText(IdGenerator.peek(IdGenerator.Type.EMPLOYEE));
        nameField.clear(); designationField.clear(); departmentField.clear();
        statusCombo.setValue(Employee.STATUS_ACTIVE);
        contactField.clear(); emailField.clear(); cnicField.clear();
        salaryField.clear(); addressField.clear(); notesArea.clear();
        photoView.setImage(null);
        hideError();
    }

    @FXML private void toggleTheme() {
        ThemeManager.getInstance().toggle(backBtn.getScene());
        themeBtn.setText(ThemeManager.getInstance().isDark() ? "☀" : "🌙");
    }

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

    private void showError(String msg) { formErrorLabel.setText(msg); formErrorLabel.setVisible(true); formErrorLabel.setManaged(true); }
    private void hideError()           { formErrorLabel.setVisible(false); formErrorLabel.setManaged(false); }
    private String nvl(String s)       { return s != null ? s : ""; }
}
