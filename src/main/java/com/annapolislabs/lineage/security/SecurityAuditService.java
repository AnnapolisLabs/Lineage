package com.annapolislabs.lineage.security;


import com.annapolislabs.lineage.entity.AuditLog;
import com.annapolislabs.lineage.entity.AuditSeverity;
import com.annapolislabs.lineage.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Centralized security audit facility that persists authentication, authorization, and anomaly events while also
 * emitting high-signal log lines for realtime monitoring.
 */
@Service
public class SecurityAuditService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditService.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");
    
    // Constants for repeated string literals
    private static final String EVENT_TYPE = "event_type";
    private static final String TIMESTAMP = "timestamp";
    private static final String IP_ADDRESS = "ip_address";
    private static final String USER_AGENT = "user_agent";
    private static final String SUCCESS = "success";
    private static final String SYSTEM = "SYSTEM";
    private static final String UNKNOWN = "unknown";
    private static final String REQUEST = "request";

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectFactory<HttpServletRequest> httpRequestFactory;

    /**
     * Persists an audit log entry capturing generic security activity while mirroring details to the security logger.
     *
     * @param userId subject performing the action, nullable for system events.
     * @param action short verb summarizing the event.
     * @param resource domain aggregate being touched.
     * @param resourceId identifier of the resource when available.
     * @param severity severity classification used for alerts.
     * @param details additional structured metadata about the event.
     */
    public void logSecurityEvent(String userId, String action, String resource, String resourceId,
                                AuditSeverity severity, Map<String, Object> details) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId != null ? UUID.fromString(userId) : null);
            auditLog.setAction(action);
            auditLog.setResource(resource);
            auditLog.setResourceId(resourceId);
            auditLog.setDetails(details != null ? details : new HashMap<>());
            auditLog.setSeverity(severity);
            auditLog.setIpAddress(getClientIpAddress());
            auditLog.setUserAgent(getUserAgent());
            auditLog.setCreatedAt(LocalDateTime.now());

            auditLogRepository.save(auditLog);

            // Log to security logger for immediate attention
            securityLogger.warn("Security Event: {} | User: {} | Action: {} | Resource: {} | Severity: {} | Details: {}", 
                    LocalDateTime.now(), userId, action, resource, severity, details);

            // Alert on critical events
            if (severity == AuditSeverity.CRITICAL) {
                alertOnCriticalEvent(auditLog);
            }

        } catch (Exception e) {
            logger.error("Failed to log security event", e);
        }
    }

    /**
     * Log MFA-related events
     */
    public void logMfaEvent(String userId, String action, AuditSeverity severity, Map<String, Object> details) {
        Map<String, Object> mfaDetails = new HashMap<>(details != null ? details : new HashMap<>());
        mfaDetails.put(EVENT_TYPE, "MFA");
        mfaDetails.put(TIMESTAMP, LocalDateTime.now());
        
        logSecurityEvent(userId, action, "MFA", userId, severity, mfaDetails);
        
        // Additional logging for MFA events
        securityLogger.warn("MFA Event: {} | User: {} | Action: {} | Severity: {} | Details: {}", 
                LocalDateTime.now(), userId, action, severity, mfaDetails);
    }

    /**
     * Log suspicious activity
     */
    public void logSuspiciousActivity(String userId, String action, AuditSeverity severity, Map<String, Object> details) {
        Map<String, Object> suspiciousDetails = new HashMap<>(details != null ? details : new HashMap<>());
        suspiciousDetails.put(EVENT_TYPE, "SUSPICIOUS_ACTIVITY");
        suspiciousDetails.put(IP_ADDRESS, getClientIpAddress());
        suspiciousDetails.put(USER_AGENT, getUserAgent());
        suspiciousDetails.put(TIMESTAMP, LocalDateTime.now());
        
        logSecurityEvent(userId, action, "SECURITY", userId, severity, suspiciousDetails);
        
        // Immediate warning for suspicious activity
        securityLogger.error("SUSPICIOUS ACTIVITY: {} | User: {} | Action: {} | IP: {} | User Agent: {} | Details: {}", 
                LocalDateTime.now(), userId, action, getClientIpAddress(), getUserAgent(), details);
        
        // Alert on suspicious activities
        alertOnSuspiciousActivity(userId, action);
    }

    /**
     * Log authentication events
     */
    public void logAuthenticationEvent(String userId, String action, AuditSeverity severity, boolean success, Map<String, Object> additionalDetails) {
        Map<String, Object> authDetails = new HashMap<>(additionalDetails != null ? additionalDetails : new HashMap<>());
        authDetails.put(EVENT_TYPE, "AUTHENTICATION");
        authDetails.put(SUCCESS, success);
        authDetails.put(IP_ADDRESS, getClientIpAddress());
        authDetails.put(USER_AGENT, getUserAgent());
        authDetails.put(TIMESTAMP, LocalDateTime.now());
        
        String resource = success ? "AUTH_SUCCESS" : "AUTH_FAILURE";
        logSecurityEvent(userId, action, resource, userId, severity, authDetails);
        
        if (!success) {
            securityLogger.warn("Authentication Failure: {} | User: {} | Action: {} | IP: {} | User Agent: {}", 
                    LocalDateTime.now(), userId, action, getClientIpAddress(), getUserAgent());
        }
    }

    /**
     * Log authorization events
     */
    public void logAuthorizationEvent(String userId, String action, String resource, String resourceId, boolean success, Map<String, Object> details) {
        Map<String, Object> authzDetails = new HashMap<>(details != null ? details : new HashMap<>());
        authzDetails.put(EVENT_TYPE, "AUTHORIZATION");
        authzDetails.put(SUCCESS, success);
        authzDetails.put(IP_ADDRESS, getClientIpAddress());
        authzDetails.put(USER_AGENT, getUserAgent());
        authzDetails.put(TIMESTAMP, LocalDateTime.now());
        
        AuditSeverity severity = success ? AuditSeverity.INFO : AuditSeverity.WARNING;
        logSecurityEvent(userId, action, resource, resourceId, severity, authzDetails);
        
        if (!success) {
            securityLogger.warn("Authorization Failure: {} | User: {} | Action: {} | Resource: {} | IP: {}", 
                    LocalDateTime.now(), userId, action, resource, getClientIpAddress());
        }
    }

    /**
     * Log account security events (password changes, lock/unlock, etc.)
     */
    public void logAccountSecurityEvent(String userId, String action, AuditSeverity severity, Map<String, Object> details) {
        Map<String, Object> accountDetails = new HashMap<>(details != null ? details : new HashMap<>());
        accountDetails.put(EVENT_TYPE, "ACCOUNT_SECURITY");
        accountDetails.put(IP_ADDRESS, getClientIpAddress());
        accountDetails.put(USER_AGENT, getUserAgent());
        accountDetails.put(TIMESTAMP, LocalDateTime.now());
        
        logSecurityEvent(userId, action, "ACCOUNT_SECURITY", userId, severity, accountDetails);
        
        // Immediate logging for account security events
        securityLogger.warn("Account Security: {} | User: {} | Action: {} | Severity: {} | IP: {}", 
                LocalDateTime.now(), userId, action, severity, getClientIpAddress());
    }

    /**
     * Log data access events
     */
    public void logDataAccessEvent(String userId, String action, String resource, String resourceId, AuditSeverity severity, Map<String, Object> details) {
        Map<String, Object> accessDetails = new HashMap<>(details != null ? details : new HashMap<>());
        accessDetails.put(EVENT_TYPE, "DATA_ACCESS");
        accessDetails.put(IP_ADDRESS, getClientIpAddress());
        accessDetails.put(USER_AGENT, getUserAgent());
        accessDetails.put(TIMESTAMP, LocalDateTime.now());
        
        logSecurityEvent(userId, action, resource, resourceId, severity, accessDetails);
    }

    /**
     * Log system events
     */
    public void logSystemEvent(String action, AuditSeverity severity, Map<String, Object> details) {
        Map<String, Object> systemDetails = new HashMap<>(details != null ? details : new HashMap<>());
        systemDetails.put(EVENT_TYPE, SYSTEM);
        systemDetails.put(TIMESTAMP, LocalDateTime.now());
        
        logSecurityEvent(null, action, "SYSTEM", "SYSTEM", severity, systemDetails);
        
        securityLogger.error("System Event: {} | Action: {} | Severity: {} | Details: {}", 
                LocalDateTime.now(), action, severity, details);
    }

    /**
     * Get security events for a user
     */
    public java.util.List<AuditLog> getSecurityEvents(UUID userId, int limit) {
        try {
            return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId)
                    .stream()
                    .limit(limit)
                    .toList();
        } catch (Exception e) {
            logger.error("Failed to get security events for user: {}", userId, e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Get recent suspicious activities
     */
    public java.util.List<AuditLog> getSuspiciousActivities(int limit) {
        try {
            return auditLogRepository.findBySeverityInOrderByCreatedAtDesc(
                            java.util.List.of(AuditSeverity.WARNING, AuditSeverity.ERROR, AuditSeverity.CRITICAL))
                    .stream()
                    .filter(log -> log.getDetails().get(EVENT_TYPE) != null &&
                            log.getDetails().get(EVENT_TYPE).toString().contains("SUSPICIOUS"))
                    .limit(limit)
                    .toList();
        } catch (Exception e) {
            logger.error("Failed to get suspicious activities", e);
            return java.util.Collections.emptyList();
        }
    }
    
    /**
     * Log successful authentication
     */
    public void logSuccessfulAuthentication(String userId, String email) {
        logAuthenticationEvent(userId, "LOGIN_SUCCESS", AuditSeverity.INFO, true,
            java.util.Map.of("email", email, "method", "password"));
    }
    
    /**
     * Log authentication failure
     */
    public void logAuthenticationFailure(String action, String reason) {
        logAuthenticationEvent(UNKNOWN, action, AuditSeverity.WARNING, false,
            java.util.Map.of("reason", reason));
    }
    
    /**
     * Log password reset request
     */
    public void logPasswordResetRequest(String email, String ipAddress, boolean success) {
        logSecurityEvent(null, success ? "PASSWORD_RESET_SUCCESS" : "PASSWORD_RESET_REQUEST",
            "USER_SECURITY", email,
            success ? AuditSeverity.INFO : AuditSeverity.WARNING,
            java.util.Map.of("email", email, "ipAddress", ipAddress, SUCCESS, success));
    }

    // Private helper methods

    private String getClientIpAddress() {
        try {
            HttpServletRequest httpRequest = httpRequestFactory.getObject();

            String xForwardedFor = httpRequest.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty() && !UNKNOWN.equalsIgnoreCase(xForwardedFor)) {
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIp = httpRequest.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty() && !UNKNOWN.equalsIgnoreCase(xRealIp)) {
                return xRealIp;
            }

            String remoteAddr = httpRequest.getRemoteAddr();
            return remoteAddr != null ? remoteAddr : UNKNOWN;
        } catch (IllegalStateException ex) {
            // No thread-bound request (e.g., application startup, background task)
            logger.debug("SecurityAuditService: No HttpServletRequest bound to current thread; using system placeholder IP.");
            return "system";
        } catch (Exception e) {
            logger.warn("Failed to get client IP address", e);
            return UNKNOWN;
        }
    }

    private String getUserAgent() {
        try {
            HttpServletRequest httpRequest = httpRequestFactory.getObject();
            String userAgent = httpRequest.getHeader("User-Agent");
            return userAgent != null ? userAgent : UNKNOWN;
        } catch (IllegalStateException ex) {
            // No thread-bound request (e.g., application startup, background task)
            logger.debug("SecurityAuditService: No HttpServletRequest bound to current thread; using system initializer user agent.");
            return "system-initializer";
        } catch (Exception e) {
            logger.warn("Failed to get user agent", e);
            return UNKNOWN;
        }
    }

    private void alertOnCriticalEvent(AuditLog auditLog) {
        // In a real implementation, this would send alerts to administrators
        // via email, Slack, PagerDuty, etc.
        securityLogger.error("CRITICAL SECURITY ALERT: {}", auditLog);
    }

    private void alertOnSuspiciousActivity(String userId, String action) {
        // Check if this is part of a pattern (multiple failed attempts, etc.)
        // In a real implementation, this might trigger account lockout or additional monitoring
        
        securityLogger.warn("Suspicious Activity Alert: User {} performed action {} from IP {}", 
                userId, action, getClientIpAddress());
        
        // Could integrate with automated response systems here
        // Example: ResponseService.handleSuspiciousActivity(userId);
    }

    public void logEvent(String eventType, UUID requestingUserId, String task, UUID taskId, Map<String, Object> details) {
        try {
            Map<String, Object> eventDetails = new HashMap<>(details != null ? details : new HashMap<>());
            eventDetails.put(EVENT_TYPE, eventType);
            eventDetails.put(TIMESTAMP, LocalDateTime.now());
            eventDetails.put(IP_ADDRESS, getClientIpAddress());
            eventDetails.put(USER_AGENT, getUserAgent());
            eventDetails.put(REQUEST, task);
            
            if (taskId != null) {
                eventDetails.put("task_id", taskId.toString());
            }
            
            AuditSeverity severity = determineEventSeverity(eventType);
            logSecurityEvent(
                requestingUserId != null ? requestingUserId.toString() : null,
                task,
                eventType,
                taskId != null ? taskId.toString() : null,
                severity,
                eventDetails
            );
            
            // Log to security logger for important events
            if (severity != AuditSeverity.INFO) {
                securityLogger.warn("Security Event: {} | User: {} | Event: {} | Task: {} | Details: {}", 
                        LocalDateTime.now(), requestingUserId, eventType, task, details);
            }
            
        } catch (Exception e) {
            logger.error("Failed to log event: {} for user: {}", eventType, requestingUserId, e);
        }
    }

    /**
     * Get audit count for time range
     */
    public long getAuditCount(LocalDateTime since) {
        try {
            return auditLogRepository.countByCreatedAtAfter(since);
        } catch (Exception e) {
            logger.error("Failed to get audit count since: {}", since, e);
            return 0;
        }
    }

    /**
     * Log collaboration events (team, task, review operations)
     */
    public void logCollaborationEvent(String userId, String action, String resource, String resourceId, Map<String, Object> details) {
        Map<String, Object> collabDetails = new HashMap<>(details != null ? details : new HashMap<>());
        collabDetails.put(EVENT_TYPE, "COLLABORATION");
        collabDetails.put(IP_ADDRESS, getClientIpAddress());
        collabDetails.put(USER_AGENT, getUserAgent());
        collabDetails.put(TIMESTAMP, LocalDateTime.now());
        
        logSecurityEvent(userId, action, resource, resourceId, AuditSeverity.INFO, collabDetails);
    }

    /**
     * Log permission changes
     */
    public void logPermissionChange(String userId, String action, String permissionKey, String changeType, Map<String, Object> details) {
        Map<String, Object> permissionDetails = new HashMap<>(details != null ? details : new HashMap<>());
        permissionDetails.put(EVENT_TYPE, "PERMISSION_CHANGE");
        permissionDetails.put("permission_key", permissionKey);
        permissionDetails.put("change_type", changeType);
        permissionDetails.put(IP_ADDRESS, getClientIpAddress());
        permissionDetails.put(USER_AGENT, getUserAgent());
        permissionDetails.put(TIMESTAMP, LocalDateTime.now());
        
        AuditSeverity severity = determinePermissionChangeSeverity(changeType);
        logSecurityEvent(userId, action, "PERMISSION", permissionKey, severity, permissionDetails);
    }

    private AuditSeverity determineEventSeverity(String eventType) {
        return switch (eventType.toUpperCase()) {
            case "ROLE_CREATED", "ROLE_UPDATED", "PERMISSIONS_GRANTED" -> AuditSeverity.INFO;
            case "ROLE_DELETED", "PERMISSIONS_REVOKED" -> AuditSeverity.WARNING;
            case "SUSPICIOUS_ACTIVITY", "UNAUTHORIZED_ACCESS" -> AuditSeverity.ERROR;
            case "SECURITY_BREACH", "PRIVILEGE_ESCALATION" -> AuditSeverity.CRITICAL;
            default -> AuditSeverity.INFO;
        };
    }

    private AuditSeverity determinePermissionChangeSeverity(String changeType) {
        return switch (changeType.toUpperCase()) {
            case "GRANT" -> AuditSeverity.INFO;
            case "REVOKE", "SUSPEND" -> AuditSeverity.WARNING;
            case "MODIFY", "EXTEND" -> AuditSeverity.MEDIUM;
            case "BULK_CHANGE" -> AuditSeverity.HIGH;
            default -> AuditSeverity.INFO;
        };
    }
}