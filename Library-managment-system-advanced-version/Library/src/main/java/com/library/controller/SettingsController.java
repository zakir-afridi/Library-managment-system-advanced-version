package com.library.controller;

import com.library.config.AppConfig;
import com.library.config.ThemeManager;
import com.library.model.*;
import com.library.security.PasswordUtil;
import com.library.security.SessionManager;
import com.library.service.*;
import com.library.util.ToastNotification;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(SettingsController.class);

    // ── Services ──
    private final LibraryInfoService libraryInfoService = LibraryInfoService.getInstance();
    private final EmployeeService employeeService       = new EmployeeService();
    private final CategoryService categoryService       = new CategoryService();
    private final PublisherService publisherService     = new PublisherService();
    private final MeasurementService measurementService = new MeasurementService();
    private final UserService userService               = new UserService();
    private final BackupScheduler backupScheduler       = BackupScheduler.getInstance();

    // ── Header & Navigation ──
    @FXML private Button backBtn;
    @FXML private Button themeToggleBtn;
    @FXML private Label settingsSubtitleLabel;

    @FXML private Button tabInfoBtn;
    @FXML private Button tabStaffBtn;
    @FXML private Button tabCategoriesBtn;
    @FXML private Button tabPublishersBtn;
    @FXML private Button tabMeasurementBtn;
    @FXML private Button tabAppSettingsBtn;
    @FXML private Button tabAccountBtn;
    @FXML private Button tabDatabaseBtn;
    @FXML private Button tabPdfBtn;

    // ── Tab Panes ──
    @FXML private StackPane contentStack;
    @FXML private VBox infoPane;
    @FXML private VBox staffPane;
    @FXML private VBox categoriesPane;
    @FXML private VBox publishersPane;
    @FXML private VBox measurementPane;
    @FXML private VBox appSettingsPane;
    @FXML private VBox accountPane;
    @FXML private VBox databasePane;
    @FXML private VBox pdfPane;

    // ── Tab 1: Information Controls ──
    @FXML private TextField infoLibraryNameField;
    @FXML private TextField infoInstitutionField;
    @FXML private TextField infoEmailField;
    @FXML private TextField infoContactField;
    @FXML private TextField infoAddressField;
    @FXML private TextField infoWebsiteField;
    @FXML private Label infoStatusLabel;
    @FXML private Button saveInfoBtn;

    // ── Tab 2: Staff Controls ──
    @FXML private VBox staffFormBox;
    @FXML private Text staffFormTitle;
    @FXML private TextField staffNameInput;
    @FXML private ComboBox<String> staffRoleCombo;
    @FXML private TextField staffContactInput;
    @FXML private TextField staffEmailInput;
    @FXML private TextField staffDeptInput;
    @FXML private ComboBox<String> staffStatusCombo;
    @FXML private TextField staffSearchField;
    @FXML private ComboBox<String> staffRoleFilter;
    @FXML private ComboBox<String> staffStatusFilter;
    @FXML private TableView<Employee> staffTable;
    @FXML private TableColumn<Employee, String> colStaffCode;
    @FXML private TableColumn<Employee, String> colStaffName;
    @FXML private TableColumn<Employee, String> colStaffRole;
    @FXML private TableColumn<Employee, String> colStaffContact;
    @FXML private TableColumn<Employee, String> colStaffEmail;
    @FXML private TableColumn<Employee, String> colStaffStatus;
    @FXML private TableColumn<Employee, Void> colStaffActions;
    private Employee editingStaff = null;

    // ── Tab 3: Category Controls ──
    @FXML private VBox categoryFormBox;
    @FXML private Text categoryFormTitle;
    @FXML private TextField categoryNameInput;
    @FXML private TextField categoryDescInput;
    @FXML private ComboBox<String> categoryStatusCombo;
    @FXML private TextField categorySearchField;
    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, Integer> colCatId;
    @FXML private TableColumn<Category, String> colCatName;
    @FXML private TableColumn<Category, String> colCatDesc;
    @FXML private TableColumn<Category, String> colCatStatus;
    @FXML private TableColumn<Category, Void> colCatActions;
    private Category editingCategory = null;

    // ── Tab 4: Publisher Controls ──
    @FXML private VBox publisherFormBox;
    @FXML private Text publisherFormTitle;
    @FXML private TextField publisherNameInput;
    @FXML private TextField publisherContactInput;
    @FXML private TextField publisherAddressInput;
    @FXML private TextField publisherSearchField;
    @FXML private TableView<Publisher> publisherTable;
    @FXML private TableColumn<Publisher, Integer> colPubId;
    @FXML private TableColumn<Publisher, String> colPubName;
    @FXML private TableColumn<Publisher, String> colPubContact;
    @FXML private TableColumn<Publisher, String> colPubAddress;
    @FXML private TableColumn<Publisher, String> colPubStatus;
    @FXML private TableColumn<Publisher, Void> colPubActions;
    private Publisher editingPublisher = null;

    // ── Tab 5: Measurement Controls ──
    @FXML private HBox unitFormBox;
    @FXML private TextField unitNameInput;
    @FXML private TextField unitSymbolInput;
    @FXML private TableView<MeasurementUnit> unitTable;
    @FXML private TableColumn<MeasurementUnit, Integer> colUnitId;
    @FXML private TableColumn<MeasurementUnit, String> colUnitName;
    @FXML private TableColumn<MeasurementUnit, String> colUnitSymbol;
    @FXML private TableColumn<MeasurementUnit, String> colUnitStatus;
    @FXML private TableColumn<MeasurementUnit, Void> colUnitActions;

    // ── Tab 6: App Settings Controls ──
    @FXML private ComboBox<String> appCurrencyCombo;
    @FXML private TextField appTaxRateField;
    @FXML private Spinner<Integer> appLoanDaysSpinner;
    @FXML private Spinner<Double> appFineRateSpinner;
    @FXML private Spinner<Integer> appGracePeriodSpinner;
    @FXML private Spinner<Integer> appMaxBooksSpinner;
    @FXML private ComboBox<String> appDateFormatCombo;
    @FXML private ComboBox<Integer> appItemsPerPageCombo;

    // ── Tab 7: Account Settings Controls ──
    @FXML private TextField accountCurrentUsernameField;
    @FXML private TextField accountNewUsernameField;
    @FXML private PasswordField accountCurrentPasswordField;
    @FXML private PasswordField accountNewPasswordField;
    @FXML private PasswordField accountConfirmPasswordField;
    @FXML private TextField accountRecoveryKeyField;
    @FXML private Label accountStatusLabel;

    // ── Tab 8: Database Controls ──
    @FXML private Label dbBackupStatusLabel;

    // ── Tab 9: PDF Settings Controls ──
    @FXML private ComboBox<String> pdfPaperSizeCombo;
    @FXML private ComboBox<String> pdfLanguageCombo;
    @FXML private CheckBox pdfIssueSlipCheck;
    @FXML private CheckBox pdfReturnSlipCheck;
    @FXML private CheckBox pdfReceiptCheck;
    @FXML private CheckBox pdfReportCheck;
    @FXML private CheckBox pdfMemberReportCheck;
    @FXML private Label pdfStatusLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initThemeLabel();
        initNavigation();
        loadLibraryInfo();
        initStaffTab();
        initCategoriesTab();
        initPublishersTab();
        initMeasurementTab();
        initAppSettingsTab();
        initAccountTab();
        initPdfTab();
    }

    private void initThemeLabel() {
        if (themeToggleBtn != null) {
            boolean dark = ThemeManager.getInstance().isDark();
            themeToggleBtn.setText(dark ? "☀️ Light" : "🌙 Dark");
        }
    }

    // ── Sub-navigation Switching ──

    private void initNavigation() {
        showInfoTab();
    }

    private void switchTab(Button activeBtn, VBox targetPane) {
        Button[] buttons = {tabInfoBtn, tabStaffBtn, tabCategoriesBtn, tabPublishersBtn, tabMeasurementBtn, tabAppSettingsBtn, tabAccountBtn, tabDatabaseBtn, tabPdfBtn};
        for (Button b : buttons) {
            if (b != null) {
                b.getStyleClass().remove("active");
            }
        }
        if (activeBtn != null) {
            if (!activeBtn.getStyleClass().contains("active")) {
                activeBtn.getStyleClass().add("active");
            }
        }

        VBox[] panes = {infoPane, staffPane, categoriesPane, publishersPane, measurementPane, appSettingsPane, accountPane, databasePane, pdfPane};
        for (VBox p : panes) {
            if (p != null) {
                p.setVisible(false);
                p.setManaged(false);
            }
        }
        if (targetPane != null) {
            targetPane.setVisible(true);
            targetPane.setManaged(true);
        }
    }

    @FXML public void showInfoTab() { switchTab(tabInfoBtn, infoPane); loadLibraryInfo(); }
    @FXML public void showStaffTab() { switchTab(tabStaffBtn, staffPane); loadStaffTable(); }
    @FXML public void showCategoriesTab() { switchTab(tabCategoriesBtn, categoriesPane); loadCategoriesTable(); }
    @FXML public void showPublishersTab() { switchTab(tabPublishersBtn, publishersPane); loadPublishersTable(); }
    @FXML public void showMeasurementTab() { switchTab(tabMeasurementBtn, measurementPane); loadMeasurementTable(); }
    @FXML public void showAppSettingsTab() { switchTab(tabAppSettingsBtn, appSettingsPane); loadAppSettings(); }
    @FXML public void showAccountTab() { switchTab(tabAccountBtn, accountPane); loadAccountInfo(); }
    @FXML public void showDatabaseTab() { switchTab(tabDatabaseBtn, databasePane); }
    @FXML public void showPdfTab() { switchTab(tabPdfBtn, pdfPane); loadPdfSettings(); }

    // ── Tab 1: Information ──

    private void loadLibraryInfo() {
        LibraryInfo info = libraryInfoService.getLibraryInfo();
        if (infoLibraryNameField != null) infoLibraryNameField.setText(info.getLibraryName());
        if (infoInstitutionField != null) infoInstitutionField.setText(info.getInstitutionName());
        if (infoEmailField != null) infoEmailField.setText(info.getEmail());
        if (infoContactField != null) infoContactField.setText(info.getContactNumber());
        if (infoAddressField != null) infoAddressField.setText(info.getAddress());
        if (infoWebsiteField != null) infoWebsiteField.setText(info.getWebsite());
        if (settingsSubtitleLabel != null) {
            settingsSubtitleLabel.setText(info.getLibraryName() + " — System Preferences");
        }
    }

    @FXML
    public void saveLibraryInfo() {
        String libName = infoLibraryNameField.getText() != null ? infoLibraryNameField.getText().trim() : "";
        String instName = infoInstitutionField.getText() != null ? infoInstitutionField.getText().trim() : "";
        String email = infoEmailField.getText() != null ? infoEmailField.getText().trim() : "";
        String contact = infoContactField.getText() != null ? infoContactField.getText().trim() : "";
        String address = infoAddressField.getText() != null ? infoAddressField.getText().trim() : "";
        String website = infoWebsiteField.getText() != null ? infoWebsiteField.getText().trim() : "";

        if (libName.isEmpty()) {
            ToastNotification.warning(backBtn.getScene(), "Library Name is required.");
            return;
        }

        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            ToastNotification.warning(backBtn.getScene(), "Please enter a valid email address.");
            return;
        }

        LibraryInfo info = new LibraryInfo(libName, instName, email, contact, address, website);
        boolean saved = libraryInfoService.saveLibraryInfo(info);
        if (saved) {
            if (infoStatusLabel != null) infoStatusLabel.setText("✓ Information saved successfully!");
            if (settingsSubtitleLabel != null) settingsSubtitleLabel.setText(libName + " — System Preferences");
            ToastNotification.success(backBtn.getScene(), "Library information updated successfully.");
        } else {
            ToastNotification.error(backBtn.getScene(), "Could not save library information. Check database.");
        }
    }

    // ── Tab 2: Staff / Counters ──

    private void initStaffTab() {
        if (staffRoleCombo != null) {
            staffRoleCombo.setItems(FXCollections.observableArrayList(
                "Librarian", "Assistant Librarian", "Library Staff", "Counter Operator", "Administrator"
            ));
            staffRoleCombo.getSelectionModel().selectFirst();
        }
        if (staffStatusCombo != null) {
            staffStatusCombo.setItems(FXCollections.observableArrayList("Active", "Inactive"));
            staffStatusCombo.getSelectionModel().selectFirst();
        }
        if (staffRoleFilter != null) {
            staffRoleFilter.setItems(FXCollections.observableArrayList(
                "All Roles", "Librarian", "Assistant Librarian", "Library Staff", "Counter Operator", "Administrator"
            ));
            staffRoleFilter.getSelectionModel().selectFirst();
        }
        if (staffStatusFilter != null) {
            staffStatusFilter.setItems(FXCollections.observableArrayList("All Status", "Active", "Inactive"));
            staffStatusFilter.getSelectionModel().selectFirst();
        }

        if (colStaffCode != null) colStaffCode.setCellValueFactory(new PropertyValueFactory<>("employeeCode"));
        if (colStaffName != null) colStaffName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (colStaffRole != null) colStaffRole.setCellValueFactory(new PropertyValueFactory<>("designation"));
        if (colStaffContact != null) colStaffContact.setCellValueFactory(new PropertyValueFactory<>("contact"));
        if (colStaffEmail != null) colStaffEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        if (colStaffStatus != null) colStaffStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        if (colStaffActions != null) {
            colStaffActions.setCellFactory(param -> new TableCell<>() {
                private final Button editBtn = new Button("✏️ Edit");
                private final Button toggleBtn = new Button("🔄 Status");
                private final Button deleteBtn = new Button("🗑️");
                private final HBox box = new HBox(6, editBtn, toggleBtn, deleteBtn);
                {
                    box.setAlignment(Pos.CENTER);
                    editBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 7; -fx-background-color: #059669; -fx-text-fill: white; -fx-cursor: hand;");
                    toggleBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 7; -fx-background-color: #0d9488; -fx-text-fill: white; -fx-cursor: hand;");
                    deleteBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 7; -fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand;");

                    editBtn.setOnAction(e -> {
                        Employee emp = getTableView().getItems().get(getIndex());
                        editStaffMember(emp);
                    });
                    toggleBtn.setOnAction(e -> {
                        Employee emp = getTableView().getItems().get(getIndex());
                        toggleStaffStatus(emp);
                    });
                    deleteBtn.setOnAction(e -> {
                        Employee emp = getTableView().getItems().get(getIndex());
                        deleteStaffMember(emp);
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
        }
    }

    @FXML
    public void toggleAddStaffForm() {
        editingStaff = null;
        if (staffFormTitle != null) staffFormTitle.setText("New Staff Member / Counter Operator");
        clearStaffForm();
        if (staffFormBox != null) {
            staffFormBox.setVisible(!staffFormBox.isVisible());
            staffFormBox.setManaged(staffFormBox.isVisible());
        }
    }

    private void editStaffMember(Employee emp) {
        if (emp == null) return;
        editingStaff = emp;
        if (staffFormTitle != null) staffFormTitle.setText("Edit Staff: " + emp.getName() + " (" + emp.getEmployeeCode() + ")");
        if (staffNameInput != null) staffNameInput.setText(emp.getName());
        if (staffRoleCombo != null) staffRoleCombo.setValue(emp.getDesignation());
        if (staffContactInput != null) staffContactInput.setText(emp.getContact());
        if (staffEmailInput != null) staffEmailInput.setText(emp.getEmail());
        if (staffDeptInput != null) staffDeptInput.setText(emp.getDepartment());
        if (staffStatusCombo != null) staffStatusCombo.setValue(emp.getStatus());

        if (staffFormBox != null) {
            staffFormBox.setVisible(true);
            staffFormBox.setManaged(true);
        }
    }

    private void toggleStaffStatus(Employee emp) {
        if (emp == null) return;
        String newStatus = "Active".equalsIgnoreCase(emp.getStatus()) ? "Inactive" : "Active";
        emp.setStatus(newStatus);
        boolean ok = employeeService.updateEmployee(emp);
        if (ok) {
            ToastNotification.success(backBtn.getScene(), emp.getName() + " status set to " + newStatus);
            loadStaffTable();
        }
    }

    private void deleteStaffMember(Employee emp) {
        if (emp == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete staff member '" + emp.getName() + "' [" + emp.getEmployeeCode() + "]?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Delete Staff");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            boolean ok = employeeService.deleteEmployee(emp.getEmpId());
            if (ok) {
                ToastNotification.success(backBtn.getScene(), "Staff member deleted.");
                loadStaffTable();
            } else {
                ToastNotification.error(backBtn.getScene(), "Cannot delete staff member with linked activity.");
            }
        }
    }

    @FXML
    public void saveStaffMember() {
        String name = staffNameInput.getText() != null ? staffNameInput.getText().trim() : "";
        String role = staffRoleCombo.getValue() != null ? staffRoleCombo.getValue() : "Library Staff";
        String contact = staffContactInput.getText() != null ? staffContactInput.getText().trim() : "";
        String email = staffEmailInput.getText() != null ? staffEmailInput.getText().trim() : "";
        String dept = staffDeptInput.getText() != null ? staffDeptInput.getText().trim() : "Circulation";
        String status = staffStatusCombo.getValue() != null ? staffStatusCombo.getValue() : "Active";

        if (name.isEmpty()) {
            ToastNotification.warning(backBtn.getScene(), "Staff Name is required.");
            return;
        }

        if (editingStaff == null) {
            Employee emp = new Employee();
            emp.setName(name);
            emp.setDesignation(role);
            emp.setContact(contact);
            emp.setEmail(email);
            emp.setDepartment(dept);
            emp.setStatus(status);
            emp.setJoinDate(LocalDate.now());

            boolean ok = employeeService.addEmployee(emp);
            if (ok) {
                ToastNotification.success(backBtn.getScene(), "New staff member " + name + " registered.");
                cancelStaffForm();
                loadStaffTable();
            } else {
                ToastNotification.error(backBtn.getScene(), "Could not register staff member.");
            }
        } else {
            editingStaff.setName(name);
            editingStaff.setDesignation(role);
            editingStaff.setContact(contact);
            editingStaff.setEmail(email);
            editingStaff.setDepartment(dept);
            editingStaff.setStatus(status);

            boolean ok = employeeService.updateEmployee(editingStaff);
            if (ok) {
                ToastNotification.success(backBtn.getScene(), "Staff member " + name + " updated.");
                cancelStaffForm();
                loadStaffTable();
            } else {
                ToastNotification.error(backBtn.getScene(), "Could not update staff member.");
            }
        }
    }

    @FXML
    public void cancelStaffForm() {
        editingStaff = null;
        clearStaffForm();
        if (staffFormBox != null) {
            staffFormBox.setVisible(false);
            staffFormBox.setManaged(false);
        }
    }

    private void clearStaffForm() {
        if (staffNameInput != null) staffNameInput.clear();
        if (staffContactInput != null) staffContactInput.clear();
        if (staffEmailInput != null) staffEmailInput.clear();
        if (staffDeptInput != null) staffDeptInput.clear();
    }

    @FXML public void searchStaff() { filterStaff(); }
    @FXML public void filterStaff() {
        String q = staffSearchField != null ? staffSearchField.getText() : "";
        String role = staffRoleFilter != null ? staffRoleFilter.getValue() : "All Roles";
        String status = staffStatusFilter != null ? staffStatusFilter.getValue() : "All Status";

        List<Employee> all = employeeService.searchEmployees(q != null ? q : "");
        ObservableList<Employee> filtered = FXCollections.observableArrayList();
        for (Employee e : all) {
            boolean matchesRole = role == null || "All Roles".equals(role) || role.equalsIgnoreCase(e.getDesignation());
            boolean matchesStatus = status == null || "All Status".equals(status) || status.equalsIgnoreCase(e.getStatus());
            if (matchesRole && matchesStatus) {
                filtered.add(e);
            }
        }
        if (staffTable != null) staffTable.setItems(filtered);
    }

    @FXML
    public void loadStaffTable() {
        List<Employee> list = employeeService.getAllEmployees(1, 100);
        if (staffTable != null) staffTable.setItems(FXCollections.observableArrayList(list));
    }

    // ── Tab 3: Categories ──

    private void initCategoriesTab() {
        if (categoryStatusCombo != null) {
            categoryStatusCombo.setItems(FXCollections.observableArrayList("Active", "Inactive"));
            categoryStatusCombo.getSelectionModel().selectFirst();
        }
        if (colCatId != null) colCatId.setCellValueFactory(new PropertyValueFactory<>("categoryId"));
        if (colCatName != null) colCatName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (colCatDesc != null) colCatDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        if (colCatStatus != null) colCatStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        if (colCatActions != null) {
            colCatActions.setCellFactory(param -> new TableCell<>() {
                private final Button editBtn = new Button("✏️ Edit");
                private final Button toggleBtn = new Button("🔄 Status");
                private final Button deleteBtn = new Button("🗑️");
                private final HBox box = new HBox(6, editBtn, toggleBtn, deleteBtn);
                {
                    box.setAlignment(Pos.CENTER);
                    editBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 7; -fx-background-color: #059669; -fx-text-fill: white; -fx-cursor: hand;");
                    toggleBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 7; -fx-background-color: #0d9488; -fx-text-fill: white; -fx-cursor: hand;");
                    deleteBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 7; -fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand;");

                    editBtn.setOnAction(e -> editCategory(getTableView().getItems().get(getIndex())));
                    toggleBtn.setOnAction(e -> toggleCategoryStatus(getTableView().getItems().get(getIndex())));
                    deleteBtn.setOnAction(e -> deleteCategory(getTableView().getItems().get(getIndex())));
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
        }
    }

    @FXML
    public void toggleAddCategoryForm() {
        editingCategory = null;
        if (categoryFormTitle != null) categoryFormTitle.setText("New Book Category");
        if (categoryNameInput != null) categoryNameInput.clear();
        if (categoryDescInput != null) categoryDescInput.clear();
        if (categoryFormBox != null) {
            categoryFormBox.setVisible(!categoryFormBox.isVisible());
            categoryFormBox.setManaged(categoryFormBox.isVisible());
        }
    }

    private void editCategory(Category cat) {
        if (cat == null) return;
        editingCategory = cat;
        if (categoryFormTitle != null) categoryFormTitle.setText("Edit Category: " + cat.getName());
        if (categoryNameInput != null) categoryNameInput.setText(cat.getName());
        if (categoryDescInput != null) categoryDescInput.setText(cat.getDescription());
        if (categoryStatusCombo != null) categoryStatusCombo.setValue(cat.getStatus());
        if (categoryFormBox != null) {
            categoryFormBox.setVisible(true);
            categoryFormBox.setManaged(true);
        }
    }

    private void toggleCategoryStatus(Category cat) {
        if (cat == null) return;
        boolean ok = categoryService.toggleStatus(cat.getCategoryId(), cat.getStatus());
        if (ok) loadCategoriesTable();
    }

    private void deleteCategory(Category cat) {
        if (cat == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete category '" + cat.getName() + "'?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Delete Category");
        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.YES) {
            boolean ok = categoryService.deleteCategory(cat.getCategoryId());
            if (ok) {
                ToastNotification.success(backBtn.getScene(), "Category deleted.");
                loadCategoriesTable();
            } else {
                ToastNotification.error(backBtn.getScene(), "Could not delete category.");
            }
        }
    }

    @FXML
    public void saveCategory() {
        String name = categoryNameInput.getText() != null ? categoryNameInput.getText().trim() : "";
        String desc = categoryDescInput.getText() != null ? categoryDescInput.getText().trim() : "";
        String status = categoryStatusCombo.getValue() != null ? categoryStatusCombo.getValue() : "Active";

        if (name.isEmpty()) {
            ToastNotification.warning(backBtn.getScene(), "Category Name is required.");
            return;
        }

        if (editingCategory == null) {
            Category cat = new Category(0, name, desc, status);
            boolean ok = categoryService.addCategory(cat);
            if (ok) {
                ToastNotification.success(backBtn.getScene(), "Category '" + name + "' added.");
                cancelCategoryForm();
                loadCategoriesTable();
            } else {
                ToastNotification.error(backBtn.getScene(), "Could not add category (name may already exist).");
            }
        } else {
            editingCategory.setName(name);
            editingCategory.setDescription(desc);
            editingCategory.setStatus(status);
            boolean ok = categoryService.updateCategory(editingCategory);
            if (ok) {
                ToastNotification.success(backBtn.getScene(), "Category '" + name + "' updated.");
                cancelCategoryForm();
                loadCategoriesTable();
            } else {
                ToastNotification.error(backBtn.getScene(), "Could not update category.");
            }
        }
    }

    @FXML
    public void cancelCategoryForm() {
        editingCategory = null;
        if (categoryFormBox != null) {
            categoryFormBox.setVisible(false);
            categoryFormBox.setManaged(false);
        }
    }

    @FXML
    public void searchCategories() {
        String q = categorySearchField != null ? categorySearchField.getText() : "";
        List<Category> list = categoryService.searchCategories(q);
        if (categoryTable != null) categoryTable.setItems(FXCollections.observableArrayList(list));
    }

    @FXML
    public void loadCategoriesTable() {
        List<Category> list = categoryService.getAllCategories();
        if (categoryTable != null) categoryTable.setItems(FXCollections.observableArrayList(list));
    }

    // ── Tab 4: Publishers ──

    private void initPublishersTab() {
        if (colPubId != null) colPubId.setCellValueFactory(new PropertyValueFactory<>("publisherId"));
        if (colPubName != null) colPubName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (colPubContact != null) colPubContact.setCellValueFactory(new PropertyValueFactory<>("contact"));
        if (colPubAddress != null) colPubAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        if (colPubStatus != null) colPubStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        if (colPubActions != null) {
            colPubActions.setCellFactory(param -> new TableCell<>() {
                private final Button editBtn = new Button("✏️ Edit");
                private final Button toggleBtn = new Button("🔄 Status");
                private final Button deleteBtn = new Button("🗑️");
                private final HBox box = new HBox(6, editBtn, toggleBtn, deleteBtn);
                {
                    box.setAlignment(Pos.CENTER);
                    editBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 7; -fx-background-color: #059669; -fx-text-fill: white; -fx-cursor: hand;");
                    toggleBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 7; -fx-background-color: #0d9488; -fx-text-fill: white; -fx-cursor: hand;");
                    deleteBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 7; -fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand;");

                    editBtn.setOnAction(e -> editPublisher(getTableView().getItems().get(getIndex())));
                    toggleBtn.setOnAction(e -> togglePublisherStatus(getTableView().getItems().get(getIndex())));
                    deleteBtn.setOnAction(e -> deletePublisher(getTableView().getItems().get(getIndex())));
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
        }
    }

    @FXML
    public void toggleAddPublisherForm() {
        editingPublisher = null;
        if (publisherFormTitle != null) publisherFormTitle.setText("New Publisher");
        if (publisherNameInput != null) publisherNameInput.clear();
        if (publisherContactInput != null) publisherContactInput.clear();
        if (publisherAddressInput != null) publisherAddressInput.clear();
        if (publisherFormBox != null) {
            publisherFormBox.setVisible(!publisherFormBox.isVisible());
            publisherFormBox.setManaged(publisherFormBox.isVisible());
        }
    }

    private void editPublisher(Publisher pub) {
        if (pub == null) return;
        editingPublisher = pub;
        if (publisherFormTitle != null) publisherFormTitle.setText("Edit Publisher: " + pub.getName());
        if (publisherNameInput != null) publisherNameInput.setText(pub.getName());
        if (publisherContactInput != null) publisherContactInput.setText(pub.getContact());
        if (publisherAddressInput != null) publisherAddressInput.setText(pub.getAddress());
        if (publisherFormBox != null) {
            publisherFormBox.setVisible(true);
            publisherFormBox.setManaged(true);
        }
    }

    private void togglePublisherStatus(Publisher pub) {
        if (pub == null) return;
        boolean ok = publisherService.toggleStatus(pub.getPublisherId(), pub.getStatus());
        if (ok) loadPublishersTable();
    }

    private void deletePublisher(Publisher pub) {
        if (pub == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete publisher '" + pub.getName() + "'?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Delete Publisher");
        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.YES) {
            boolean ok = publisherService.deletePublisher(pub.getPublisherId());
            if (ok) {
                ToastNotification.success(backBtn.getScene(), "Publisher deleted.");
                loadPublishersTable();
            } else {
                ToastNotification.error(backBtn.getScene(), "Could not delete publisher.");
            }
        }
    }

    @FXML
    public void savePublisher() {
        String name = publisherNameInput.getText() != null ? publisherNameInput.getText().trim() : "";
        String contact = publisherContactInput.getText() != null ? publisherContactInput.getText().trim() : "";
        String address = publisherAddressInput.getText() != null ? publisherAddressInput.getText().trim() : "";

        if (name.isEmpty()) {
            ToastNotification.warning(backBtn.getScene(), "Publisher Name is required.");
            return;
        }

        if (editingPublisher == null) {
            Publisher pub = new Publisher(0, name, contact, address, "Active");
            boolean ok = publisherService.addPublisher(pub);
            if (ok) {
                ToastNotification.success(backBtn.getScene(), "Publisher '" + name + "' added.");
                cancelPublisherForm();
                loadPublishersTable();
            } else {
                ToastNotification.error(backBtn.getScene(), "Could not add publisher (duplicate name).");
            }
        } else {
            editingPublisher.setName(name);
            editingPublisher.setContact(contact);
            editingPublisher.setAddress(address);
            boolean ok = publisherService.updatePublisher(editingPublisher);
            if (ok) {
                ToastNotification.success(backBtn.getScene(), "Publisher '" + name + "' updated.");
                cancelPublisherForm();
                loadPublishersTable();
            } else {
                ToastNotification.error(backBtn.getScene(), "Could not update publisher.");
            }
        }
    }

    @FXML
    public void cancelPublisherForm() {
        editingPublisher = null;
        if (publisherFormBox != null) {
            publisherFormBox.setVisible(false);
            publisherFormBox.setManaged(false);
        }
    }

    @FXML
    public void searchPublishers() {
        String q = publisherSearchField != null ? publisherSearchField.getText() : "";
        List<Publisher> list = publisherService.searchPublishers(q);
        if (publisherTable != null) publisherTable.setItems(FXCollections.observableArrayList(list));
    }

    @FXML
    public void loadPublishersTable() {
        List<Publisher> list = publisherService.getAllPublishers();
        if (publisherTable != null) publisherTable.setItems(FXCollections.observableArrayList(list));
    }

    // ── Tab 5: Measurement ──

    private void initMeasurementTab() {
        if (colUnitId != null) colUnitId.setCellValueFactory(new PropertyValueFactory<>("unitId"));
        if (colUnitName != null) colUnitName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (colUnitSymbol != null) colUnitSymbol.setCellValueFactory(new PropertyValueFactory<>("symbol"));
        if (colUnitStatus != null) colUnitStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        if (colUnitActions != null) {
            colUnitActions.setCellFactory(param -> new TableCell<>() {
                private final Button deleteBtn = new Button("🗑️ Delete");
                {
                    deleteBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 7; -fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand;");
                    deleteBtn.setOnAction(e -> {
                        MeasurementUnit u = getTableView().getItems().get(getIndex());
                        if (u != null) {
                            measurementService.deleteUnit(u.getUnitId());
                            loadMeasurementTable();
                        }
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : deleteBtn);
                }
            });
        }
    }

    @FXML
    public void toggleAddUnitForm() {
        if (unitFormBox != null) {
            unitFormBox.setVisible(!unitFormBox.isVisible());
            unitFormBox.setManaged(unitFormBox.isVisible());
        }
    }

    @FXML
    public void cancelUnitForm() {
        if (unitFormBox != null) {
            unitFormBox.setVisible(false);
            unitFormBox.setManaged(false);
        }
    }

    @FXML
    public void saveUnit() {
        String name = unitNameInput != null ? unitNameInput.getText().trim() : "";
        String sym = unitSymbolInput != null ? unitSymbolInput.getText().trim() : "";
        if (name.isEmpty()) {
            ToastNotification.warning(backBtn.getScene(), "Unit Name is required.");
            return;
        }
        measurementService.addUnit(new MeasurementUnit(0, name, sym, "Active"));
        cancelUnitForm();
        loadMeasurementTable();
    }

    @FXML
    public void loadMeasurementTable() {
        List<MeasurementUnit> list = measurementService.getAllUnits();
        if (unitTable != null) unitTable.setItems(FXCollections.observableArrayList(list));
    }

    // ── Tab 6: App Settings ──

    private void initAppSettingsTab() {
        if (appCurrencyCombo != null) {
            appCurrencyCombo.setItems(FXCollections.observableArrayList("PKR", "USD ($)", "EUR (€)", "GBP (£)", "AED", "SAR", "INR (₹)"));
        }
        if (appDateFormatCombo != null) {
            appDateFormatCombo.setItems(FXCollections.observableArrayList("yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MMM-yyyy"));
        }
        if (appItemsPerPageCombo != null) {
            appItemsPerPageCombo.setItems(FXCollections.observableArrayList(10, 15, 20, 25, 50, 100));
        }
        if (appLoanDaysSpinner != null) {
            appLoanDaysSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 90, 14));
        }
        if (appFineRateSpinner != null) {
            appFineRateSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 100.0, 5.0, 0.5));
        }
        if (appGracePeriodSpinner != null) {
            appGracePeriodSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 30, 0));
        }
        if (appMaxBooksSpinner != null) {
            appMaxBooksSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 3));
        }
    }

    private void loadAppSettings() {
        AppConfig cfg = AppConfig.getInstance();
        if (appCurrencyCombo != null) appCurrencyCombo.setValue(libraryInfoService.getCurrency());
        if (appTaxRateField != null) appTaxRateField.setText(String.format("%.2f", libraryInfoService.getTaxRate()));
        if (appLoanDaysSpinner != null) appLoanDaysSpinner.getValueFactory().setValue(cfg.getLoanDays());
        if (appFineRateSpinner != null) appFineRateSpinner.getValueFactory().setValue(cfg.getFineRate());
        if (appGracePeriodSpinner != null) appGracePeriodSpinner.getValueFactory().setValue(cfg.getGracePeriod());
        if (appMaxBooksSpinner != null) appMaxBooksSpinner.getValueFactory().setValue(cfg.getInt("library.max_books_per_member", 3));
        if (appDateFormatCombo != null) appDateFormatCombo.setValue(cfg.getDateFormat());
        if (appItemsPerPageCombo != null) appItemsPerPageCombo.setValue(cfg.getDefaultLimit());
    }

    @FXML
    public void saveAppSettings() {
        AppConfig cfg = AppConfig.getInstance();
        if (appCurrencyCombo != null && appCurrencyCombo.getValue() != null) {
            libraryInfoService.saveCurrency(appCurrencyCombo.getValue());
        }
        if (appTaxRateField != null) {
            try {
                double tr = Double.parseDouble(appTaxRateField.getText().trim());
                libraryInfoService.saveTaxRate(tr);
            } catch (Exception ignored) {}
        }
        if (appLoanDaysSpinner != null) cfg.set(AppConfig.KEY_LOAN_DAYS, String.valueOf(appLoanDaysSpinner.getValue()));
        if (appFineRateSpinner != null) cfg.set(AppConfig.KEY_FINE_RATE, String.valueOf(appFineRateSpinner.getValue()));
        if (appGracePeriodSpinner != null) cfg.set(AppConfig.KEY_GRACE_PERIOD, String.valueOf(appGracePeriodSpinner.getValue()));
        if (appMaxBooksSpinner != null) cfg.set("library.max_books_per_member", String.valueOf(appMaxBooksSpinner.getValue()));
        if (appDateFormatCombo != null && appDateFormatCombo.getValue() != null) cfg.set(AppConfig.KEY_DATE_FORMAT, appDateFormatCombo.getValue());
        if (appItemsPerPageCombo != null && appItemsPerPageCombo.getValue() != null) cfg.set(AppConfig.KEY_DEFAULT_LIMIT, String.valueOf(appItemsPerPageCombo.getValue()));

        cfg.save();
        ToastNotification.success(backBtn.getScene(), "Application settings saved.");
    }

    // ── Tab 7: Account Settings ──

    private void initAccountTab() {
        loadAccountInfo();
    }

    private void loadAccountInfo() {
        if (accountCurrentUsernameField != null) {
            String u = SessionManager.getInstance().isLoggedIn() ? SessionManager.getInstance().getUsername() : "admin";
            accountCurrentUsernameField.setText(u);
        }
    }

    @FXML
    public void updateAccountSettings() {
        String curUsername = SessionManager.getInstance().isLoggedIn() ? SessionManager.getInstance().getUsername() : "admin";
        User dbUser = userService.getUserByUsername(curUsername);
        int userId = dbUser != null ? dbUser.getUserId() : 1;

        String curPass = accountCurrentPasswordField != null ? accountCurrentPasswordField.getText() : "";
        String newPass = accountNewPasswordField != null ? accountNewPasswordField.getText() : "";
        String confPass = accountConfirmPasswordField != null ? accountConfirmPasswordField.getText() : "";
        String newUsername = accountNewUsernameField != null ? accountNewUsernameField.getText().trim() : "";

        if (curPass.isEmpty()) {
            ToastNotification.warning(backBtn.getScene(), "Current Password is required to update account.");
            return;
        }

        if (dbUser == null || !PasswordUtil.verify(curPass, dbUser.getPasswordHash())) {
            ToastNotification.error(backBtn.getScene(), "Incorrect current password.");
            return;
        }

        // Update username if requested
        if (!newUsername.isEmpty() && !newUsername.equalsIgnoreCase(dbUser.getUsername())) {
            boolean userOk = userService.updateUsername(userId, newUsername);
            if (!userOk) {
                ToastNotification.error(backBtn.getScene(), "Username may already be taken.");
                return;
            }
            accountCurrentUsernameField.setText(newUsername);
        }

        // Update password if requested
        if (!newPass.isEmpty()) {
            if (newPass.length() < 6) {
                ToastNotification.warning(backBtn.getScene(), "New password must be at least 6 characters long.");
                return;
            }
            if (!newPass.equals(confPass)) {
                ToastNotification.error(backBtn.getScene(), "New password and Confirm Password do not match.");
                return;
            }
            boolean passOk = userService.changePassword(userId, curPass, newPass);
            if (!passOk) {
                ToastNotification.error(backBtn.getScene(), "Could not change password.");
                return;
            }
        }

        if (accountStatusLabel != null) accountStatusLabel.setText("✓ Account updated successfully.");
        ToastNotification.success(backBtn.getScene(), "Account credentials updated successfully.");
        if (accountCurrentPasswordField != null) accountCurrentPasswordField.clear();
        if (accountNewPasswordField != null) accountNewPasswordField.clear();
        if (accountConfirmPasswordField != null) accountConfirmPasswordField.clear();
    }

    // ── Tab 8: Database Backup & Restore ──

    @FXML
    public void backupDatabase() {
        try {
            Path path = backupScheduler.backup();
            if (dbBackupStatusLabel != null) {
                dbBackupStatusLabel.setText("✓ Backup created: " + path.getFileName());
            }
            ToastNotification.success(backBtn.getScene(), "Safe backup created at:\n" + path);
        } catch (Exception e) {
            LOG.error("Backup failed: {}", e.getMessage(), e);
            ToastNotification.error(backBtn.getScene(), "Backup failed: " + e.getMessage());
        }
    }

    @FXML
    public void restoreDatabase() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "Are you sure you want to restore the database?\nThis will REPLACE all current data with the backup file!",
            ButtonType.YES, ButtonType.NO);
        alert.setTitle("Restore Database");
        Optional<ButtonType> res = alert.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.YES) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Select SQLite Database Backup File");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Database Files (*.db, *.sqlite)", "*.db", "*.sqlite"));
        File f = fc.showOpenDialog(null);
        if (f != null && f.exists()) {
            try {
                boolean ok = backupScheduler.restore(f.toPath());
                if (ok) {
                    ToastNotification.success(backBtn.getScene(), "Database restored successfully.");
                    loadLibraryInfo();
                    loadStaffTable();
                    loadCategoriesTable();
                    loadPublishersTable();
                } else {
                    ToastNotification.error(backBtn.getScene(), "Could not restore database file.");
                }
            } catch (Exception e) {
                ToastNotification.error(backBtn.getScene(), "Restore Error: " + e.getMessage());
            }
        }
    }

    // ── Tab 9: PDF & Print Settings ──

    private void initPdfTab() {
        if (pdfPaperSizeCombo != null) {
            pdfPaperSizeCombo.setItems(FXCollections.observableArrayList(
                "A4 — 210 × 297 mm", "Letter — 8.5 × 11 in", "Legal — 8.5 × 14 in"
            ));
        }
        if (pdfLanguageCombo != null) {
            pdfLanguageCombo.setItems(FXCollections.observableArrayList(
                "English", "اردو (Urdu)"
            ));
        }
        loadPdfSettings();
    }

    private void loadPdfSettings() {
        if (pdfPaperSizeCombo != null) pdfPaperSizeCombo.setValue(libraryInfoService.getPdfPaperSize());
        if (pdfLanguageCombo != null) pdfLanguageCombo.setValue(libraryInfoService.getPdfLanguage());
    }

    @FXML
    public void savePdfSettings() {
        if (pdfPaperSizeCombo != null && pdfPaperSizeCombo.getValue() != null) {
            libraryInfoService.savePdfPaperSize(pdfPaperSizeCombo.getValue());
        }
        if (pdfLanguageCombo != null && pdfLanguageCombo.getValue() != null) {
            libraryInfoService.savePdfLanguage(pdfLanguageCombo.getValue());
        }
        if (pdfStatusLabel != null) pdfStatusLabel.setText("✓ PDF & print settings saved.");
        ToastNotification.success(backBtn.getScene(), "PDF & Print preferences saved successfully.");
    }

    // ── Navigation & Common ──

    @FXML
    public void handleLogout() {
        SessionManager.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/library/ui/LoginPage.fxml"));
            Stage stage = (Stage) backBtn.getScene().getWindow();
            Scene scene = new Scene(loader.load(), 1100, 700);
            ThemeManager.getInstance().applyTheme(scene);
            stage.setScene(scene);
        } catch (IOException e) {
            LOG.error("Logout error", e);
        }
    }

    @FXML
    public void goBack() {
        Stage stage = (Stage) backBtn.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/library/ui/ProfessionalDashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 800);
            ThemeManager.getInstance().applyTheme(scene);
            stage.setScene(scene);
        } catch (IOException e) {
            LOG.error("Failed to return to dashboard: {}", e.getMessage(), e);
        }
    }

    @FXML
    public void toggleTheme() {
        ThemeManager.getInstance().toggle(backBtn.getScene());
        initThemeLabel();
    }
}
