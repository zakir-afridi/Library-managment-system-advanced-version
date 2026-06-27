package com.library.controller;

import com.library.LibraCoreApp;
import com.library.cache.DashboardCache;
import com.library.config.AppConfig;
import com.library.config.ThemeManager;
import com.library.model.Member;
import com.library.security.SessionManager;
import com.library.service.MemberService;
import com.library.service.SearchService;
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

public class MemberController {

    // ── Top ───────────────────────────────────────────────────────────────────
    @FXML private Button backBtn;
    @FXML private Button themeBtn;

    // ── Table panel ───────────────────────────────────────────────────────────
    @FXML private TextField            searchField;
    @FXML private ComboBox<String>     departmentFilter;
    @FXML private ComboBox<String>     statusFilter;
    @FXML private Label                totalLabel;
    @FXML private Label                activeLabel;
    @FXML private Label                suspendedLabel;

    @FXML private TableView<Member>              memberTable;
    @FXML private TableColumn<Member, String>    colId;
    @FXML private TableColumn<Member, String>    colStudentId;
    @FXML private TableColumn<Member, String>    colName;
    @FXML private TableColumn<Member, String>    colDept;
    @FXML private TableColumn<Member, String>    colContact;
    @FXML private TableColumn<Member, String>    colStatus;
    @FXML private TableColumn<Member, String>    colFine;
    @FXML private TableColumn<Member, String>    colActions;

    @FXML private Button           prevPageBtn;
    @FXML private Button           nextPageBtn;
    @FXML private Label            pageLabel;
    @FXML private ComboBox<String> pageSizeCombo;

    // ── Form panel ────────────────────────────────────────────────────────────
    @FXML private Text      formTitle;
    @FXML private ImageView photoView;
    @FXML private TextField studentIdField;
    @FXML private TextField nameField;
    @FXML private TextField fnameField;
    @FXML private ComboBox<String> genderCombo;
    @FXML private ComboBox<String> bloodGroupCombo;
    @FXML private TextField contactField;
    @FXML private TextField emailField;
    @FXML private TextField departmentField;
    @FXML private ComboBox<String> programCombo;
    @FXML private ComboBox<String> semesterCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextField addressField;
    @FXML private TextField cityField;
    @FXML private TextField cnicField;
    @FXML private Label     fineBalanceLabel;
    @FXML private Label     formErrorLabel;
    @FXML private Button    saveBtn;

    // ── State ─────────────────────────────────────────────────────────────────
    private final MemberService  memberService = new MemberService();
    private final SearchService  searchService = SearchService.getInstance();

    private final ObservableList<Member> allMembers   = FXCollections.observableArrayList();
    private FilteredList<Member>         filteredMembers;
    private SortedList<Member>           sortedMembers;

