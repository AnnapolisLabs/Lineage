package com.annapolislabs.lineage.exception.auth;

/**
 * Exception for MFA-related authentication failures
 */
public class MfaVerificationException extends AuthenticationException {
    
    public MfaVerificationException(String message) {
        super("MFA_VERIFICATION_FAILED", message);
    }
    
    public MfaVerificationException(String message, Throwable cause) {
        super("MFA_VERIFICATION_FAILED", message, cause);
    }
}