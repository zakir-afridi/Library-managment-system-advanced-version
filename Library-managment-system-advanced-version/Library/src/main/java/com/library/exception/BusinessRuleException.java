package com.library.exception;

/**
 * Exception thrown when a business rule (e.g. loan limits, duplicate active loan, unavailable copy) is violated.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
