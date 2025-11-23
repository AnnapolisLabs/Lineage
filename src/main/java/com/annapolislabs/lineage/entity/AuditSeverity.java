package com.annapolislabs.lineage.entity;

/**
 * Audit log severity levels
 */
public enum AuditSeverity {
    /**
     * Informational events
     */
    INFO,
    LOW,
    
    /**
     * Warning events that may require attention
     */
    WARNING,
    MEDIUM,
    
    /**
     * Error events that may affect functionality
     */
    ERROR,
    HIGH,
    
    /**
     * Critical security events requiring immediate attention
     */
    CRITICAL

}