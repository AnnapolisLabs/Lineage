package com.annapolislabs.lineage.exception.auth;

/**
 * Exception thrown when user attempts to login without verifying their email
 */
public class EmailNotVerifiedException extends RuntimeException {
    
    public EmailNotVerifiedException(String message) {
        super(message);
    }
    
    public EmailNotVerifiedException(String message, Throwable cause) {
        super(message, cause);
    }
}