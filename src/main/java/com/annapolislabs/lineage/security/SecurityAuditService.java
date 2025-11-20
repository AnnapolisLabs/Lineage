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
    }

    private void alertOnSuspiciousActivity(String userId, String action) {
        // Check if this is part of a pattern (multiple failed attempts, etc.)
        // In a real implementation, this might trigger account lockout or additional monitoring
        
        securityLogger.warn("Suspicious Activity Alert: User {} performed action {} from IP {}", 
                userId, action, getClientIpAddress());
        
        // Could integrate with automated response systems here
        // Example: ResponseService.handleSuspiciousActivity(userId);
    }
}