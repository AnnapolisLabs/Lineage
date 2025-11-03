package com.annapolislabs.lineage.exception;

/**
 * Exception thrown when LLM API operations fail
 */
public class LLMApiException extends RuntimeException {
    public LLMApiException(String message) {
        super(message);
    }

    public LLMApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
