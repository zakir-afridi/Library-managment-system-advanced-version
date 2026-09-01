package com.library.exception;

/**
 * Exception thrown when domain or input validation fails.
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
