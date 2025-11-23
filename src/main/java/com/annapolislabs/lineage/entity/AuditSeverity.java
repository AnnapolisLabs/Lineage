package com.annapolislabs.lineage.entity;

/**
 * Audit log severity levels
 */
public enum AuditSeverity {
    /**
     * Informational events
     */
    INFO,
    
    /**
     * Warning events that may require attention
     */
    WARNING,
    
    /**
     * Error events that may affect functionality
     */
    ERROR,
    
    /**
     * Critical security events requiring immediate attention
     */
    CRITICAL
}