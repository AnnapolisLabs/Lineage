package com.annapolislabs.lineage.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JWT Authentication Entry Point that handles unauthorized access attempts
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
    
    private static final String UNAUTHORIZED_MESSAGE = "Full authentication is required to access this resource";
    private static final String INVALID_TOKEN_MESSAGE = "Invalid or expired authentication token";
    private static final String NO_TOKEN_MESSAGE = "No authentication token provided";
    
    @Override
    public void commence(HttpServletRequest request, 
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        
        logger.warn("Unauthorized access attempt to: {} from IP: {}, User-Agent: {}", 
                   request.getRequestURI(), 
                   getClientIpAddress(request), 
                   request.getHeader("User-Agent"));
        
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        String message = determineErrorMessage(request, authException);
        String errorCode = determineErrorCode(request, authException);
        
        String jsonResponse = String.format("""
            {
                "error": {
                    "code": "%s",
                    "message": "%s",
                    "path": "%s",
                    "timestamp": "%s"
                }
            }
            """, 
            errorCode,
            escapeJson(message),
            escapeJson(request.getRequestURI()),
            java.time.Instant.now().toString()
        );
        
        response.getWriter().write(jsonResponse);
        
        // Log security event
        logSecurityEvent(request, errorCode, message);
    }
    
    private String determineErrorMessage(HttpServletRequest request, AuthenticationException authException) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || authHeader.isEmpty()) {
            return NO_TOKEN_MESSAGE;
        } else if (authException.getMessage().contains("Expired")) {
            return INVALID_TOKEN_MESSAGE;
        } else if (authException.getMessage().contains("Invalid")) {
            return INVALID_TOKEN_MESSAGE;
        } else {
            return UNAUTHORIZED_MESSAGE;
        }
    }
    
    private String determineErrorCode(HttpServletRequest request, AuthenticationException authException) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || authHeader.isEmpty()) {
            return "NO_TOKEN";
        } else if (authException.getMessage().contains("Expired")) {
            return "TOKEN_EXPIRED";
        } else if (authException.getMessage().contains("Invalid")) {
            return "TOKEN_INVALID";
        } else {
            return "UNAUTHORIZED";
        }
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
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
    
    private void logSecurityEvent(HttpServletRequest request, String errorCode, String message) {
        logger.warn("Security Event - {}: {} - URI: {} - IP: {} - User-Agent: {}", 
                   errorCode, 
                   message, 
                   request.getRequestURI(), 
                   getClientIpAddress(request), 
                   request.getHeader("User-Agent"));
    }
}