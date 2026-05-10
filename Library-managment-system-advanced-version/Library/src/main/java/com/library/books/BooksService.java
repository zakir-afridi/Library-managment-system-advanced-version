package com.library.books;

import com.library.model.Book;
import com.library.service.BookService;

import java.util.List;

/**
 * BOOKS BRANCH — service layer.
 * Delegates to com.library.service.BookService.
 */
public class BooksService {

    private final BookService delegate = new BookService();

    public boolean add(Book book)                              { return delegate.addBook(book); }
    public boolean update(Book book)                           { return delegate.updateBook(book); }
    public boolean delete(int bookId)                          { return delegate.deleteBook(bookId); }
    public Book getById(int id)                                { return delegate.getBookById(id); }
    public Book getByCode(String code)                         { return delegate.getBookByCode(code); }
    public Book getByIsbn(String isbn)                         { return delegate.getBookByIsbn(isbn); }
    public List<Book> getPage(int page, int size)              { return delegate.getAllBooks(page, size); }
    public List<Book> search(String query)                     { return delegate.searchBooks(query); }
    public List<Book> getArchived()                            { return delegate.getArchivedBooks(); }
    public boolean archive(int bookId)                         { return delegate.archiveBook(bookId); }
    public boolean unarchive(int bookId)                       { return delegate.unarchiveBook(bookId); }
    public List<String> getCategories()                        { return delegate.getAllCategories(); }
    public int getTotalCount()                                 { return delegate.getTotalBooks(); }
}