    private int    currentPage = 1;
    private int    pageSize    = AppConfig.getInstance().getItemsPerPage();
    private int    totalPages  = 1;
    private Member editingMember = null;
    private byte[] photoBytes    = null;

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupTable();
        setupCombos();
        loadPage();
        generateStudentId();
        nameField   .textProperty().addListener((o,v,n) -> com.library.shared.ValidationUtil.clearError(nameField));
        contactField.textProperty().addListener((o,v,n) -> com.library.shared.ValidationUtil.clearError(contactField));
    }

    private void setupTable() {
        // colId shows serial number; colStudentId shows ST code
        colId       .setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getSerialNo() > 0 ? String.valueOf(d.getValue().getSerialNo()) : String.valueOf(d.getValue().getStdId())));
        colStudentId.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getMemberCode() != null ? d.getValue().getMemberCode() : nvl(d.getValue().getStudentId())));
        colName     .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colDept     .setCellValueFactory(d -> new SimpleStringProperty(nvl(d.getValue().getDepartment())));
        colContact  .setCellValueFactory(d -> new SimpleStringProperty(nvl(d.getValue().getContact())));
        colStatus   .setCellValueFactory(d -> new SimpleStringProperty(nvl(d.getValue().getStatus())));
        colFine     .setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFineBalance() > 0
                        ? AppConfig.getInstance().getCurrency() + " " +
                          String.format("%.0f", d.getValue().getFineBalance())
                        : "—"));

        // Colour-coded status
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle(switch (s) {
                    case "Active"    -> "-fx-text-fill:#388e3c; -fx-font-weight:bold;";
                    case "Suspended" -> "-fx-text-fill:#d32f2f; -fx-font-weight:bold;";
                    case "Expired"   -> "-fx-text-fill:#757575; -fx-font-weight:bold;";
                    case "Archived"  -> "-fx-text-fill:#9e9e9e; -fx-font-style:italic;";
                    default          -> "";
                });
            }
        });

        // Fine column — red if outstanding
        colFine.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle(s.equals("—") ? "" : "-fx-text-fill:#d32f2f; -fx-font-weight:bold;");
            }
        });

        // Edit / Archive / Restore buttons
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn    = new Button("✏");
            private final Button archiveBtn = new Button("📦");
            private final javafx.scene.layout.HBox box =
                    new javafx.scene.layout.HBox(4, editBtn, archiveBtn);
            {
                editBtn   .setStyle("-fx-background-color:#1976d2; -fx-text-fill:white; -fx-background-radius:5; -fx-cursor:hand; -fx-font-size:11px;");
                archiveBtn.setStyle("-fx-background-color:#757575; -fx-text-fill:white; -fx-background-radius:5; -fx-cursor:hand; -fx-font-size:11px;");
                editBtn   .setOnAction(e -> editMember(getTableView().getItems().get(getIndex())));
                archiveBtn.setOnAction(e -> toggleArchive(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setGraphic(null); return; }
                Member m = getTableView().getItems().get(getIndex());
                archiveBtn.setText("Archived".equals(m.getStatus()) ? "♻ Restore" : "📦 Archive");
                archiveBtn.setStyle("Archived".equals(m.getStatus())
                    ? "-fx-background-color:#388e3c; -fx-text-fill:white; -fx-background-radius:5; -fx-cursor:hand; -fx-font-size:10px;"
                    : "-fx-background-color:#757575; -fx-text-fill:white; -fx-background-radius:5; -fx-cursor:hand; -fx-font-size:10px;");
                setGraphic(box);
            }
        });

        // Click row to populate form
        memberTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, m) -> { if (m != null) populateForm(m); });

        filteredMembers = new FilteredList<>(allMembers, m -> true);
        sortedMembers   = new SortedList<>(filteredMembers);
        sortedMembers.comparatorProperty().bind(memberTable.comparatorProperty());
        memberTable.setItems(sortedMembers);
    }

    private void setupCombos() {
        pageSizeCombo.setItems(FXCollections.observableArrayList("10", "25", "50", "100", "200"));
        pageSizeCombo.setValue(String.valueOf(pageSize));

        genderCombo.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        bloodGroupCombo.setItems(FXCollections.observableArrayList(
                "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"));
        programCombo.setItems(FXCollections.observableArrayList(
                "BS", "MS", "PhD", "MBA", "BBA", "MPhil", "Diploma"));
        semesterCombo.setItems(FXCollections.observableArrayList(
                "1", "2", "3", "4", "5", "6", "7", "8"));
        statusCombo.setItems(FXCollections.observableArrayList(
                Member.STATUS_ACTIVE, Member.STATUS_SUSPENDED,
                Member.STATUS_EXPIRED, Member.STATUS_ARCHIVED));
        statusCombo.setValue(Member.STATUS_ACTIVE);

        statusFilter.setItems(FXCollections.observableArrayList(
                "All Status", "Active", "Suspended", "Expired", "Archived"));
        statusFilter.setValue("All Status");

        departmentFilter.setItems(FXCollections.observableArrayList("All Departments"));
        departmentFilter.setValue("All Departments");
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    /** Public API for MemberModule.refreshMemberData() */
    public void loadPage() {
        List<Member> page = memberService.getAllMembers(currentPage, pageSize);
        allMembers.setAll(page);

        int total = memberService.getTotalMembers();
        totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        pageLabel.setText("Page " + currentPage + " of " + totalPages);
        prevPageBtn.setDisable(currentPage <= 1);
        nextPageBtn.setDisable(currentPage >= totalPages);

        long active    = page.stream().filter(m -> "Active".equals(m.getStatus())).count();
        long suspended = page.stream().filter(m -> "Suspended".equals(m.getStatus())).count();
        totalLabel    .setText("Total: " + total);
        activeLabel   .setText("Active: " + active);
        suspendedLabel.setText("Suspended: " + suspended);
    }

    @FXML private void handleRefresh() { loadPage(); }
    @FXML private void prevPage() { if (currentPage > 1) { currentPage--; loadPage(); } }
    @FXML private void nextPage() { if (currentPage < totalPages) { currentPage++; loadPage(); } }
    @FXML private void changePageSize() {
        pageSize = Integer.parseInt(pageSizeCombo.getValue());
        currentPage = 1;
        loadPage();
    }

    // ── Search & Filter ───────────────────────────────────────────────────────

    @FXML private void handleSearch()     { applyFilter(); }
    @FXML private void handleDeptFilter() { applyFilter(); }
    @FXML private void handleStatusFilter(){ applyFilter(); }

    private void applyFilter() {
        String q      = searchField.getText().trim().toLowerCase();
        String dept   = departmentFilter.getValue();
        String status = statusFilter.getValue();

        filteredMembers.setPredicate(m -> {
            boolean matchQ = q.isBlank()
                    || m.getName().toLowerCase().contains(q)
                    || (m.getStudentId() != null && m.getStudentId().toLowerCase().contains(q))
                    || (m.getEmail()     != null && m.getEmail().toLowerCase().contains(q))
                    || (m.getContact()   != null && m.getContact().contains(q));

            boolean matchDept = dept == null || dept.equals("All Departments")
                    || dept.equals(m.getDepartment());

            boolean matchStatus = status == null || status.equals("All Status")
                    || status.equals(m.getStatus());

            return matchQ && matchDept && matchStatus;
        });
    }

    // ── Form operations ───────────────────────────────────────────────────────

    private void populateForm(Member m) {
        editingMember = m;
        formTitle.setText("Edit Member");
        saveBtn.setText("💾 Update Member");
        studentIdField .setText(nvl(m.getStudentId()));
        nameField      .setText(m.getName());
        fnameField     .setText(nvl(m.getFname()));
        genderCombo    .setValue(nvl(m.getGender()));
        bloodGroupCombo.setValue(nvl(m.getBloodGroup()));
        contactField   .setText(nvl(m.getContact()));
        emailField     .setText(nvl(m.getEmail()));
        departmentField.setText(nvl(m.getDepartment()));
        programCombo   .setValue(nvl(m.getProgram()));
        semesterCombo  .setValue(nvl(m.getSemester()));
        statusCombo    .setValue(nvl(m.getStatus()));
        addressField   .setText(nvl(m.getAddress()));
        cityField      .setText(nvl(m.getCity()));
        cnicField      .setText(nvl(m.getCnic()));
        fineBalanceLabel.setText(AppConfig.getInstance().getCurrency() + " " +
                String.format("%.2f", m.getFineBalance()));
        fineBalanceLabel.setStyle(m.getFineBalance() > 0
                ? "-fx-font-weight:bold; -fx-text-fill:#d32f2f;"
                : "-fx-font-weight:bold; -fx-text-fill:#388e3c;");
        photoBytes = m.getProfilePic();
        if (photoBytes != null && photoBytes.length > 0)
            photoView.setImage(new Image(new ByteArrayInputStream(photoBytes)));
        hideError();
    }

    @FXML
    private void saveMember() {
        if (!validateForm()) return;

        Member m = editingMember != null ? editingMember : new Member();
        m.setStudentId(studentIdField.getText().trim());
        m.setName(nameField.getText().trim());
        m.setFname(fnameField.getText().trim());
        m.setGender(genderCombo.getValue());
        m.setBloodGroup(bloodGroupCombo.getValue());
        m.setContact(contactField.getText().trim());
        m.setEmail(emailField.getText().trim());
        m.setDepartment(departmentField.getText().trim());
        m.setProgram(programCombo.getValue());
        m.setSemester(semesterCombo.getValue());
        m.setStatus(statusCombo.getValue());
        m.setAddress(addressField.getText().trim());
        m.setCity(cityField.getText().trim());
        m.setCnic(cnicField.getText().trim());
        m.setProfilePic(photoBytes);

        boolean ok = editingMember != null
                ? memberService.updateMember(m)
                : memberService.addMember(m);

        if (ok) {
            searchService.addMember(m.getName());
            DashboardCache.getInstance().invalidate();
            ToastNotification.success(backBtn.getScene(),
                    (editingMember != null ? "Member updated: " : "Member added: ") + m.getName());
            clearForm();
            loadPage();
        } else {
            showError("Failed to save member. Email may already exist.");
        }
    }

    private void editMember(Member m)   { populateForm(m); }

    private void toggleArchive(Member m) {
        boolean isArchived = Member.STATUS_ARCHIVED.equals(m.getStatus());
        String action = isArchived ? "Restore" : "Archive";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(action + " Member");
        confirm.setHeaderText(action + ": " + m.getName() + "?");
        confirm.setContentText(isArchived
            ? "This will restore the member to Active status."
            : "This will move the member to Archived Students. They will not appear in normal operations.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                boolean ok = isArchived
                    ? memberService.unarchiveMember(m.getStdId())
                    : memberService.archiveMember(m.getStdId());
                if (ok) {
                    DashboardCache.getInstance().invalidate();
                    ToastNotification.success(backBtn.getScene(),
                        "Member " + (isArchived ? "restored" : "archived") + ": " + m.getName());
                    loadPage();
                } else {
                    ToastNotification.error(backBtn.getScene(), action + " failed.");
                }
            }
        });
    }

    private void deleteMember(Member m) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Member");
        confirm.setHeaderText("Permanently delete: " + m.getName() + "?");
        confirm.setContentText("Consider archiving instead. Delete cannot be undone.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                if (memberService.deleteMember(m.getStdId())) {
                    DashboardCache.getInstance().invalidate();
                    ToastNotification.success(backBtn.getScene(), "Member deleted.");
                    loadPage();
                } else {
                    ToastNotification.error(backBtn.getScene(),
                            "Cannot delete — member has active transactions. Archive instead.");
                }
            }
        });
    }

    @FXML
    private void clearForm() {
        editingMember = null;
        photoBytes    = null;
        formTitle.setText("Add New Member");
        saveBtn.setText("💾 Save Member");
        studentIdField.clear(); nameField.clear(); fnameField.clear();
        genderCombo.setValue(null); bloodGroupCombo.setValue(null);
        contactField.clear(); emailField.clear(); departmentField.clear();
        programCombo.setValue(null); semesterCombo.setValue(null);
        statusCombo.setValue(Member.STATUS_ACTIVE);
        addressField.clear(); cityField.clear(); cnicField.clear();
        fineBalanceLabel.setText(AppConfig.getInstance().getCurrency() + " 0.00");
        photoView.setImage(null);
        hideError();
        generateStudentId();
    }

    @FXML
    private void generateStudentId() {
        studentIdField.setText(memberService.generateStudentId());
    }

    @FXML
    private void uploadPhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Profile Photo");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
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
    private void clearFine() {
        if (editingMember == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Fine");
        confirm.setHeaderText("Clear fine for " + editingMember.getName() + "?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                memberService.clearFine(editingMember.getStdId());
                fineBalanceLabel.setText(AppConfig.getInstance().getCurrency() + " 0.00");
                fineBalanceLabel.setStyle("-fx-font-weight:bold; -fx-text-fill:#388e3c;");
                ToastNotification.success(backBtn.getScene(), "Fine cleared.");
                loadPage();
            }
        });
    }

    // ── Import / Export ───────────────────────────────────────────────────────

    @FXML
    private void importCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Import Members from CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File f = fc.showOpenDialog(backBtn.getScene().getWindow());
        if (f == null) return;

        int imported = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] p = line.split(",", -1);
                if (p.length < 3) continue;
                Member m = new Member();
                m.setName(p[0].trim());
                m.setContact(p.length > 1 ? p[1].trim() : "");
                m.setEmail(p.length > 2 ? p[2].trim() : "");
                m.setDepartment(p.length > 3 ? p[3].trim() : "");
                if (memberService.addMember(m)) imported++;
            }
        } catch (IOException e) {
            ToastNotification.error(backBtn.getScene(), "Import error: " + e.getMessage());
            return;
        }
        searchService.reload();
        DashboardCache.getInstance().invalidate();
        ToastNotification.success(backBtn.getScene(), "Imported " + imported + " members.");
        loadPage();
    }

    @FXML
    private void exportExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Members to CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fc.setInitialFileName("members_export.csv");
        File f = fc.showSaveDialog(backBtn.getScene().getWindow());
        if (f == null) return;

        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            pw.println("ID,StudentID,Name,FatherName,Gender,Contact,Email,Department,Program,Semester,Status,FineBalance");
            for (Member m : memberService.getAllMembers(1, Integer.MAX_VALUE)) {
                pw.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%.2f%n",
                        m.getStdId(), nvl(m.getStudentId()), esc(m.getName()),
                        esc(nvl(m.getFname())), nvl(m.getGender()),
                        nvl(m.getContact()), nvl(m.getEmail()),
                        nvl(m.getDepartment()), nvl(m.getProgram()),
                        nvl(m.getSemester()), nvl(m.getStatus()),
                        m.getFineBalance());
            }
            ToastNotification.success(backBtn.getScene(), "Exported to: " + f.getName());
        } catch (IOException e) {
            ToastNotification.error(backBtn.getScene(), "Export error: " + e.getMessage());
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private boolean validateForm() {
        boolean ok = true;
        ok &= com.library.shared.ValidationUtil.requireNonBlank(nameField);
        ok &= com.library.shared.ValidationUtil.requireNonBlank(contactField);
        if (!ok) showError("Please fill all required fields (*) correctly.");
        else hideError();
        return ok;
    }

    private void showError(String msg) {
        formErrorLabel.setText(msg);
        formErrorLabel.setVisible(true);
        formErrorLabel.setManaged(true);
    }
    private void hideError() {
        formErrorLabel.setVisible(false);
        formErrorLabel.setManaged(false);
    }

    // ── Theme / Navigation ────────────────────────────────────────────────────

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

    private String nvl(String s) { return s != null ? s : ""; }
    private String esc(String s) { return s != null ? "\"" + s.replace("\"", "\"\"") + "\"" : ""; }
}
