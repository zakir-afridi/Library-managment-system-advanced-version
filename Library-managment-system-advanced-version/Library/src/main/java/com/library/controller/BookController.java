package com.library.controller;

import com.library.LibraCoreApp;
import com.library.cache.DashboardCache;
import com.library.config.AppConfig;
import com.library.config.ThemeManager;
import com.library.model.Book;
import com.library.model.BookMetadata;
import com.library.scanner.BarcodeScanner;
import com.library.security.SessionManager;
import com.library.service.BookMetadataCacheService;
import com.library.service.BookService;
import com.library.service.SearchService;
import com.library.util.AsyncRunner;
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
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

public class BookController {

    private static final Logger LOG = LoggerFactory.getLogger(BookController.class);

    // ── Top ───────────────────────────────────────────────────────────────────
    @FXML private Button backBtn;
    @FXML private Button themeBtn;

    // ── Table panel ───────────────────────────────────────────────────────────
    @FXML private TextField            searchField;
    @FXML private ComboBox<String>     categoryFilter;
    @FXML private ComboBox<String>     statusFilter;
    @FXML private Label                totalLabel;
    @FXML private Label                availableLabel;
    @FXML private Label                issuedLabel;

    @FXML private TableView<Book>              bookTable;
    @FXML private TableColumn<Book, String>    colId;
    @FXML private TableColumn<Book, String>    colIsbn;
    @FXML private TableColumn<Book, String>    colTitle;
    @FXML private TableColumn<Book, String>    colAuthor;
    @FXML private TableColumn<Book, String>    colCategory;
    @FXML private TableColumn<Book, String>    colQty;
    @FXML private TableColumn<Book, String>    colAvail;
    @FXML private TableColumn<Book, String>    colStatus;
    @FXML private TableColumn<Book, String>    colActions;

    @FXML private Button           prevPageBtn;
    @FXML private Button           nextPageBtn;
    @FXML private Label            pageLabel;
    @FXML private ComboBox<String> pageSizeCombo;

    // ── Form panel ────────────────────────────────────────────────────────────
    @FXML private Text      formTitle;
    @FXML private ImageView coverImageView;
    @FXML private TextField isbnField;
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField publisherField;
    @FXML private TextField yearField;
    @FXML private TextField editionField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField quantityField;
    @FXML private TextField shelfField;
    @FXML private TextArea  descriptionArea;
    @FXML private Label     formErrorLabel;
    @FXML private Button    saveBtn;

    // ── State ─────────────────────────────────────────────────────────────────
    private final BookService               bookService   = new BookService();
    private final SearchService             searchService = SearchService.getInstance();
    private final BookMetadataCacheService  metaCache     = BookMetadataCacheService.getInstance();

    private final ObservableList<Book> allBooks    = FXCollections.observableArrayList();
    private FilteredList<Book>         filteredBooks;
    private SortedList<Book>           sortedBooks;

