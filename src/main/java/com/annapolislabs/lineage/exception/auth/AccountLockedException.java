package com.annapolislabs.lineage.exception.auth;

/**
 * Exception for account lockout
 */
public class AccountLockedException extends AuthenticationException {
    
    private final long lockoutDurationMinutes;
    
    public AccountLockedException(String message, long lockoutDurationMinutes) {
        super("ACCOUNT_LOCKED", message);
        this.lockoutDurationMinutes = lockoutDurationMinutes;
    }
    
    public AccountLockedException(String message, long lockoutDurationMinutes, Throwable cause) {
        super("ACCOUNT_LOCKED", message, cause);
        this.lockoutDurationMinutes = lockoutDurationMinutes;
    }
    
    public long getLockoutDurationMinutes() {
        return lockoutDurationMinutes;
    }
}