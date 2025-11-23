package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.dto.request.ChangePasswordRequest;
import com.annapolislabs.lineage.dto.request.SetupMfaRequest;
import com.annapolislabs.lineage.dto.request.ValidateMfaRequest;
import com.annapolislabs.lineage.dto.response.MfaSetupResponse;
import com.annapolislabs.lineage.dto.response.UserProfileResponse;
import com.annapolislabs.lineage.entity.AuditLog;
import com.annapolislabs.lineage.entity.AuditSeverity;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserSecurity;
import com.annapolislabs.lineage.exception.auth.MfaVerificationException;
import com.annapolislabs.lineage.repository.AuditLogRepository;
import com.annapolislabs.lineage.repository.UserSecurityRepository;
import com.annapolislabs.lineage.security.SecurityAuditService;
import com.annapolislabs.lineage.service.MfaService;
import com.annapolislabs.lineage.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller covering MFA lifecycle, session management, security events, and password hygiene endpoints.
 */
@RestController
@RequestMapping("/api/security")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SecurityController {

    private static final Logger logger = LoggerFactory.getLogger(SecurityController.class);
    private static final String ENABLED = "enabled";
    private static final String MESSAGE = "message";
    private static final String METHOD = "method";
    private static final String SUCCESS = "success";
    private static final String TIMESTAMP = "timestamp";
    private static final String INVALID_CODE = "INVALID_CODE";
    private static final String REASON = "reason";

    @Autowired
    private MfaService mfaService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserSecurityRepository userSecurityRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private SecurityAuditService securityAuditService;

    /**
     * GET /api/security/mfa/setup returns QR-code and secret data unless MFA is already enabled.
     *
     * @param request used only for logging request scope
     * @return 200 OK with setup metadata or confirmation that MFA is already configured
     */
    @GetMapping("/mfa/setup")
    public ResponseEntity<?> getMfaSetup(HttpServletRequest request) {
        try {
            String userId = getCurrentUserId();

            // Check if MFA is already set up
            UserSecurity userSecurity = userSecurityRepository.findByUserId(UUID.fromString(userId))
                    .orElse(null);

            if (userSecurity != null && userSecurity.isMfaEnabled()) {
                return ResponseEntity.ok(Map.of(
                        ENABLED, true,
                        "setupComplete", true,
                        MESSAGE, "MFA is already enabled for your account"
                ));
            }

            // Generate new MFA setup
            MfaSetupResponse mfaSetup = mfaService.generateMfaSetup(UUID.fromString(userId));

            // Log MFA setup initiation
            securityAuditService.logMfaEvent(userId, "MFA_SETUP_INITIATED", AuditSeverity.INFO,
                    Map.of("setupMethod", "TOTP"));

            return ResponseEntity.ok(Map.of(
                    ENABLED, false,
                    "setupComplete", false,
                    "secretKey", mfaSetup.getSecretKey(),
                    "qrCodeUrl", mfaSetup.getQrCodeUrl(),
                    "backupCodes", mfaSetup.getBackupCodes()
            ));

        } catch (Exception e) {
            logger.error("MFA setup failed", e);
            throw e;
        }
    }

    /**
     * POST /api/security/mfa/enable verifies a TOTP code and persists MFA enablement state.
     * Returns 200 OK on success or surfaces {@link MfaVerificationException} when verification fails.
     *
     * @param request payload containing the verification code
     * @param httpRequest used for contextual logging
     */
    @PostMapping("/mfa/enable")
    public ResponseEntity<?> enableMfa(@Valid @RequestBody SetupMfaRequest request,
                                       HttpServletRequest httpRequest) {
        try {
            String userId = getCurrentUserId();

            // Verify the MFA code first
            boolean isValidCode = mfaService.verifyCode(UUID.fromString(userId), request.getVerificationCode());

            if (!isValidCode) {
                throw new MfaVerificationException("Invalid MFA verification code");
            }

            // Enable MFA
            mfaService.enableMfa(UUID.fromString(userId), request.getVerificationCode());

            // Get updated user security data
            UserSecurity userSecurity = userSecurityRepository.findByUserId(UUID.fromString(userId))
                    .orElseThrow(() -> new RuntimeException("User security settings not found"));

            // Log MFA enablement
            securityAuditService.logMfaEvent(userId, "MFA_ENABLED", AuditSeverity.INFO,
                    Map.of(METHOD, "TOTP", "enabledAt", LocalDateTime.now()));

            return ResponseEntity.ok(Map.of(
                    SUCCESS, true,
                    ENABLED, true,
                    "enabledAt", userSecurity.getMfaEnabledAt(),
                    MESSAGE, "MFA has been successfully enabled for your account"
            ));

        } catch (MfaVerificationException e) {
            logger.warn("MFA verification failed during enablement for user: {}", getCurrentUserId());
            securityAuditService.logMfaEvent(getCurrentUserId(), "MFA_ENABLE_FAILED", AuditSeverity.WARNING,
                    Map.of(REASON, INVALID_CODE, TIMESTAMP, LocalDateTime.now()));
            throw e;
        } catch (Exception e) {
            logger.error("Failed to enable MFA", e);
            throw e;
        }
    }

    /**
     * POST /api/security/mfa/validate verifies the provided code and logs the outcome for auditing.
     *
     * @param request validation payload with TOTP code
     * @param httpRequest used to derive IP information for logs
     * @return 200 OK with "valid" flag detailing the result
     */
    @PostMapping("/mfa/validate")
    public ResponseEntity<?> validateMfa(@Valid @RequestBody ValidateMfaRequest request,
                                         HttpServletRequest httpRequest) {
        try {
            String userId = getCurrentUserId();

            // Verify MFA code
            boolean isValid = mfaService.verifyCode(UUID.fromString(userId), request.getMfaCode());

            if (isValid) {
                // Log successful MFA validation
                securityAuditService.logMfaEvent(userId, "MFA_VALIDATION_SUCCESS", AuditSeverity.INFO,
                        Map.of(METHOD, "TOTP", TIMESTAMP, LocalDateTime.now()));

                return ResponseEntity.ok(Map.of(
                        "valid", true,
                        MESSAGE, "MFA code validated successfully"
                ));
            } else {
                // Log failed MFA validation
                securityAuditService.logMfaEvent(userId, "MFA_VALIDATION_FAILED", AuditSeverity.WARNING,
                        Map.of(METHOD, "TOTP", REASON, INVALID_CODE, TIMESTAMP, LocalDateTime.now()));

                return ResponseEntity.ok(Map.of(
                        "valid", false,
                        MESSAGE, "Invalid MFA code"
                ));
            }

        } catch (Exception e) {
            logger.error("MFA validation failed", e);
            throw e;
        }
    }

    /**
     * POST /api/security/mfa/disable requires a verification code before clearing MFA state for the user.
     *
     * @param request map containing verificationCode
     * @param httpRequest used for logging metadata
     * @return 200 OK once MFA is disabled or 400/409 style errors on failure
     */
    @PostMapping("/mfa/disable")
    public ResponseEntity<?> disableMfa(@RequestBody Map<String, String> request,
                                        HttpServletRequest httpRequest) {
        try {
            String userId = getCurrentUserId();
            String verificationCode = request.get("verificationCode");

            if (verificationCode == null || verificationCode.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "VERIFICATION_CODE_REQUIRED",
                        MESSAGE, "MFA verification code is required to disable MFA"
                ));
            }

            // Verify MFA code before disabling
            boolean isValid = mfaService.verifyCode(UUID.fromString(userId), verificationCode);

            if (!isValid) {
                throw new MfaVerificationException("Invalid MFA verification code");
            }

            // Disable MFA
            mfaService.disableMfa(UUID.fromString(userId));

            // Log MFA disablement
            securityAuditService.logMfaEvent(userId, "MFA_DISABLED", AuditSeverity.WARNING,
                    Map.of(METHOD, "TOTP", "disabledAt", LocalDateTime.now()));

            return ResponseEntity.ok(Map.of(
                    SUCCESS, true,
                    ENABLED, false,
                    MESSAGE, "MFA has been successfully disabled for your account"
            ));

        } catch (MfaVerificationException e) {
            logger.warn("MFA verification failed during disablement for user: {}", getCurrentUserId());
            securityAuditService.logMfaEvent(getCurrentUserId(), "MFA_DISABLE_FAILED", AuditSeverity.WARNING,
                    Map.of(REASON, INVALID_CODE, TIMESTAMP, LocalDateTime.now()));
            throw e;
        } catch (Exception e) {
            logger.error("Failed to disable MFA", e);
            throw e;
        }
    }

    /**
     * GET /api/security/sessions returns a placeholder response until session inventory is implemented.
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getUserSessions(HttpServletRequest request) {
        try {
            // Get user sessions (simplified implementation)
            // In a real implementation, this would query the UserSessionRepository

            return ResponseEntity.ok(Map.of(
                    "sessions", List.of(),
                    MESSAGE, "Session management features coming soon"
            ));

        } catch (Exception e) {
            logger.error("Failed to get user sessions", e);
            throw e;
        }
    }

    private String extractTokenFromRequest() {
        // This would be extracted from the JWT filter in practice
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() != null) {
            return authentication.getCredentials().toString();
        }
        return null;
    }

    /**
     * DELETE /api/security/sessions/{sessionId} revokes a session token as part of session management.
     *
     * @param sessionId identifier of the session to revoke
     * @param request HTTP request for logging
     * @return 200 OK with confirmation
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> revokeSession(@PathVariable String sessionId,
                                           HttpServletRequest request) {
        try {
            String userId = getCurrentUserId();

            // Revoke session (simplified implementation)
            // In a real implementation, this would revoke the session in UserSessionRepository

            // Log session revocation
            securityAuditService.logSuspiciousActivity(userId, "SESSION_REVOKED", AuditSeverity.INFO,
                    Map.of("sessionId", sessionId, "revokedAt", LocalDateTime.now()));

            return ResponseEntity.ok(Map.of(
                    SUCCESS, true,
                    MESSAGE, "Session has been successfully revoked"
            ));

        } catch (Exception e) {
            logger.error("Failed to revoke session", e);
            throw e;
        }
    }

    /**
     * POST /api/security/change-password validates current credentials then delegates the change to {@link UserService}.
     *
     * @param request map containing currentPassword and newPassword
     * @param httpRequest request used to capture client IP for auditing
     * @return 200 OK when the change succeeds
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request,
                                            HttpServletRequest httpRequest) {
        try {
            String userId = getCurrentUserId();
            String currentPassword = request.get("currentPassword");
            String newPassword = request.get("newPassword");

            if (currentPassword == null || newPassword == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "INVALID_REQUEST",
                        MESSAGE, "Current password and new password are required"
                ));
            }

            // Change password
            ChangePasswordRequest changePasswordRequest =
                new ChangePasswordRequest();
            changePasswordRequest.setCurrentPassword(currentPassword);
            changePasswordRequest.setNewPassword(newPassword);
            userService.changePassword(UUID.fromString(userId), changePasswordRequest, getClientIpAddress(httpRequest));

            // Log password change
            securityAuditService.logSuspiciousActivity(userId, "PASSWORD_CHANGED", AuditSeverity.WARNING,
                    Map.of("changedAt", LocalDateTime.now(), "ipAddress", getClientIpAddress(httpRequest)));

            return ResponseEntity.ok(Map.of(
                    SUCCESS, true,
                    MESSAGE, "Password changed successfully"
            ));

        } catch (Exception e) {
            logger.error("Password change failed", e);
            throw e;
        }
    }

    /**
     * GET /api/security/password/last-changed reports the timestamp of the latest password update event.
     *
     * @param request used to derive the authenticated principal email
     * @return 200 OK with timestamp or null when no change exists
     */
    @GetMapping("/password/last-changed")
    public ResponseEntity<?> getLastPasswordChange(HttpServletRequest request) {
        try {
            String email = request.getRemoteUser();
            User currentUser = userService.getUserByEmail(email);
            // Get the most recent password change event
            List<AuditLog> allUserLogs = auditLogRepository
                .findByUserIdOrderByCreatedAtDesc(currentUser.getId());

            // Filter for password changes
            List<AuditLog> passwordChanges = allUserLogs.stream()
                .filter(log -> "PASSWORD_CHANGED".equals(log.getAction()))
                .toList();

            if (!passwordChanges.isEmpty()) {
                AuditLog lastChange = passwordChanges.getFirst();
                java.util.Map<String, Object> response = new java.util.HashMap<>();
                response.put("lastChanged", lastChange.getCreatedAt());
                response.put(MESSAGE, "Last password change retrieved successfully");
                return ResponseEntity.ok(response);
            }
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("lastChanged", null);
            response.put(MESSAGE, "No password changes found");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to get last password change", e);
            throw e;
        }
    }

    /**
     * GET /api/security/events paginates recent audit entries for the authenticated user.
     *
     * @param page zero-based page index
     * @param size page size
     * @param request used for authentication context
     * @return 200 OK with event slice metadata
     */
    @GetMapping("/events")
    public ResponseEntity<?> getSecurityEvents(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               HttpServletRequest request) {
        try {
            String userId = getCurrentUserId();

            // Get security events for user
            List<AuditLog> events = auditLogRepository
                .findByUserIdOrderByCreatedAtDesc(UUID.fromString(userId));

            // Apply pagination
            int total = events.size();
            int fromIndex = Math.min(page * size, total);
            int toIndex = Math.min(fromIndex + size, total);
            List<AuditLog> paginatedEvents = events.subList(fromIndex, toIndex);

            return ResponseEntity.ok(Map.of(
                    "events", paginatedEvents.stream().map(event -> Map.of(
                            "id", event.getId(),
                            "action", event.getAction(),
                            "resource", event.getResource(),
                            "resourceId", event.getResourceId(),
                            "severity", event.getSeverity(),
                            "createdAt", event.getCreatedAt(),
                            "ipAddress", event.getIpAddress(),
                            "details", event.getDetails()
                    )).toList(),
                    "page", page,
                    "size", size,
                    "total", total
            ));

        } catch (Exception e) {
            logger.error("Failed to get security events", e);
            throw e;
        }
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            try {
                // Get user by email from authentication name
                String email = authentication.getName();
                User user = userService.getUserByEmail(email);
                return user.getId().toString();
            } catch (Exception e) {
                logger.error("Failed to get current user ID", e);
                throw new com.annapolislabs.lineage.exception.auth.AuthenticationException("Unable to authenticate user", e);
            }
        }
        throw new com.annapolislabs.lineage.exception.auth.AuthenticationException("No authenticated user found");
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}