    private int     currentPage = 1;
    private int     pageSize    = AppConfig.getInstance().getItemsPerPage();
    private int     totalPages  = 1;
    private Book    editingBook = null;
    private byte[]  coverBytes  = null;

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupTable();
        setupCombos();
        loadPage();
        // Clear red border as soon as user starts typing
        titleField   .textProperty().addListener((o,v,n) -> com.library.shared.ValidationUtil.clearError(titleField));
        authorField  .textProperty().addListener((o,v,n) -> com.library.shared.ValidationUtil.clearError(authorField));
        quantityField.textProperty().addListener((o,v,n) -> com.library.shared.ValidationUtil.clearError(quantityField));
        // v3: Auto-fill form when ISBN changes (on focus lost)
        isbnField.setOnAction(e -> lookupIsbn());
        isbnField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && editingBook == null) lookupIsbn(); // only for new books
        });
    }

    // ── v3: ISBN auto-fill from Open Library ─────────────────────────────────

    /**
     * Triggered when the ISBN field loses focus or Enter is pressed.
     * Fetches metadata from Open Library (cached) and auto-fills the form.
     */
    @FXML
    private void lookupIsbn() {
        String isbn = isbnField.getText().trim();
        if (isbn.isBlank() || isbn.length() < 10) return;
        if (editingBook != null) return; // don't overwrite existing book data

        ToastNotification.info(backBtn.getScene(), "Looking up ISBN " + isbn + "...");
        AsyncRunner.run(
            () -> metaCache.getMetadata(isbn),
            optMeta -> {
                if (optMeta.isEmpty()) {
                    ToastNotification.warning(backBtn.getScene(),
                        "No metadata found for ISBN " + isbn + ". Fill in manually.");
                    return;
                }
                BookMetadata m = optMeta.get();
                if (titleField.getText().isBlank() && m.getTitle() != null)
                    titleField.setText(m.getTitle());
                if (authorField.getText().isBlank() && m.getAuthor() != null)
                    authorField.setText(m.getAuthor());
                if (publisherField.getText().isBlank() && m.getPublisher() != null)
                    publisherField.setText(m.getPublisher());
                if (yearField.getText().isBlank() && m.extractYear() > 0)
                    yearField.setText(String.valueOf(m.extractYear()));
                if (descriptionArea.getText().isBlank() && m.getDescription() != null)
                    descriptionArea.setText(m.getDescription());
                if (categoryCombo.getValue() == null && m.getCategory() != null)
                    categoryCombo.setValue(m.getCategory());
                ToastNotification.success(backBtn.getScene(),
                    "Auto-filled: " + m.getTitle());
                LOG.info("ISBN auto-fill: {}", m.getTitle());
            },
            err -> {
                LOG.warn("ISBN lookup failed: {}", err.getMessage());
                ToastNotification.warning(backBtn.getScene(), "ISBN lookup failed — fill in manually.");
            }
        );
    }

    /** Decode a barcode/QR from an image file (user selects the file). */
    @FXML
    private void scanBarcodeFromFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Barcode / QR Image");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File f = fc.showOpenDialog(backBtn.getScene().getWindow());
        if (f == null) return;
        try {
            byte[] imageBytes = Files.readAllBytes(f.toPath());
            BarcodeScanner.decodeFromImageBytes(imageBytes).ifPresentOrElse(
                code -> {
                    isbnField.setText(code);
                    lookupIsbn();
                },
                () -> ToastNotification.warning(backBtn.getScene(),
                    "No barcode found in image.")
            );
        } catch (IOException e) {
            ToastNotification.error(backBtn.getScene(), "Could not read image: " + e.getMessage());
        }
    }

    private void setupTable() {
        // colId shows serial number (display order, resequences on add/remove)
        colId      .setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getSerialNo() > 0 ? String.valueOf(d.getValue().getSerialNo()) : String.valueOf(d.getValue().getBookId())));
        // colIsbn shows structured BK code (BK00000001)
        colIsbn    .setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getBookCode() != null ? d.getValue().getBookCode() : nvl(d.getValue().getIsbn())));
        colTitle   .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookName()));
        colAuthor  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAuthor()));
        colCategory.setCellValueFactory(d -> new SimpleStringProperty(nvl(d.getValue().getCategory())));
        colQty     .setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getQuantity())));
        colAvail   .setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getAvailableQty())));
        colStatus  .setCellValueFactory(d -> new SimpleStringProperty(nvl(d.getValue().getStatus())));

        // Colour-coded status
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle(switch (s) {
                    case "Available" -> "-fx-text-fill:#388e3c; -fx-font-weight:bold;";
                    case "Issued"    -> "-fx-text-fill:#f57c00; -fx-font-weight:bold;";
                    case "Overdue"   -> "-fx-text-fill:#d32f2f; -fx-font-weight:bold;";
                    case "Archived"  -> "-fx-text-fill:#9e9e9e; -fx-font-style:italic;";
                    default          -> "-fx-text-fill:#757575;";
                });
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
                editBtn   .setOnAction(e -> editBook(getTableView().getItems().get(getIndex())));
                archiveBtn.setOnAction(e -> toggleArchiveBook(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setGraphic(null); return; }
                Book b = getTableView().getItems().get(getIndex());
                boolean archived = Book.STATUS_ARCHIVED.equals(b.getStatus());
                archiveBtn.setText(archived ? "♻" : "📦");
                archiveBtn.setStyle(archived
                    ? "-fx-background-color:#388e3c; -fx-text-fill:white; -fx-background-radius:5; -fx-cursor:hand;"
                    : "-fx-background-color:#757575; -fx-text-fill:white; -fx-background-radius:5; -fx-cursor:hand;");
                setGraphic(box);
            }
        });

        // Click row to populate form
        bookTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, book) -> { if (book != null) populateForm(book); });

        filteredBooks = new FilteredList<>(allBooks, b -> true);
        sortedBooks   = new SortedList<>(filteredBooks);
        sortedBooks.comparatorProperty().bind(bookTable.comparatorProperty());
        bookTable.setItems(sortedBooks);
    }

    private void setupCombos() {
        pageSizeCombo.setItems(FXCollections.observableArrayList("10", "25", "50", "100", "200"));
        pageSizeCombo.setValue(String.valueOf(pageSize));

        statusFilter.setItems(FXCollections.observableArrayList(
                "All Status", "Available", "Issued", "Overdue", "Lost", "Damaged", "Archived"));
        statusFilter.setValue("All Status");

        List<String> cats = bookService.getAllCategories();
        categoryFilter.setItems(FXCollections.observableArrayList());
        categoryFilter.getItems().add("All Categories");
        categoryFilter.getItems().addAll(cats);
        categoryFilter.setValue("All Categories");

        categoryCombo.setItems(FXCollections.observableArrayList(cats));
        categoryCombo.setEditable(true);
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadPage() {
        List<Book> page = bookService.getAllBooks(currentPage, pageSize);
        allBooks.setAll(page);

        int total = bookService.getTotalBooks();
        totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        pageLabel.setText("Page " + currentPage + " of " + totalPages);
        prevPageBtn.setDisable(currentPage <= 1);
        nextPageBtn.setDisable(currentPage >= totalPages);

        long avail  = page.stream().mapToLong(Book::getAvailableQty).sum();
        long issued = page.stream().filter(b -> b.getAvailableQty() < b.getQuantity()).count();
        totalLabel    .setText("Total: " + total);
        availableLabel.setText("Available: " + avail);
        issuedLabel   .setText("Issued: "    + issued);

        // Refresh category combo
        List<String> cats = bookService.getAllCategories();
        categoryCombo.getItems().setAll(cats);
        categoryFilter.getItems().setAll("All Categories");
        categoryFilter.getItems().addAll(cats);
    }

    /** Public API for BookModule.refreshBookData() */
    public void loadBooks() { loadPage(); }

    @FXML private void handleRefresh() { loadPage(); }

    @FXML private void prevPage() { if (currentPage > 1) { currentPage--; loadPage(); } }
    @FXML private void nextPage() { if (currentPage < totalPages) { currentPage++; loadPage(); } }
    @FXML private void changePageSize() {
        pageSize = Integer.parseInt(pageSizeCombo.getValue());
        currentPage = 1;
        loadPage();
    }

    // ── Search & Filter ───────────────────────────────────────────────────────

    @FXML
    private void handleSearch() {
        String q = searchField.getText().trim().toLowerCase();
        applyFilter(q, categoryFilter.getValue(), statusFilter.getValue());
    }

    @FXML private void handleCategoryFilter() { handleSearch(); }
    @FXML private void handleStatusFilter()   { handleSearch(); }

    private void applyFilter(String q, String cat, String status) {
        filteredBooks.setPredicate(b -> {
            boolean matchQ = q.isBlank()
                    || b.getBookName().toLowerCase().contains(q)
                    || b.getAuthor().toLowerCase().contains(q)
                    || (b.getIsbn() != null && b.getIsbn().toLowerCase().contains(q))
                    || (b.getCategory() != null && b.getCategory().toLowerCase().contains(q));

            boolean matchCat = cat == null || cat.equals("All Categories")
                    || cat.equals(b.getCategory());

            boolean matchStatus = status == null || status.equals("All Status")
                    || status.equals(b.getStatus());

            return matchQ && matchCat && matchStatus;
        });
    }

    // ── Form operations ───────────────────────────────────────────────────────

    private void populateForm(Book b) {
        editingBook = b;
        formTitle.setText("Edit Book");
        saveBtn.setText("💾 Update Book");
        isbnField      .setText(nvl(b.getIsbn()));
        titleField     .setText(b.getBookName());
        authorField    .setText(b.getAuthor());
        publisherField .setText(nvl(b.getPublisher()));
        yearField      .setText(b.getPublicationYear() > 0 ? String.valueOf(b.getPublicationYear()) : "");
        editionField   .setText(nvl(b.getEdition()));
        categoryCombo  .setValue(nvl(b.getCategory()));
        quantityField  .setText(String.valueOf(b.getQuantity()));
        shelfField     .setText(nvl(b.getShelfLocation()));
        descriptionArea.setText(nvl(b.getDescription()));
        coverBytes = b.getCoverImage();
        if (coverBytes != null && coverBytes.length > 0)
            coverImageView.setImage(new Image(new ByteArrayInputStream(coverBytes)));
        hideError();
    }

    @FXML
    private void saveBook() {
        if (!validateForm()) return;

        Book b = editingBook != null ? editingBook : new Book();
        b.setIsbn(isbnField.getText().trim());
        b.setBookName(titleField.getText().trim());
        b.setAuthor(authorField.getText().trim());
        b.setPublisher(publisherField.getText().trim());
        b.setPublicationYear(parseIntSafe(yearField.getText()));
        b.setEdition(editionField.getText().trim());
        b.setCategory(categoryCombo.getValue());
        int qty = parseIntSafe(quantityField.getText());
        b.setQuantity(qty);
        if (editingBook == null) b.setAvailableQty(qty);
        b.setShelfLocation(shelfField.getText().trim());
        b.setDescription(descriptionArea.getText().trim());
        b.setCoverImage(coverBytes);

        boolean ok = editingBook != null
                ? bookService.updateBook(b)
                : bookService.addBook(b);

        if (ok) {
            searchService.addTitle(b.getBookName());
            searchService.addAuthor(b.getAuthor());
            DashboardCache.getInstance().invalidate();
            ToastNotification.success(backBtn.getScene(),
                    (editingBook != null ? "Book updated: " : "Book added: ") + b.getBookName());
            clearForm();
            loadPage();
        } else {
            showError("Failed to save book. Check for duplicate ISBN.");
        }
    }

    private void editBook(Book b) { populateForm(b); }

    private void toggleArchiveBook(Book b) {
        boolean isArchived = Book.STATUS_ARCHIVED.equals(b.getStatus());
        String action = isArchived ? "Restore" : "Archive";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(action + " Book");
        confirm.setHeaderText(action + ": " + b.getBookName() + "?");
        confirm.setContentText(isArchived
            ? "This will restore the book to Available status."
            : "This will move the book to Archived Books. It will not appear in normal operations.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                boolean ok = isArchived
                    ? bookService.unarchiveBook(b.getBookId())
                    : bookService.archiveBook(b.getBookId());
                if (ok) {
                    DashboardCache.getInstance().invalidate();
                    ToastNotification.success(backBtn.getScene(),
                        "Book " + (isArchived ? "restored" : "archived") + ": " + b.getBookName());
                    loadPage();
                } else {
                    ToastNotification.error(backBtn.getScene(), action + " failed.");
                }
            }
        });
    }

    private void deleteBook(Book b) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Book");
        confirm.setHeaderText("Permanently delete: " + b.getBookName() + "?");
        confirm.setContentText("Consider archiving instead. Delete cannot be undone.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                if (bookService.deleteBook(b.getBookId())) {
                    DashboardCache.getInstance().invalidate();
                    ToastNotification.success(backBtn.getScene(), "Book deleted.");
                    loadPage();
                } else {
                    ToastNotification.error(backBtn.getScene(),
                            "Cannot delete — book has active transactions. Archive instead.");
                }
            }
        });
    }

    @FXML
    private void clearForm() {
        editingBook = null;
        coverBytes  = null;
        formTitle.setText("Add New Book");
        saveBtn.setText("💾 Save Book");
        isbnField.clear(); titleField.clear(); authorField.clear();
        publisherField.clear(); yearField.clear(); editionField.clear();
        categoryCombo.setValue(null); quantityField.setText("1");
        shelfField.clear(); descriptionArea.clear();
        coverImageView.setImage(null);
        hideError();
    }

    @FXML
    private void uploadCover() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Cover Image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File f = fc.showOpenDialog(backBtn.getScene().getWindow());
        if (f == null) return;
        try {
            coverBytes = Files.readAllBytes(f.toPath());
            coverImageView.setImage(new Image(new ByteArrayInputStream(coverBytes)));
        } catch (IOException e) {
            ToastNotification.error(backBtn.getScene(), "Failed to load image.");
        }
    }

    // ── Import / Export ───────────────────────────────────────────────────────

    @FXML
    private void importCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Import Books from CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File f = fc.showOpenDialog(backBtn.getScene().getWindow());
        if (f == null) return;

        int imported = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; } // skip header
                String[] parts = line.split(",", -1);
                if (parts.length < 4) continue;
                Book b = new Book();
                b.setIsbn(parts[0].trim());
                b.setBookName(parts[1].trim());
                b.setAuthor(parts[2].trim());
                b.setCategory(parts[3].trim());
                b.setQuantity(parts.length > 4 ? parseIntSafe(parts[4].trim()) : 1);
                b.setAvailableQty(b.getQuantity());
                if (bookService.addBook(b)) imported++;
            }
        } catch (IOException e) {
            ToastNotification.error(backBtn.getScene(), "Import error: " + e.getMessage());
            return;
        }
        searchService.reload();
        DashboardCache.getInstance().invalidate();
        ToastNotification.success(backBtn.getScene(), "Imported " + imported + " books.");
        loadPage();
    }

    @FXML
    private void exportExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Books to CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fc.setInitialFileName("books_export.csv");
        File f = fc.showSaveDialog(backBtn.getScene().getWindow());
        if (f == null) return;

        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            pw.println("ID,ISBN,Title,Author,Publisher,Year,Category,Quantity,Available,Status,Shelf");
            for (Book b : bookService.getAllBooks(1, Integer.MAX_VALUE)) {
                pw.printf("%d,%s,%s,%s,%s,%d,%s,%d,%d,%s,%s%n",
                        b.getBookId(), nvl(b.getIsbn()), esc(b.getBookName()),
                        esc(b.getAuthor()), nvl(b.getPublisher()),
                        b.getPublicationYear(), nvl(b.getCategory()),
                        b.getQuantity(), b.getAvailableQty(),
                        nvl(b.getStatus()), nvl(b.getShelfLocation()));
            }
            ToastNotification.success(backBtn.getScene(), "Exported to: " + f.getName());
        } catch (IOException e) {
            ToastNotification.error(backBtn.getScene(), "Export error: " + e.getMessage());
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private boolean validateForm() {
        boolean ok = true;
        ok &= com.library.shared.ValidationUtil.requireNonBlank(titleField);
        ok &= com.library.shared.ValidationUtil.requireNonBlank(authorField);
        ok &= com.library.shared.ValidationUtil.requirePositiveInt(quantityField);
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
        // Fallback: load dashboard fresh
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
