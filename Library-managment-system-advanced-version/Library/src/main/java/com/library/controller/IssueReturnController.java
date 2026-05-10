package com.library.controller;

import com.library.LibraCoreApp;
import com.library.config.AppConfig;
import com.library.config.ThemeManager;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Transaction;
import com.library.security.SessionManager;
import com.library.service.BookService;
import com.library.service.MemberService;
import com.library.service.TransactionService;
import com.library.util.ToastNotification;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class IssueReturnController {

    // ── Top bar ───────────────────────────────────────────────────────────────
    @FXML private Button backBtn;
    @FXML private Button refreshBtn;

    // ── Operation toggle ──────────────────────────────────────────────────────
    @FXML private ToggleGroup operationToggleGroup;
    @FXML private RadioButton issueRadio;
    @FXML private RadioButton returnRadio;
    @FXML private RadioButton renewRadio;

    // ── Student search ────────────────────────────────────────────────────────
    @FXML private TextField studentSearchField;
    @FXML private Button    searchStudentBtn;
    @FXML private Button    clearStudentBtn;
    @FXML private VBox      studentDetailsCard;
    @FXML private ImageView studentPhotoView;
    @FXML private Text      studentNameText;
    @FXML private Text      studentIdText;
    @FXML private Text      libraryCardText;
    @FXML private Text      booksIssuedText;
    @FXML private Text      booksLimitText;
    @FXML private Text      overdueCountText;

    // ── Issued books table ────────────────────────────────────────────────────
    @FXML private TableView<Transaction>              issuedBooksTable;
    @FXML private TableColumn<Transaction, String>    issuedBookIdColumn;
    @FXML private TableColumn<Transaction, String>    issuedTitleColumn;
    @FXML private TableColumn<Transaction, String>    issuedAuthorColumn;
    @FXML private TableColumn<Transaction, String>    issueDateColumn;
    @FXML private TableColumn<Transaction, String>    dueDateColumn;
    @FXML private TableColumn<Transaction, String>    statusColumn;
    @FXML private TableColumn<Transaction, String>    actionColumn;

    // ── Book search ───────────────────────────────────────────────────────────
    @FXML private TextField bookSearchField;
    @FXML private VBox      bookSearchResults;
    @FXML private TableView<Book>              bookSearchTable;
    @FXML private TableColumn<Book, String>    bookIdColumn;
    @FXML private TableColumn<Book, String>    titleColumn;
    @FXML private TableColumn<Book, String>    authorColumn;
    @FXML private TableColumn<Book, String>    categoryColumn;
    @FXML private TableColumn<Book, String>    availableColumn;
    @FXML private TableColumn<Book, String>    locationColumn;
    @FXML private TableColumn<Book, String>    selectColumn;

    // ── Selected book card ────────────────────────────────────────────────────
    @FXML private VBox      selectedBookCard;
    @FXML private ImageView bookCoverView;
    @FXML private Text      selectedBookTitle;
    @FXML private Text      selectedBookAuthor;
    @FXML private Text      selectedBookId;
    @FXML private Text      selectedBookISBN;
    @FXML private Text      selectedBookCategory;
    @FXML private Text      selectedBookLocation;
    @FXML private Text      selectedBookAvailable;

    // ── Transaction details ───────────────────────────────────────────────────
    @FXML private VBox       transactionSection;
    @FXML private DatePicker issueDatePicker;
    @FXML private DatePicker dueDatePicker;
    @FXML private Spinner<Integer> issuePeriodSpinner;
    @FXML private TextField  fineAmountField;
    @FXML private TextField  issuedByField;
    @FXML private ComboBox<String> transactionTypeCombo;
    @FXML private TextArea   transactionNotesArea;

    // ── Action buttons ────────────────────────────────────────────────────────
    @FXML private Button processBtn;
    @FXML private Button processAndNewBtn;
    @FXML private Button clearAllBtn;
    @FXML private Button printReceiptBtn;

    // ── Services ──────────────────────────────────────────────────────────────
    private final BookService        bookService   = new BookService();
    private final MemberService      memberService = new MemberService();
    private final TransactionService txService     = new TransactionService();

    private Member  selectedMember;
    private Book    selectedBook;
    private Transaction selectedTransaction; // for return

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupIssuedBooksTable();
        setupBookSearchTable();
        setupTransactionDefaults();

        // Wire operation toggle
        operationToggleGroup.selectedToggleProperty().addListener(
                (obs, old, nw) -> onOperationChanged());

        // Wire spinner to auto-update due date
        issuePeriodSpinner.valueProperty().addListener(
                (obs, old, nw) -> updateDueDate());

        // Set logged-in user
        if (SessionManager.getInstance().isLoggedIn())
            issuedByField.setText(SessionManager.getInstance().getUsername());
    }

    // ── Table setup ───────────────────────────────────────────────────────────

    private void setupIssuedBooksTable() {
        issuedBookIdColumn .setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getBookId())));
        issuedTitleColumn  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookName()));
        issuedAuthorColumn .setCellValueFactory(d -> new SimpleStringProperty(""));
        issueDateColumn    .setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getIssueDate() != null ? d.getValue().getIssueDate().toString() : ""));
        dueDateColumn      .setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDueDate() != null ? d.getValue().getDueDate().toString() : ""));
        statusColumn       .setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().isOverdue() ? "⚠ Overdue" : d.getValue().getStatus()));

        // Return button in action column
        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Return");
            {
                btn.setStyle("-fx-background-color:#1976d2; -fx-text-fill:white;" +
                             "-fx-background-radius:6; -fx-font-size:11px; -fx-cursor:hand;");
                btn.setOnAction(e -> {
                    Transaction tx = getTableView().getItems().get(getIndex());
                    handleQuickReturn(tx);
                });
            }
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void setupBookSearchTable() {
        bookIdColumn   .setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getBookId())));
        titleColumn    .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookName()));
        authorColumn   .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAuthor()));
        categoryColumn .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));
        availableColumn.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getAvailableQty())));
        locationColumn .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getShelfLocation()));

        // Select button
        selectColumn.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Select");
            {
                btn.setStyle("-fx-background-color:#388e3c; -fx-text-fill:white;" +
                             "-fx-background-radius:6; -fx-font-size:11px; -fx-cursor:hand;");
                btn.setOnAction(e -> selectBook(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setGraphic(empty ? null : btn);
            }
        });

        // Double-click to select
        bookSearchTable.setRowFactory(tv -> {
            TableRow<Book> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) selectBook(row.getItem());
            });
            return row;
        });
    }

    private void setupTransactionDefaults() {
        issuePeriodSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 90,
                        AppConfig.getInstance().getLoanDays()));
        issueDatePicker.setValue(LocalDate.now());
        updateDueDate();
        transactionTypeCombo.setItems(FXCollections.observableArrayList(
                "Standard Loan", "Reference Only", "Short Loan (7 days)", "Extended Loan (30 days)"));
        transactionTypeCombo.setValue("Standard Loan");
    }

    private void updateDueDate() {
        if (issueDatePicker.getValue() != null && issuePeriodSpinner.getValue() != null)
            dueDatePicker.setValue(issueDatePicker.getValue()
                    .plusDays(issuePeriodSpinner.getValue()));
    }

    // ── Operation toggle ──────────────────────────────────────────────────────

    private void onOperationChanged() {
        boolean isReturn = returnRadio.isSelected();
        bookSearchField.setPromptText(isReturn
                ? "Enter Transaction ID or Book ID to return"
                : "Enter Book ID, Title, Author, or ISBN");
        fineAmountField.setEditable(isReturn);
        if (isReturn) fineAmountField.setPromptText("Auto-calculated on return");
    }

    // ── Student search ────────────────────────────────────────────────────────

    @FXML
    private void searchStudent() {
        String query = studentSearchField.getText().trim();
        if (query.isBlank()) return;

        Member m = null;
        // Try student ID first
        m = memberService.getMemberByStudentId(query);
        // Fallback to name search
        if (m == null) {
            List<Member> results = memberService.searchMembers(query);
            if (!results.isEmpty()) m = results.get(0);
        }

        if (m == null) {
            ToastNotification.warning(backBtn.getScene(), "No member found for: " + query);
            return;
        }
        populateStudentCard(m);
    }

    private void populateStudentCard(Member m) {
        selectedMember = m;
        studentNameText .setText(m.getName());
        studentIdText   .setText("Student ID: " + m.getStudentId());
        libraryCardText .setText("Card: " + m.getLibraryCardNumber());

        int active   = memberService.getActiveBookCount(m.getStdId());
        int overdue  = (int) txService.getMemberTransactions(m.getStdId())
                .stream().filter(Transaction::isOverdue).count();

        booksIssuedText.setText("Issued: " + active);
        booksLimitText .setText("Limit: "  + m.getBookLimit());
        overdueCountText.setText("Overdue: " + overdue);
        overdueCountText.setStyle(overdue > 0
                ? "-fx-fill:#d32f2f; -fx-font-weight:bold;"
                : "-fx-fill:#388e3c;");

        // Profile photo
        if (m.getProfilePic() != null && m.getProfilePic().length > 0) {
            studentPhotoView.setImage(
                    new Image(new ByteArrayInputStream(m.getProfilePic())));
        }

        // Load issued books
        List<Transaction> txList = txService.getMemberTransactions(m.getStdId())
                .stream().filter(t -> Transaction.STATUS_ISSUED.equals(t.getStatus()))
                .toList();
        issuedBooksTable.setItems(FXCollections.observableArrayList(txList));

        studentDetailsCard.setVisible(true);
        studentDetailsCard.setManaged(true);
        transactionSection.setVisible(true);
        transactionSection.setManaged(true);
    }

    @FXML
    private void clearStudent() {
        selectedMember = null;
        studentSearchField.clear();
        studentDetailsCard.setVisible(false);
        studentDetailsCard.setManaged(false);
        transactionSection.setVisible(false);
        transactionSection.setManaged(false);
    }

    // ── Book search ───────────────────────────────────────────────────────────

    @FXML
    private void searchBook() {
        String query = bookSearchField.getText().trim();
        if (query.isBlank()) return;

        List<Book> results = bookService.searchBooks(query);
        if (results.isEmpty()) {
            ToastNotification.warning(backBtn.getScene(), "No books found for: " + query);
            return;
        }
        bookSearchTable.setItems(FXCollections.observableArrayList(results));
        bookSearchResults.setVisible(true);
        bookSearchResults.setManaged(true);
    }

    @FXML
    private void scanBarcode() {
        // Simulate barcode scan — focus the field for manual entry
        bookSearchField.requestFocus();
        bookSearchField.setPromptText("Scan or type ISBN / Book ID...");
        ToastNotification.info(backBtn.getScene(), "Barcode scanner ready — scan or type ISBN");
    }

    private void selectBook(Book book) {
        selectedBook = book;
        selectedBookTitle   .setText(book.getBookName());
        selectedBookAuthor  .setText("Author: " + book.getAuthor());
        selectedBookId      .setText("Book ID: " + book.getBookId());
        selectedBookISBN    .setText("ISBN: " + (book.getIsbn() != null ? book.getIsbn() : "N/A"));
        selectedBookCategory.setText(book.getCategory() != null ? book.getCategory() : "");
        selectedBookLocation.setText("Location: " + (book.getShelfLocation() != null ? book.getShelfLocation() : "N/A"));
        selectedBookAvailable.setText("Available: " + book.getAvailableQty());
        selectedBookAvailable.setStyle(book.getAvailableQty() > 0
                ? "-fx-fill:#388e3c; -fx-font-weight:bold;"
                : "-fx-fill:#d32f2f; -fx-font-weight:bold;");

        if (book.getCoverImage() != null && book.getCoverImage().length > 0)
            bookCoverView.setImage(new Image(new ByteArrayInputStream(book.getCoverImage())));

        selectedBookCard.setVisible(true);
        selectedBookCard.setManaged(true);
        bookSearchResults.setVisible(false);
        bookSearchResults.setManaged(false);
    }

    // ── Process transaction ───────────────────────────────────────────────────

    @FXML
    private void processTransaction() {
        if (issueRadio.isSelected()) processIssue(false);
        else if (returnRadio.isSelected()) processReturn();
        else if (renewRadio.isSelected()) processRenew();
    }

    @FXML
    private void processAndNew() {
        if (issueRadio.isSelected()) processIssue(true);
    }

    private void processIssue(boolean andNew) {
        if (selectedMember == null) {
            ToastNotification.warning(backBtn.getScene(), "Please select a member first.");
            return;
        }
        if (selectedBook == null) {
            ToastNotification.warning(backBtn.getScene(), "Please select a book first.");
            return;
        }

        String issuedBy = issuedByField.getText().trim();
        String error = txService.issueBook(
                selectedBook.getBookId(), selectedMember.getStdId(), issuedBy);

        if (error.isEmpty()) {
            ToastNotification.success(backBtn.getScene(),
                    "✅ Book issued: " + selectedBook.getBookName()
                    + " → " + selectedMember.getName());
            if (andNew) clearAll();
            else refreshData();
        } else {
            ToastNotification.error(backBtn.getScene(), error);
        }
    }

    private void processReturn() {
        Transaction selected = issuedBooksTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            ToastNotification.warning(backBtn.getScene(),
                    "Select a book from the issued list to return.");
            return;
        }
        handleQuickReturn(selected);
    }

    private void handleQuickReturn(Transaction tx) {
        // Condition dialog
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Good",
                "Good", "Damaged", "Lost");
        dialog.setTitle("Return Book");
        dialog.setHeaderText("Return: " + tx.getBookName());
        dialog.setContentText("Book condition:");

        dialog.showAndWait().ifPresent(condition -> {
            double fine = txService.returnBook(tx.getTransactionId(), condition,
                    SessionManager.getInstance().getUsername());
            if (fine >= 0) {
                String msg = "Book returned successfully.";
                if (fine > 0) msg += " Fine: " + AppConfig.getInstance().getCurrency()
                        + " " + String.format("%.2f", fine);
                ToastNotification.success(backBtn.getScene(), msg);
                refreshData();
            } else {
                ToastNotification.error(backBtn.getScene(), "Return failed. Try again.");
            }
        });
    }

    private void processRenew() {
        Transaction selected = issuedBooksTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            ToastNotification.warning(backBtn.getScene(), "Select a book to renew.");
            return;
        }
        ToastNotification.info(backBtn.getScene(),
                "Renew feature — extends due date by " +
                AppConfig.getInstance().getLoanDays() + " days.");
    }

    // ── Other actions ─────────────────────────────────────────────────────────

    @FXML
    private void clearAll() {
        clearStudent();
        selectedBook = null;
        bookSearchField.clear();
        selectedBookCard.setVisible(false);
        selectedBookCard.setManaged(false);
        bookSearchResults.setVisible(false);
        bookSearchResults.setManaged(false);
        transactionNotesArea.clear();
        fineAmountField.clear();
    }

    @FXML
    private void refreshData() {
        if (selectedMember != null) {
            Member refreshed = memberService.getMemberById(selectedMember.getStdId());
            if (refreshed != null) populateStudentCard(refreshed);
        }
    }

    @FXML
    private void printReceipt() {
        ToastNotification.info(backBtn.getScene(),
                "Receipt printing — available in Phase 5 (Reports).");
    }

    @FXML
    private void viewHistory() {
        if (selectedMember == null) {
            ToastNotification.warning(backBtn.getScene(), "Select a member first.");
            return;
        }
        List<Transaction> history = txService.getMemberTransactions(selectedMember.getStdId());
        ToastNotification.info(backBtn.getScene(),
                selectedMember.getName() + " has " + history.size() + " total transactions.");
    }

    @FXML private void sendNotification() {
        ToastNotification.info(backBtn.getScene(), "Email notification logged.");
    }

    @FXML private void generateReport() {
        ToastNotification.info(backBtn.getScene(), "Reports — available in Phase 5.");
    }

    @FXML private void previewTransaction() {
        if (selectedMember == null || selectedBook == null) {
            ToastNotification.warning(backBtn.getScene(), "Select both member and book first.");
            return;
        }
        String preview = "Issue: " + selectedBook.getBookName()
                + "\nTo: " + selectedMember.getName()
                + "\nDue: " + dueDatePicker.getValue();
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Transaction Preview");
        a.setHeaderText("Preview");
        a.setContentText(preview);
        a.showAndWait();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void goBackToDashboard() {
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
