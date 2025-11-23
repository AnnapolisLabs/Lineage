package com.annapolislabs.lineage.entity;

/**
 * Enhanced Role Types for the RBAC system
 * Includes legacy support while adding new hierarchical roles
 */
public enum UserRole {
    // Legacy values for backward compatibility
    VIEWER("VIEWER", 1, false, false),
    EDITOR("EDITOR", 1, false, false), 
    ADMIN("ADMIN", 3, true, true),
    
    // New enhanced role system with hierarchy levels
    PROJECT_MANAGER("PROJECT_MANAGER", 2, true, true),
    DEVELOPER("DEVELOPER", 1, false, false),
    
    // New RBAC hierarchical roles
    OWNER("OWNER", 3, true, true),           // Super-user level 3
    ADMINISTRATOR("ADMINISTRATOR", 2, true, true),  // Admin level 2  
    USER("USER", 1, false, true);           // Standard user level 1
    
    private final String name;
    private final int hierarchyLevel;
    private final boolean isSystem;
    private final boolean isActive;
    
    UserRole(String name, int hierarchyLevel, boolean isSystem, boolean isActive) {
        this.name = name;
        this.hierarchyLevel = hierarchyLevel;
        this.isSystem = isSystem;
        this.isActive = isActive;
    }
    
    public String getName() {
        return name;
    }
    
    public int getHierarchyLevel() {
        return hierarchyLevel;
    }
    
    public boolean isSystemRole() {
        return isSystem;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * Check if this role has at least the same hierarchy level as another role
     */
    public boolean hasRoleLevelOrHigher(UserRole other) {
        return this.hierarchyLevel >= other.hierarchyLevel;
    }
    
    /**
     * Check if this role is considered administrative
     */
    public boolean isAdministrative() {
        return hierarchyLevel >= 2;
    }
    
    /**
     * Map legacy roles to new RBAC roles for backward compatibility
     */
    public static UserRole mapLegacyRole(UserRole legacyRole) {
        switch (legacyRole) {
            case ADMIN:
                return OWNER;  // Legacy admin becomes owner
            case PROJECT_MANAGER:
                return ADMINISTRATOR;  // Project manager becomes administrator
            case EDITOR:
            case DEVELOPER:
                return USER;  // Both become standard users
            case VIEWER:
                return USER;  // Viewer becomes standard user
            default:
                return USER;  // Default to user
        }
    }
}
