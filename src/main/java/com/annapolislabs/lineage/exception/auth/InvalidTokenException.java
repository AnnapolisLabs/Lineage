package com.annapolislabs.lineage.exception.auth;

/**
 * Exception for invalid tokens
 */
public class InvalidTokenException extends AuthenticationException {
    
    public InvalidTokenException(String message) {
        super("INVALID_TOKEN", message);
    }
    
    public InvalidTokenException(String message, Throwable cause) {
        super("INVALID_TOKEN", message, cause);
    }
}