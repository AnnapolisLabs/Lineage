package com.annapolislabs.lineage.entity;

/**
 * User invitation status enumeration
 */
public enum InvitationStatus {
    /**
     * Invitation sent and waiting for acceptance
     */
    PENDING,
    
    /**
     * Invitation accepted by the user
     */
    ACCEPTED,
    
    /**
     * Invitation has expired
     */
    EXPIRED,
    
    /**
     * Invitation was cancelled by the inviter
     */
    CANCELLED
}