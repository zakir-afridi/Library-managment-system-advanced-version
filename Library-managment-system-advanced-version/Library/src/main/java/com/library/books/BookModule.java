package com.library.books;

import com.library.model.Book;
import com.library.service.BookService;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

import java.util.List;

/**
 * BOOKS BRANCH — public API.
 * Other branches call these static methods; never import BookController directly.
 */
public class BookModule {

    private static final BookService service = new BookService();
    private static com.library.controller.BookController controller;

    // ── UI Navigation ─────────────────────────────────────────────────────────

    /** Load the book list view into the given container pane. */
    public static void showBookList(Pane container) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    BookModule.class.getResource("/com/library/ui/AddBookForm.fxml"));
            Pane view = loader.load();
            controller = loader.getController();
            container.getChildren().setAll(view);
        } catch (Exception e) {
            System.err.println("BookModule.showBookList: " + e.getMessage());
        }
    }

    /** Open the add-book dialog. */
    public static void showAddBook() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    BookModule.class.getResource("/com/library/ui/AddBookForm.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.setTitle("Add Book");
            dialog.setScene(new javafx.scene.Scene(root));
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.showAndWait();
        } catch (Exception e) {
            System.err.println("BookModule.showAddBook: " + e.getMessage());
        }
    }

    /** Refresh the book table after an external change. */
    public static void refreshBookData() {
        if (controller != null) controller.loadBooks();
    }

    // ── Data API ──────────────────────────────────────────────────────────────

    public static List<Book> searchBooks(String query)     { return service.searchBooks(query); }
    public static Book getById(int bookId)                 { return service.getBookById(bookId); }
    public static Book getByCode(String code)              { return service.getBookByCode(code); }
    public static int getTotalBookCount()                  { return service.getTotalBooks(); }
    public static int getAvailableBookCount() {
        return service.getAllBooks(1, Integer.MAX_VALUE)
                .stream().mapToInt(Book::getAvailableQty).sum();
    }
    public static boolean decrementAvailability(int id)    { return service.decrementAvailability(id); }
    public static boolean incrementAvailability(int id)    { return service.incrementAvailability(id); }
    public static List<Book> getPage(int page, int size)   { return service.getAllBooks(page, size); }
    public static List<String> getCategories()             { return service.getAllCategories(); }
}
