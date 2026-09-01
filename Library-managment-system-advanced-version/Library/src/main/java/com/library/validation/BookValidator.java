package com.library.validation;

import com.library.exception.ValidationException;
import com.library.model.Book;

/**
 * Validates bibliographic Book entities and inventory constraints.
 */
public class BookValidator {

    public static void validate(Book book) {
        if (book == null) {
            throw new ValidationException("Book entity cannot be null.");
        }
        if (book.getBookName() == null || book.getBookName().trim().isEmpty()) {
            throw new ValidationException("Book title is required.");
        }
        if (book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
            throw new ValidationException("Book author is required.");
        }
        if (book.getQuantity() < 1) {
            throw new ValidationException("Total quantity must be at least 1.");
        }
        if (book.getAvailableQty() < 0) {
            throw new ValidationException("Available quantity cannot be negative.");
        }
        if (book.getAvailableQty() > book.getQuantity()) {
            throw new ValidationException("Available quantity cannot exceed total quantity.");
        }
        if (book.getIsbn() != null && !book.getIsbn().trim().isEmpty()) {
            String cleanIsbn = book.getIsbn().replaceAll("[\\s-]", "");
            if (cleanIsbn.length() != 10 && cleanIsbn.length() != 13 && cleanIsbn.length() < 5) {
                throw new ValidationException("Invalid ISBN format.");
            }
        }
    }
}
