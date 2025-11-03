package com.annapolislabs.lineage.exception;

/**
 * Exception thrown when access to a resource is denied
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException() {
        super("Access denied");
    }
}
