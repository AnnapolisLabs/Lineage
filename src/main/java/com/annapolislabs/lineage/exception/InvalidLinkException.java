package com.annapolislabs.lineage.exception;

/**
 * Exception thrown when a requirement link operation is invalid
 */
public class InvalidLinkException extends RuntimeException {

    public InvalidLinkException(String message) {
        super(message);
    }
}
