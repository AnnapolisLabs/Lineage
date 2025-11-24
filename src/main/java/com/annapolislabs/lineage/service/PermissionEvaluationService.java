package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.entity.*;
import com.annapolislabs.lineage.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Core service for permission evaluation in the RBAC system
 * Provides high-performance permission checking with caching
 */
@Slf4j
@Service
public class PermissionEvaluationService {

    @Autowired
    private PermissionDefinitionRepository permissionDefinitionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PermissionChangeRepository permissionChangeRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private TeamRepository teamRepository;

    // In-memory cache for frequently evaluated permissions
    private final Map<String, PermissionCacheEntry> permissionCache = new ConcurrentHashMap<>();
    
    // Cache expiration time in seconds
    private static final long CACHE_EXPIRY_SECONDS = 300; // 5 minutes
    private static final long CACHE_MAX_SIZE = 10000;

    /**
     * Check if user has specific permission
     * Uses multi-level caching for performance (< 10ms target)
     */
    @Cacheable(value = "userPermissions", key = "#userId + '_' + #permissionKey")
    public boolean hasPermission(UUID userId, String permissionKey) {
        return hasPermission(userId, permissionKey, null);
    }

    /**
     * Check if user has specific permission for a resource
     */
    @Cacheable(value = "userPermissions", key = "#userId + '_' + #permissionKey + '_' + (#resourceId != null ? #resourceId.toString() : 'null')")
    public boolean hasPermission(UUID userId, String permissionKey, UUID resourceId) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. Check cache first
            String cacheKey = generateCacheKey(userId, permissionKey, resourceId);
            PermissionCacheEntry cached = getFromCache(cacheKey);
//            if (cached != null && !cached.isExpired()) {
//                log.debug("Permission check cache hit for user {}: {}", userId, permissionKey);
//                return cached.isAllowed();
//            }

            // 2. Validate inputs
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            
            if (user.getStatus() != UserStatus.ACTIVE) {
                log.warn("Permission check for inactive user: {}", userId);
                return false;
            }

            // 3. Check role hierarchy permissions
            boolean hasPermission = checkRoleBasedPermissions(user, permissionKey, resourceId);
            
            // 4. Check explicit permission grants/revokes
            if (!hasPermission) {
                hasPermission = checkExplicitPermissions(userId, permissionKey, resourceId);
            }
            
            // 5. Check team-based permissions if resource is project-scoped
            if (!hasPermission && resourceId != null) {
                hasPermission = checkTeamPermissions(userId, permissionKey, resourceId);
            }

            // 6. Cache result
            putInCache(cacheKey, new PermissionCacheEntry(hasPermission, LocalDateTime.now()));
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Permission evaluation completed in {}ms for user {}: {}", duration, userId, permissionKey);
            
            // Target < 10ms for simple checks, < 50ms for complex checks
            if (duration > 50) {
                log.warn("Slow permission evaluation: {}ms for user {}: {}", duration, userId, permissionKey);
            }
            
