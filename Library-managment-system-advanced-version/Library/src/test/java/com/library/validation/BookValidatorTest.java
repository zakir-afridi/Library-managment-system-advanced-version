package com.library.validation;

import com.library.exception.ValidationException;
import com.library.model.Book;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BookValidatorTest {

    @Test
    void testValidBookPasses() {
        Book book = new Book();
        book.setBookName("Design Patterns");
        book.setAuthor("Gang of Four");
        book.setIsbn("9780201633610");
        book.setQuantity(5);
        book.setAvailableQty(5);

        assertDoesNotThrow(() -> BookValidator.validate(book));
    }

    @Test
    void testNullBookThrowsException() {
        assertThrows(ValidationException.class, () -> BookValidator.validate(null));
    }

    @Test
    void testEmptyTitleThrowsException() {
        Book book = new Book();
        book.setBookName("");
        book.setAuthor("Robert Martin");
        book.setQuantity(2);
        book.setAvailableQty(2);

        assertThrows(ValidationException.class, () -> BookValidator.validate(book));
    }

    @Test
    void testInvalidQuantityThrowsException() {
        Book book = new Book();
        book.setBookName("Clean Code");
        book.setAuthor("Robert Martin");
        book.setQuantity(0);
        book.setAvailableQty(0);

        assertThrows(ValidationException.class, () -> BookValidator.validate(book));
    }

    @Test
    void testAvailableQtyExceedingTotalThrowsException() {
        Book book = new Book();
        book.setBookName("Clean Architecture");
        book.setAuthor("Robert Martin");
        book.setQuantity(2);
        book.setAvailableQty(5);

        assertThrows(ValidationException.class, () -> BookValidator.validate(book));
    }
}
