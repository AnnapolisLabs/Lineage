package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.service.PermissionEvaluationService;
import com.annapolislabs.lineage.service.RoleManagementService;
import com.annapolislabs.lineage.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * REST controller for permission evaluation and RBAC operations
 * Provides endpoints for checking user permissions, evaluating access, and managing roles
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/rbac")
@RequiredArgsConstructor
@Tag(name = "Permission Evaluation", description = "Permission checking and evaluation APIs")
public class PermissionEvaluationController {
    private final UserRepository userRepository;

    private final PermissionEvaluationService permissionEvaluationService;
    private final RoleManagementService roleManagementService;

    /**
     * Check if the current user has a specific permission
     */
    @PostMapping("/permissions/check")
    @Operation(
            summary = "Check user permission",
            description = "Check if the current user has a specific permission"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Permission check completed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Map<String, Object>> checkPermission(
            @Parameter(description = "Permission key to check", required = true)
            @RequestParam String permission,

            @Parameter(description = "Resource ID for scoped permissions", required = false)
            @RequestParam(required = false) UUID resourceId) {

        UUID userId = getCurrentUserId();
        boolean hasPermission = permissionEvaluationService.hasPermission(userId, permission, resourceId);

        Map<String, Object> response = new HashMap<>();
        response.put("user_id", userId);
        response.put("permission", permission);
        response.put("resource_id", resourceId);
        response.put("authorized", hasPermission);
        response.put("timestamp", System.currentTimeMillis());

        log.debug("Permission check for user {}: {} = {}", userId, permission, hasPermission);

        return ResponseEntity.ok(response);
    }

    /**
     * Batch evaluate multiple permissions
     */
    @PostMapping("/permissions/evaluate")
    @Operation(
            summary = "Batch evaluate permissions",
            description = "Evaluate multiple permissions for the current user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Batch evaluation completed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Map<String, Object>> evaluatePermissions(
            @Parameter(description = "List of permission keys to evaluate", required = true)
            @RequestBody Map<String, Object> request) {

        UUID userId = getCurrentUserId();

        @SuppressWarnings("unchecked")
        Set<String> permissions = (Set<String>) request.get("permissions");
        UUID resourceId = request.get("resource_id") != null ? UUID.fromString(request.get("resource_id").toString()) : null;

        long startTime = System.currentTimeMillis();
        Map<String, Boolean> results = permissionEvaluationService.evaluatePermissions(
                userId, permissions.stream().toList(), resourceId);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("user_id", userId);
        response.put("resource_id", resourceId);
        response.put("results", results);
        response.put("evaluation_duration_ms", duration);
        response.put("total_evaluations", permissions.size());
        response.put("successful_evaluations", results.values().stream().mapToLong(v -> v ? 1 : 0).sum());

        log.debug("Batch permission evaluation for user {}: {} permissions in {}ms",
                userId, permissions.size(), duration);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all effective permissions for the current user
     */
    @GetMapping("/permissions/user")
    @Operation(
            summary = "Get user permissions",
            description = "Get all effective permissions for the current user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User permissions retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Object>> getUserPermissions(
            @Parameter(description = "User ID (optional, defaults to current user)")
            @RequestParam(required = false) UUID userId,

            @Parameter(description = "Resource ID for scoped permissions")
            @RequestParam(required = false) UUID resourceId) {

        UUID targetUserId = userId != null ? userId : getCurrentUserId();
        Set<String> permissions = permissionEvaluationService.getEffectivePermissions(targetUserId, resourceId);

        Map<String, Object> response = new HashMap<>();
        response.put("user_id", targetUserId);
        response.put("resource_id", resourceId);
        response.put("permissions", permissions);
        response.put("permission_count", permissions.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Check role hierarchy - if user has role or higher
     */
    @PostMapping("/roles/check-hierarchy")
    @Operation(
            summary = "Check role hierarchy",
            description = "Check if user has specified role or higher in the hierarchy"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role hierarchy check completed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Map<String, Object>> checkRoleHierarchy(
            @Parameter(description = "Required role", required = true)
            @RequestParam String requiredRole,

            @Parameter(description = "User ID (optional, defaults to current user)")
            @RequestParam(required = false) UUID userId) {

        UUID targetUserId = userId != null ? userId : getCurrentUserId();

        try {
            com.annapolislabs.lineage.entity.UserRole role =
                    com.annapolislabs.lineage.entity.UserRole.valueOf(requiredRole.toUpperCase());

            boolean hasRoleOrHigher = permissionEvaluationService.hasRoleOrHigher(targetUserId, role);

            Map<String, Object> response = new HashMap<>();
            response.put("user_id", targetUserId);
            response.put("required_role", requiredRole);
            response.put("has_role_or_higher", hasRoleOrHigher);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid role: " + requiredRole);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Clear permission cache for a user
     */
    @PostMapping("/cache/clear")
    @Operation(
            summary = "Clear permission cache",
            description = "Clear permission cache for a specific user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cache cleared successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Map<String, String>> clearUserCache(
            @Parameter(description = "User ID to clear cache for")
            @RequestParam UUID userId) {

        // Check if user can clear caches (admin users)
        UUID currentUserId = getCurrentUserId();
        if (!permissionEvaluationService.hasPermission(currentUserId, "system.configure", null)) {
            return ResponseEntity.status(403).body(Map.of("error", "Insufficient permissions to clear caches"));
        }

        permissionEvaluationService.clearUserCache(userId);

        return ResponseEntity.ok(Map.of("message", "Cache cleared successfully for user " + userId));
    }

    /**
     * Get permission statistics
     */
    @GetMapping("/statistics")
    @Operation(
            summary = "Get permission statistics",
            description = "Get statistics about permission usage and distribution"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Map<String, Object>> getPermissionStatistics() {
        UUID currentUserId = getCurrentUserId();

        if (!permissionEvaluationService.hasPermission(currentUserId, "audit.read", null)) {
            return ResponseEntity.status(403).body(Map.of("error", "Insufficient permissions to view statistics"));
        }

        Map<String, Object> statistics = roleManagementService.getPermissionStatistics(currentUserId);
        return ResponseEntity.ok(statistics);
    }

    /**
     * Health check for permission service
     */
    @GetMapping("/health")
    @Operation(
            summary = "Permission service health check",
            description = "Check if the permission evaluation service is running"
    )
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "Permission Evaluation Service");
        response.put("status", "healthy");
        response.put("timestamp", System.currentTimeMillis());
        response.put("version", "1.0.0");

        // Test basic permission evaluation
        try {
            // This should always return false for non-existent user
            boolean testResult = permissionEvaluationService.hasPermission(
                    UUID.randomUUID(), "test.permission", null);
            response.put("test_evaluation", "passed");
        } catch (Exception e) {
            response.put("test_evaluation", "failed");
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get current user ID from authentication context
     */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }

        // Extract user ID from authentication details
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            // Principal is UserDetails, extract email and look up user
            String email = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            return userRepository.findByEmail(email)
                    .map(com.annapolislabs.lineage.entity.User::getId)
                    .orElseThrow(() -> new SecurityException("User not found in database"));
        } else if (principal instanceof String) {
            try {
                return UUID.fromString((String) principal);
            } catch (IllegalArgumentException e) {
                throw new SecurityException("Invalid user ID in authentication context");
            }
        }

        throw new SecurityException("Unable to extract user ID from authentication context");
    }
}
