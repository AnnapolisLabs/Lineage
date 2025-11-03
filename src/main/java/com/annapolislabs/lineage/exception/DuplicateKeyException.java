package com.annapolislabs.lineage.exception;

/**
 * Exception thrown when a duplicate key constraint is violated
 */
public class DuplicateKeyException extends RuntimeException {
    public DuplicateKeyException(String message) {
        super(message);
    }

    public DuplicateKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
