package com.library.exception;

/**
 * Exception thrown when authentication or login validation fails.
 */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}
