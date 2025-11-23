package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.entity.*;
import com.annapolislabs.lineage.repository.*;
import com.annapolislabs.lineage.security.SecurityAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for role management and permission operations
 * Handles role creation, updates, permission grants/revokes, and role hierarchy management
 */
@Slf4j
@Service
public class RoleManagementService {

    @Autowired
    private PermissionDefinitionRepository permissionDefinitionRepository;

    @Autowired
    private PermissionChangeRepository permissionChangeRepository;

    @Autowired
    private PermissionEvaluationService permissionEvaluationService;

    @Autowired
    private SecurityAuditService securityAuditService;

    /**
     * Get all roles with permissions
     */
    public Page<PermissionDefinition> getAllRoles(Pageable pageable, UUID requestingUserId) {
        log.debug("Getting all roles by user {}", requestingUserId);

        // Check if user has permission to view roles
        if (!permissionEvaluationService.hasPermission(requestingUserId, "role.read", null)) {
            throw new SecurityException("User does not have permission to view roles");
        }

        return permissionDefinitionRepository.findAll(pageable);
    }

    /**
     * Get specific role by ID
     */
    public PermissionDefinition getRoleById(UUID roleId, UUID requestingUserId) {
        log.debug("Getting role {} by user {}", roleId, requestingUserId);

        // Check if user has permission to view roles
        if (!permissionEvaluationService.hasPermission(requestingUserId, "role.read", null)) {
            throw new SecurityException("User does not have permission to view roles");
        }

        return permissionDefinitionRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
    }

