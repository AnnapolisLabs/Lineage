package com.annapolislabs.lineage.entity;

/**
 * User account status enumeration
 */
public enum UserStatus {
    /**
     * Active and can login
     */
    ACTIVE,
    
    /**
     * Temporarily suspended
     */
    SUSPENDED,
    
    /**
     * Deactivated by user or admin
     */
    DEACTIVATED,
    
    /**
     * Pending email verification
     */
    PENDING_VERIFICATION
}