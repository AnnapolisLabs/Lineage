package com.annapolislabs.lineage.security;

import com.annapolislabs.lineage.entity.AuditLog;
import com.annapolislabs.lineage.entity.AuditSeverity;
import com.annapolislabs.lineage.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for security auditing and logging of security events
 * Provides centralized logging for authentication, authorization, and security events
 */
@Service
public class SecurityAuditService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditService.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private HttpServletRequest httpRequest;

    /**
     * Log a general security event
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
        mfaDetails.put("event_type", "MFA");
        mfaDetails.put("timestamp", LocalDateTime.now());
        
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
        suspiciousDetails.put("event_type", "SUSPICIOUS_ACTIVITY");
        suspiciousDetails.put("ip_address", getClientIpAddress());
        suspiciousDetails.put("user_agent", getUserAgent());
        suspiciousDetails.put("timestamp", LocalDateTime.now());
        
        logSecurityEvent(userId, action, "SECURITY", userId, severity, suspiciousDetails);
        
        // Immediate warning for suspicious activity
        securityLogger.error("SUSPICIOUS ACTIVITY: {} | User: {} | Action: {} | IP: {} | User Agent: {} | Details: {}", 
                LocalDateTime.now(), userId, action, getClientIpAddress(), getUserAgent(), details);
        
        // Alert on suspicious activities
        alertOnSuspiciousActivity(userId, action, details);
    }

    /**
     * Log authentication events
     */
    public void logAuthenticationEvent(String userId, String action, AuditSeverity severity, boolean success, Map<String, Object> additionalDetails) {
        Map<String, Object> authDetails = new HashMap<>(additionalDetails != null ? additionalDetails : new HashMap<>());
        authDetails.put("event_type", "AUTHENTICATION");
        authDetails.put("success", success);
        authDetails.put("ip_address", getClientIpAddress());
        authDetails.put("user_agent", getUserAgent());
        authDetails.put("timestamp", LocalDateTime.now());
        
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
        authzDetails.put("event_type", "AUTHORIZATION");
        authzDetails.put("success", success);
        authzDetails.put("ip_address", getClientIpAddress());
        authzDetails.put("user_agent", getUserAgent());
        authzDetails.put("timestamp", LocalDateTime.now());
        
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
        accountDetails.put("event_type", "ACCOUNT_SECURITY");
        accountDetails.put("ip_address", getClientIpAddress());
        accountDetails.put("user_agent", getUserAgent());
        accountDetails.put("timestamp", LocalDateTime.now());
        
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
        accessDetails.put("event_type", "DATA_ACCESS");
        accessDetails.put("ip_address", getClientIpAddress());
        accessDetails.put("user_agent", getUserAgent());
        accessDetails.put("timestamp", LocalDateTime.now());
        
        logSecurityEvent(userId, action, resource, resourceId, severity, accessDetails);
    }

    /**
     * Log system events
     */
    public void logSystemEvent(String action, AuditSeverity severity, Map<String, Object> details) {
        Map<String, Object> systemDetails = new HashMap<>(details != null ? details : new HashMap<>());
        systemDetails.put("event_type", "SYSTEM");
        systemDetails.put("timestamp", LocalDateTime.now());
        
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
                    .collect(java.util.stream.Collectors.toList());
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
                    .filter(log -> log.getDetails().get("event_type") != null &&
                            log.getDetails().get("event_type").toString().contains("SUSPICIOUS"))
                    .limit(limit)
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to get suspicious activities", e);
            return java.util.Collections.emptyList();
        }
    }
    
    /**
     * Log successful authentication
     */
    public void logSuccessfulAuthentication(HttpServletRequest request, String userId, String email) {
        logAuthenticationEvent(userId, "LOGIN_SUCCESS", AuditSeverity.INFO, true,
            java.util.Map.of("email", email, "method", "password"));
    }
    
    /**
     * Log authentication failure
     */
    public void logAuthenticationFailure(HttpServletRequest request, String action, String reason) {
        logAuthenticationEvent("unknown", action, AuditSeverity.WARNING, false,
            java.util.Map.of("reason", reason));
    }
    
    /**
     * Log password reset request
     */
    public void logPasswordResetRequest(String email, String ipAddress, boolean success) {
        logSecurityEvent(null, success ? "PASSWORD_RESET_SUCCESS" : "PASSWORD_RESET_REQUEST",
            "USER_SECURITY", email,
            success ? AuditSeverity.INFO : AuditSeverity.WARNING,
            java.util.Map.of("email", email, "ipAddress", ipAddress, "success", success));
    }

    // Private helper methods

    private String getClientIpAddress() {
        try {
            String xForwardedFor = httpRequest.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            String xRealIp = httpRequest.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
                return xRealIp;
            }
            
            return httpRequest.getRemoteAddr();
        } catch (Exception e) {
            logger.warn("Failed to get client IP address", e);
            return "unknown";
        }
    }

    private String getUserAgent() {
        try {
            return httpRequest.getHeader("User-Agent");
        } catch (Exception e) {
            logger.warn("Failed to get user agent", e);
            return "unknown";
        }
    }

    private void alertOnCriticalEvent(AuditLog auditLog) {
        // In a real implementation, this would send alerts to administrators
        // via email, Slack, PagerDuty, etc.
        securityLogger.error("CRITICAL SECURITY ALERT: {}", auditLog);
        
        // Could integrate with external alerting systems here
        // Example: AlertingService.sendAlert("CRITICAL_SECURITY_EVENT", auditLog);
    }

    private void alertOnSuspiciousActivity(String userId, String action, Map<String, Object> details) {
        // Check if this is part of a pattern (multiple failed attempts, etc.)
        // In a real implementation, this might trigger account lockout or additional monitoring
        
        securityLogger.warn("Suspicious Activity Alert: User {} performed action {} from IP {}", 
                userId, action, getClientIpAddress());
        
        // Could integrate with automated response systems here
        // Example: ResponseService.handleSuspiciousActivity(userId, details);
    }
}