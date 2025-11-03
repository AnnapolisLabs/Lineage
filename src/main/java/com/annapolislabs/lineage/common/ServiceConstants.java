package com.annapolislabs.lineage.common;

/**
 * Centralized constants for service-level messages and identifiers
 */
public final class ServiceConstants {

    private ServiceConstants() {
        // Utility class - prevent instantiation
    }

    // Error Messages - Resources Not Found
    public static final String PROJECT_NOT_FOUND = "Project not found";
    public static final String REQUIREMENT_NOT_FOUND = "Requirement not found";
    public static final String LINK_NOT_FOUND = "Link not found";

    // Error Messages - Access Control
    public static final String ACCESS_DENIED = "Access denied";

    // Field Names
    public static final String REQUIREMENT_ID = "requirementId";
    public static final String PROJECT_ID = "projectId";
    public static final String DESCRIPTION = "description";
    public static final String TITLE = "title";
    public static final String STATUS = "status";
    public static final String PRIORITY = "priority";
}
