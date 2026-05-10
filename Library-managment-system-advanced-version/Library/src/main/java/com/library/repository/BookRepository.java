package com.library.repository;

import com.library.model.Book;
import com.library.service.BookService;

import java.util.List;

/**
 * Legacy repository — delegates to BookService.
 * Kept for backward compatibility with any old code that imports this class.
 */
public class BookRepository {

    private final BookService service = new BookService();

    public List<Book> getAllBooks()          { return service.getAllBooks(1, 10000); }
    public boolean    addBook(Book b)        { return service.addBook(b); }
    public boolean    updateBook(Book b)     { return service.updateBook(b); }
    public boolean    deleteBook(int id)     { return service.deleteBook(id); }
    public Book       getBookById(int id)    { return service.getBookById(id); }
    public int        getTotalBooks()        { return service.getTotalBooks(); }
}