    /**
     * Create a new role
     */
    @Transactional
    public PermissionDefinition createRole(String permissionKey, String resource, String action, 
                                         String description, String category, String riskLevel,
                                         UUID requestingUserId) {
        log.info("Creating role '{}' by user {}", permissionKey, requestingUserId);

        // Validate inputs
        if (!StringUtils.hasText(permissionKey)) {
            throw new IllegalArgumentException("Permission key is required");
        }
        if (!StringUtils.hasText(resource)) {
            throw new IllegalArgumentException("Resource is required");
        }
        if (!StringUtils.hasText(action)) {
            throw new IllegalArgumentException("Action is required");
        }

        // Check if user has permission to create roles
        if (!permissionEvaluationService.hasPermission(requestingUserId, "role.create", null)) {
            throw new SecurityException("User does not have permission to create roles");
        }

        // Check if role already exists
        if (permissionDefinitionRepository.existsByPermissionKey(permissionKey)) {
            throw new IllegalArgumentException("Role with permission key already exists: " + permissionKey);
        }

        // Create role
        PermissionDefinition role = new PermissionDefinition();
        role.setPermissionKey(permissionKey);
        role.setResource(resource);
        role.setAction(action);
        role.setDescription(description);
        role.setCategory(category);
        role.setSystem(false);
        
        try {
            role.setRiskLevel(PermissionDefinition.RiskLevel.valueOf(riskLevel.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid risk level: " + riskLevel);
        }

        // Save role
        role = permissionDefinitionRepository.save(role);

        // Audit log
        securityAuditService.logEvent("ROLE_CREATED", requestingUserId, "ROLE", role.getId(),
                Map.of("permission_key", permissionKey, "resource", resource, "action", action, 
                       "category", category, "risk_level", riskLevel));

        log.info("Role '{}' created successfully with ID {}", permissionKey, role.getId());
        return role;
    }

    /**
     * Update existing role
     */
    @Transactional
    public PermissionDefinition updateRole(UUID roleId, String description, String category, 
                                         String riskLevel, UUID requestingUserId) {
        log.info("Updating role {} by user {}", roleId, requestingUserId);

        // Check if user has permission to update roles
        if (!permissionEvaluationService.hasPermission(requestingUserId, "role.update", null)) {
            throw new SecurityException("User does not have permission to update roles");
        }

        PermissionDefinition role = permissionDefinitionRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        // Cannot update system roles
        if (role.isSystem()) {
            throw new IllegalArgumentException("Cannot update system roles");
        }

        // Update fields
        if (StringUtils.hasText(description)) {
            role.setDescription(description);
        }
        if (StringUtils.hasText(category)) {
            role.setCategory(category);
        }
        if (StringUtils.hasText(riskLevel)) {
            try {
                role.setRiskLevel(PermissionDefinition.RiskLevel.valueOf(riskLevel.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid risk level: " + riskLevel);
            }
        }

        role = permissionDefinitionRepository.save(role);

        // Audit log
        securityAuditService.logEvent("ROLE_UPDATED", requestingUserId, "ROLE", roleId,
                Map.of("description", description, "category", category, "risk_level", riskLevel));

        log.info("Role {} updated successfully", roleId);
        return role;
    }

    /**
     * Delete a role
     */
    @Transactional
    public void deleteRole(UUID roleId, UUID requestingUserId) {
        log.info("Deleting role {} by user {}", roleId, requestingUserId);

        // Check if user has permission to delete roles
        if (!permissionEvaluationService.hasPermission(requestingUserId, "role.delete", null)) {
            throw new SecurityException("User does not have permission to delete roles");
        }

        PermissionDefinition role = permissionDefinitionRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        // Cannot delete system roles
        if (role.isSystem()) {
            throw new IllegalArgumentException("Cannot delete system roles");
        }

        // Check if role is in use (has permission changes)
        long usageCount = permissionChangeRepository.findByPermissionKeyAndApproved(role.getPermissionKey(), true).size();
        if (usageCount > 0) {
            throw new IllegalArgumentException("Cannot delete role that is in use. Found " + usageCount + " active assignments");
        }

        permissionDefinitionRepository.delete(role);

        // Audit log
        securityAuditService.logEvent("ROLE_DELETED", requestingUserId, "ROLE", roleId,
                Map.of("permission_key", role.getPermissionKey()));

        log.info("Role {} deleted successfully", roleId);
    }

    /**
     * Grant permissions to user
     */
    @Transactional
    public void grantPermissions(UUID userId, List<String> permissionKeys, UUID resourceId, 
                               String reason, LocalDateTime expiresAt, UUID requestingUserId) {
        log.info("Granting permissions {} to user {} by user {}", permissionKeys, userId, requestingUserId);

        // Validate inputs
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (permissionKeys == null || permissionKeys.isEmpty()) {
            throw new IllegalArgumentException("Permission keys are required");
        }

        // Check if requesting user has permission to grant permissions
        if (!permissionEvaluationService.hasPermission(requestingUserId, "user.manage", resourceId)) {
            throw new SecurityException("User does not have permission to grant permissions");
        }

        // Validate permission keys exist
        for (String permissionKey : permissionKeys) {
            if (!permissionDefinitionRepository.existsByPermissionKey(permissionKey)) {
                throw new IllegalArgumentException("Permission key does not exist: " + permissionKey);
            }
        }

        // Create permission changes for each permission
        for (String permissionKey : permissionKeys) {
            PermissionChange change = new PermissionChange(userId, requestingUserId, permissionKey, 
                    PermissionChange.ChangeType.GRANT);
            change.setReason(reason);
            change.setResourceId(resourceId);
            change.setEffectiveFrom(LocalDateTime.now());
            change.setExpiresAt(expiresAt);
            change.setTemporary(expiresAt != null);
            
            permissionChangeRepository.save(change);
        }

        // Clear user's permission cache
        permissionEvaluationService.clearUserCache(userId);

        // Audit log
        securityAuditService.logEvent("PERMISSIONS_GRANTED", requestingUserId, "USER", userId,
                Map.of("permission_keys", permissionKeys, "resource_id", resourceId, 
                       "reason", reason, "expires_at", expiresAt));

        log.info("Permissions granted successfully to user {}", userId);
    }

    /**
     * Revoke permissions from user
     */
    @Transactional
    public void revokePermissions(UUID userId, List<String> permissionKeys, UUID resourceId, 
                                String reason, UUID requestingUserId) {
        log.info("Revoking permissions {} from user {} by user {}", permissionKeys, userId, requestingUserId);

        // Validate inputs
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (permissionKeys == null || permissionKeys.isEmpty()) {
            throw new IllegalArgumentException("Permission keys are required");
        }

        // Check if requesting user has permission to revoke permissions
        if (!permissionEvaluationService.hasPermission(requestingUserId, "user.manage", resourceId)) {
            throw new SecurityException("User does not have permission to revoke permissions");
        }

        // Create permission changes for each permission
        for (String permissionKey : permissionKeys) {
            PermissionChange change = new PermissionChange(userId, requestingUserId, permissionKey, 
                    PermissionChange.ChangeType.REVOKE);
            change.setReason(reason);
            change.setResourceId(resourceId);
            change.setEffectiveFrom(LocalDateTime.now());
            change.setEffectiveUntil(LocalDateTime.now());
            
            permissionChangeRepository.save(change);
        }

        // Clear user's permission cache
        permissionEvaluationService.clearUserCache(userId);

        // Audit log
        securityAuditService.logEvent("PERMISSIONS_REVOKED", requestingUserId, "USER", userId,
                Map.of("permission_keys", permissionKeys, "resource_id", resourceId, "reason", reason));

        log.info("Permissions revoked successfully from user {}", userId);
    }

    /**
     * Get user permissions
     */
    public Set<String> getUserPermissions(UUID userId, UUID requestingUserId) {
        log.debug("Getting permissions for user {} by user {}", userId, requestingUserId);

        // Users can view their own permissions, or admins can view any user's permissions
        if (!userId.equals(requestingUserId) && 
            !permissionEvaluationService.hasPermission(requestingUserId, "user.manage", null)) {
            throw new SecurityException("User does not have permission to view these permissions");
        }

        return permissionEvaluationService.getEffectivePermissions(userId);
    }

    /**
     * Get permission changes history for user
     */
    public Page<PermissionChange> getPermissionChanges(UUID userId, Pageable pageable, UUID requestingUserId) {
        log.debug("Getting permission changes for user {} by user {}", userId, requestingUserId);

        // Users can view their own changes, or admins can view any user's changes
        if (!userId.equals(requestingUserId) && 
            !permissionEvaluationService.hasPermission(requestingUserId, "audit.read", null)) {
            throw new SecurityException("User does not have permission to view permission changes");
        }

        return permissionChangeRepository.findByUserIdAndApproved(userId, true, pageable);
    }

    /**
     * Search permissions
     */
    public Page<PermissionDefinition> searchPermissions(String searchTerm, String resource, 
                                                     String category, String riskLevel, Pageable pageable, 
                                                     UUID requestingUserId) {
        log.debug("Searching permissions with term '{}' by user {}", searchTerm, requestingUserId);

        // Check if user has permission to search permissions
        if (!permissionEvaluationService.hasPermission(requestingUserId, "role.read", null)) {
            throw new SecurityException("User does not have permission to search permissions");
        }

        if (StringUtils.hasText(searchTerm)) {
            return permissionDefinitionRepository.findBySearchTerm(searchTerm, pageable);
        } else {
            PermissionDefinition.RiskLevel parsedRiskLevel = null;
            if (StringUtils.hasText(riskLevel)) {
                try {
                    parsedRiskLevel = PermissionDefinition.RiskLevel.valueOf(riskLevel.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid risk level: " + riskLevel);
                }
            }
            
            return permissionDefinitionRepository.findWithFilters(resource, category, parsedRiskLevel, false, pageable);
        }
    }

    /**
     * Get permission statistics
     */
    public Map<String, Object> getPermissionStatistics(UUID requestingUserId) {
        log.debug("Getting permission statistics for user {}", requestingUserId);

        // Check if user has permission to view statistics
        if (!permissionEvaluationService.hasPermission(requestingUserId, "audit.read", null)) {
            throw new SecurityException("User does not have permission to view permission statistics");
        }

        Map<String, Object> statistics = new HashMap<>();

        // Get permission counts by resource
        List<Object[]> resourceCounts = permissionDefinitionRepository.getPermissionCountsByResource();
        Map<String, Long> resourceMap = resourceCounts.stream()
                .collect(Collectors.toMap(obj -> obj[0].toString(), obj -> (Long) obj[1]));
        statistics.put("by_resource", resourceMap);

        // Get permission counts by category
        List<Object[]> categoryCounts = permissionDefinitionRepository.getPermissionCountsByCategory();
        Map<String, Long> categoryMap = categoryCounts.stream()
                .collect(Collectors.toMap(obj -> obj[0].toString(), obj -> (Long) obj[1]));
        statistics.put("by_category", categoryMap);

        // Get permission counts by risk level
        List<Object[]> riskLevelCounts = permissionDefinitionRepository.getPermissionCountsByRiskLevel();
        Map<String, Long> riskLevelMap = riskLevelCounts.stream()
                .collect(Collectors.toMap(obj -> obj[0].toString(), obj -> (Long) obj[1]));
        statistics.put("by_risk_level", riskLevelMap);

        // Get total counts
        statistics.put("total_permissions", (long) permissionDefinitionRepository.count());
        statistics.put("system_permissions", (long) permissionDefinitionRepository.findBySystemTrue().size());

        return statistics;
    }

    /**
     * Batch permission evaluation
     */
    public Map<String, Boolean> batchEvaluatePermissions(UUID userId, List<String> permissionKeys, 
                                                       UUID resourceId, UUID requestingUserId) {
        log.debug("Batch evaluating {} permissions for user {} by user {}", 
                permissionKeys.size(), userId, requestingUserId);

        // Check if user has permission to evaluate permissions
        if (!permissionEvaluationService.hasPermission(requestingUserId, "user.manage", resourceId)) {
            throw new SecurityException("User does not have permission to evaluate permissions");
        }

        return permissionEvaluationService.evaluatePermissions(userId, permissionKeys, resourceId);
    }
}