            return hasPermission;

        } catch (Exception e) {
            log.error("Error evaluating permission for user {}: {}", userId, permissionKey, e);
            return false; // Fail secure
        }
    }

    /**
     * Get all effective permissions for a user
     */
    public Set<String> getEffectivePermissions(UUID userId) {
        return getEffectivePermissions(userId, null);
    }

    /**
     * Get all effective permissions for a user, optionally scoped to a resource
     */
    @Cacheable(value = "userEffectivePermissions", key = "#userId + '_' + (#resourceId != null ? #resourceId.toString() : 'global')")
    public Set<String> getEffectivePermissions(UUID userId, UUID resourceId) {
        Set<String> permissions = new HashSet<>();
        
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            
            // Add role-based permissions
            permissions.addAll(getRoleBasedPermissions(user.getGlobalRole()));
            
            // Add explicit permissions
            List<PermissionChange> activeChanges = permissionChangeRepository
                    .findActiveChanges(LocalDateTime.now())
                    .stream()
                    .filter(change -> change.getUserId().equals(userId))
                    .filter(change -> resourceId == null || Objects.equals(change.getResourceId(), resourceId))
                    .collect(Collectors.toList());
            
            for (PermissionChange change : activeChanges) {
                if (change.getChangeType() == PermissionChange.ChangeType.GRANT ||
                    change.getChangeType() == PermissionChange.ChangeType.MODIFY) {
                    permissions.add(change.getPermissionKey());
                } else if (change.getChangeType() == PermissionChange.ChangeType.REVOKE) {
                    permissions.remove(change.getPermissionKey());
                }
            }
            
            // Add team-based permissions if scoped to resource
            if (resourceId != null) {
                List<TeamMember> teamMemberships = teamMemberRepository
                        .findByUserIdAndStatus(userId, TeamMember.TeamMemberStatus.ACTIVE);
                for (TeamMember membership : teamMemberships) {
                    Team team = teamRepository.findById(membership.getTeamId()).orElse(null);
                    if (team != null && team.getProjectId() != null && 
                        team.getProjectId().equals(resourceId) && team.isActive()) {
                        // Add team-specific permissions based on role
                        permissions.addAll(getTeamBasedPermissions(membership.getRole()));
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error getting effective permissions for user {}", userId, e);
        }
        
        return permissions;
    }

    /**
     * Check role hierarchy - higher roles inherit permissions from lower roles
     */
    public boolean hasRoleOrHigher(UUID userId, UserRole requiredRole) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getGlobalRole() == null) {
            return false;
        }
        
        return user.getGlobalRole().hasRoleLevelOrHigher(requiredRole);
    }

    /**
     * Batch permission evaluation for multiple permissions
     */
    public Map<String, Boolean> evaluatePermissions(UUID userId, List<String> permissionKeys, UUID resourceId) {
        Map<String, Boolean> results = new HashMap<>();
        
        for (String permissionKey : permissionKeys) {
            results.put(permissionKey, hasPermission(userId, permissionKey, resourceId));
        }
        
        return results;
    }

    /**
     * Clear permission cache for a user
     */
    public void clearUserCache(UUID userId) {
        String pattern = userId.toString() + "_";
        permissionCache.keySet().removeIf(key -> key.startsWith(pattern));
        log.debug("Cleared permission cache for user {}", userId);
    }

    /**
     * Clear all permission caches
     */
    public void clearAllCaches() {
        permissionCache.clear();
        log.info("Cleared all permission caches");
    }

    // Private helper methods

    private boolean checkRoleBasedPermissions(User user, String permissionKey, UUID resourceId) {
        UserRole userRole = user.getGlobalRole();
        
        // Parse permission key to get resource and action
        String[] parts = permissionKey.split("\\.", 2);
        if (parts.length != 2) {
            return false;
        }
        
        String resource = parts[0];
        String action = parts[1];
        
        // Check against role hierarchy
        return switch (userRole) {
            case OWNER -> true; // Owner has all permissions
            case ADMINISTRATOR -> checkAdministratorPermissions(resource, action);
            case USER -> checkUserPermissions(resource, action);
            case PROJECT_MANAGER -> checkProjectManagerPermissions(resource, action);
            case DEVELOPER -> checkDeveloperPermissions(resource, action);
            default -> false;
        };
    }

    private boolean checkAdministratorPermissions(String resource, String action) {
        // Administrator can manage users, projects, teams, and requirements
        return (resource.equals("user") && Arrays.asList("read", "update", "manage").contains(action)) ||
               (resource.equals("project") && !action.equals("delete") && !action.equals("transfer_ownership")) ||
               (resource.equals("team") && !action.equals("delete")) ||
               (resource.equals("requirement") && !action.equals("delete")) ||
               (resource.equals("task") && !action.equals("delete")) ||
               (resource.equals("review") && action.equals("conduct"));
    }

    private boolean checkUserPermissions(String resource, String action) {
        // Standard user can read and create, update their own data
        return (resource.equals("project") && action.equals("read")) ||
               (resource.equals("requirement") && Arrays.asList("create", "read", "update").contains(action)) ||
               (resource.equals("task") && Arrays.asList("complete", "update").contains(action)) ||
               (resource.equals("team") && action.equals("participate")) ||
               (resource.equals("review") && action.equals("conduct"));
    }

    private boolean checkProjectManagerPermissions(String resource, String action) {
        // Project Manager has administrative rights for projects
        return checkAdministratorPermissions(resource, action) ||
               (resource.equals("project") && action.equals("update"));
    }

    private boolean checkDeveloperPermissions(String resource, String action) {
        // Developer has user permissions plus development-specific rights
        return checkUserPermissions(resource, action) ||
               (resource.equals("project") && action.equals("read"));
    }

    private boolean checkExplicitPermissions(UUID userId, String permissionKey, UUID resourceId) {
        List<PermissionChange> activeChanges = permissionChangeRepository
                .findActiveChanges(LocalDateTime.now())
                .stream()
                .filter(change -> change.getUserId().equals(userId))
                .filter(change -> change.getPermissionKey().equals(permissionKey))
                .filter(change -> resourceId == null || Objects.equals(change.getResourceId(), resourceId))
                .collect(Collectors.toList());

        // If multiple changes exist, the most recent one takes precedence
        if (!activeChanges.isEmpty()) {
            PermissionChange latestChange = activeChanges.stream()
                    .max(Comparator.comparing(PermissionChange::getEffectiveFrom))
                    .orElse(null);
            
            if (latestChange != null) {
                return latestChange.getChangeType() == PermissionChange.ChangeType.GRANT ||
                       latestChange.getChangeType() == PermissionChange.ChangeType.MODIFY;
            }
        }
        
        return false;
    }

    private boolean checkTeamPermissions(UUID userId, String permissionKey, UUID resourceId) {
        // Check if user is member of any team associated with the resource
        List<TeamMember> teamMemberships = teamMemberRepository
                .findByUserIdAndStatus(userId, TeamMember.TeamMemberStatus.ACTIVE);
        
        for (TeamMember membership : teamMemberships) {
            Team team = teamRepository.findById(membership.getTeamId()).orElse(null);
            if (team != null && team.getProjectId() != null && 
                team.getProjectId().equals(resourceId) && team.isActive()) {
                
                // Check team role permissions
                if (membership.canEdit() || membership.canManage()) {
                    return true;
                }
            }
        }
        
        return false;
    }

    private Set<String> getRoleBasedPermissions(UserRole role) {
        Set<String> permissions = new HashSet<>();
        
        switch (role) {
            case OWNER:
                permissions.addAll(Arrays.asList(
                        "user.*", "project.*", "team.*", "task.*", "review.*", "requirement.*", "role.*"
                ));
                break;
            case ADMINISTRATOR:
                permissions.addAll(Arrays.asList(
                        "user.read", "user.update", "user.manage",
                        "project.read", "project.update", "project.create",
                        "team.read", "team.manage", "team.invite",
                        "task.read", "task.assign", "task.manage",
                        "requirement.read", "requirement.update", "requirement.create",
                        "review.read", "review.conduct"
                ));
                break;
            case USER:
            case DEVELOPER:
                permissions.addAll(Arrays.asList(
                        "project.read",
                        "requirement.read", "requirement.create", "requirement.update",
                        "task.read", "task.complete", "task.update",
                        "team.participate",
                        "review.read", "review.conduct"
                ));
                break;
            default:
                break;
        }
        
        return permissions;
    }

    private Set<String> getTeamBasedPermissions(TeamMember.TeamRole teamRole) {
        Set<String> permissions = new HashSet<>();
        
        switch (teamRole) {
            case OWNER:
            case ADMIN:
                permissions.addAll(Arrays.asList(
                        "team.manage", "team.invite", "team.remove",
                        "task.assign", "task.manage"
                ));
                break;
            case MEMBER:
                permissions.addAll(Arrays.asList(
                        "team.participate",
                        "task.complete", "task.update"
                ));
                break;
            case VIEWER:
                permissions.add("team.participate");
                break;
        }
        
        return permissions;
    }

    private String generateCacheKey(UUID userId, String permissionKey, UUID resourceId) {
        return String.format("%s_%s_%s", userId, permissionKey, 
                resourceId != null ? resourceId.toString() : "null");
    }

    private PermissionCacheEntry getFromCache(String key) {
        return permissionCache.get(key);
    }

    private void putInCache(String key, PermissionCacheEntry entry) {
        // Clean expired entries
        cleanExpiredCache();
        
        // Enforce cache size limit
        if (permissionCache.size() >= CACHE_MAX_SIZE) {
            cleanOldestEntries();
        }
        
        permissionCache.put(key, entry);
    }

    private void cleanExpiredCache() {
        LocalDateTime expiryTime = LocalDateTime.now().minusSeconds(CACHE_EXPIRY_SECONDS);
        permissionCache.entrySet().removeIf(entry -> entry.getValue().isExpired(expiryTime));
    }

    private void cleanOldestEntries() {
        // Remove oldest 10% of entries
        int entriesToRemove = (int) (permissionCache.size() * 0.1);
        permissionCache.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.comparing(PermissionCacheEntry::getCreatedAt)))
                .limit(entriesToRemove)
                .forEach(entry -> permissionCache.remove(entry.getKey()));
    }

    // Inner class for cache entries
    private static class PermissionCacheEntry {
        private final boolean allowed;
        private final LocalDateTime createdAt;

        public PermissionCacheEntry(boolean allowed, LocalDateTime createdAt) {
            this.allowed = allowed;
            this.createdAt = createdAt;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(createdAt.plusSeconds(CACHE_EXPIRY_SECONDS));
        }

        public boolean isExpired(LocalDateTime expiryTime) {
            return createdAt.isBefore(expiryTime);
        }
    }
}