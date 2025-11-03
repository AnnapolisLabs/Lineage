package com.annapolislabs.lineage.exception;

/**
 * Exception thrown when search operations fail
 */
public class SearchException extends RuntimeException {

    public SearchException(String message) {
        super(message);
    }

    public SearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
