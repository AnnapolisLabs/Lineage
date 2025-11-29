package com.annapolislabs.lineage.entity;

/**
 * RBAC Role Types for the Lineage system
 * Clean hierarchical role system with clear permission levels
 */
public enum UserRole {
    // Standard user with basic permissions
    USER("USER", 1, false, true),           // Standard user level 1
    
    // Enhanced role system with hierarchy levels
    PROJECT_MANAGER("PROJECT_MANAGER", 2, true, true),
    DEVELOPER("DEVELOPER", 1, false, false),
    
    // Administrative roles
    OWNER("OWNER", 3, true, true),           // Super-user level 3
    ADMINISTRATOR("ADMINISTRATOR", 2, true, true);  // Admin level 2
    
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
}
