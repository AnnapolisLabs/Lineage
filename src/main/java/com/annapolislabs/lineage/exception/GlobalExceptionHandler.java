package com.annapolislabs.lineage.exception;

import com.annapolislabs.lineage.exception.auth.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for consistent error responses across the application
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle authentication exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        
        logger.warn("Authentication error: {} - Path: {} - IP: {}", 
                   ex.getMessage(), request.getRequestURI(), getClientIpAddress(request));
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            HttpStatus.UNAUTHORIZED.value(),
            LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    /**
     * Handle MFA verification exceptions
     */
    @ExceptionHandler(MfaVerificationException.class)
    public ResponseEntity<ErrorResponse> handleMfaVerificationException(
            MfaVerificationException ex, HttpServletRequest request) {
        
        logger.warn("MFA verification failed: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "MFA_VERIFICATION_FAILED",
            ex.getMessage(),
            request.getRequestURI(),
            HttpStatus.UNAUTHORIZED.value(),
            LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    /**
     * Handle token expiration exceptions
     */
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpiredException(
            TokenExpiredException ex, HttpServletRequest request) {
        
        logger.warn("Token expired: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "TOKEN_EXPIRED",
            "Your session has expired. Please log in again.",
            request.getRequestURI(),
            HttpStatus.UNAUTHORIZED.value(),
            LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    /**
     * Handle invalid token exceptions
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTokenException(
            InvalidTokenException ex, HttpServletRequest request) {
        
        logger.warn("Invalid token: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_TOKEN",
            "Invalid authentication token provided.",
            request.getRequestURI(),
            HttpStatus.UNAUTHORIZED.value(),
            LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    /**
     * Handle account lockout exceptions
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLockedException(
            AccountLockedException ex, HttpServletRequest request) {
        
        logger.warn("Account locked: {} - Duration: {} minutes - Path: {}", 
                   ex.getMessage(), ex.getLockoutDurationMinutes(), request.getRequestURI());
        
        Map<String, Object> details = new HashMap<>();
        details.put("lockoutDurationMinutes", ex.getLockoutDurationMinutes());
        details.put("retryAfter", ex.getLockoutDurationMinutes() * 60); // in seconds
        
        ErrorResponse errorResponse = new ErrorResponse(
            "ACCOUNT_LOCKED",
            ex.getMessage(),
            request.getRequestURI(),
            HttpStatus.TOO_MANY_REQUESTS.value(),
            LocalDateTime.now(),
            details
        );
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }
    
    /**
     * Handle access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        logger.warn("Access denied: {} - Path: {} - IP: {}", 
                   ex.getMessage(), request.getRequestURI(), getClientIpAddress(request));
        
        ErrorResponse errorResponse = new ErrorResponse(
            "ACCESS_DENIED",
            "You do not have permission to access this resource.",
            request.getRequestURI(),
            HttpStatus.FORBIDDEN.value(),
            LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    /**
     * Handle bad credentials exceptions
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {
        
        logger.warn("Bad credentials: {} - Path: {} - IP: {}", 
                   ex.getMessage(), request.getRequestURI(), getClientIpAddress(request));
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_CREDENTIALS",
            "Invalid email or password.",
            request.getRequestURI(),
            HttpStatus.UNAUTHORIZED.value(),
            LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        logger.warn("Validation error: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        
        Map<String, Object> details = new HashMap<>();
        details.put("validationErrors", validationErrors);
        details.put("errorCount", validationErrors.size());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed. Please check your input.",
            request.getRequestURI(),
            HttpStatus.BAD_REQUEST.value(),
            LocalDateTime.now(),
            details
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        logger.warn("Illegal argument: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_ARGUMENT",
            ex.getMessage(),
            request.getRequestURI(),
            HttpStatus.BAD_REQUEST.value(),
            LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle generic runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        
        logger.error("Runtime error: {} - Path: {} - IP: {}", 
                    ex.getMessage(), request.getRequestURI(), getClientIpAddress(request), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_ERROR",
            "An internal error occurred. Please try again later.",
            request.getRequestURI(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        logger.error("Unexpected error: {} - Path: {} - IP: {}", 
                    ex.getMessage(), request.getRequestURI(), getClientIpAddress(request), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred. Please contact support.",
            request.getRequestURI(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            LocalDateTime.now()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Standard error response structure
     */
    public static class ErrorResponse {
        private String errorCode;
        private String message;
        private String path;
        private int status;
        private LocalDateTime timestamp;
        private Map<String, Object> details;
        
        public ErrorResponse() {}
        
        public ErrorResponse(String errorCode, String message, String path, int status, LocalDateTime timestamp) {
            this.errorCode = errorCode;
            this.message = message;
            this.path = path;
            this.status = status;
            this.timestamp = timestamp;
        }
        
        public ErrorResponse(String errorCode, String message, String path, int status, LocalDateTime timestamp, Map<String, Object> details) {
            this(errorCode, message, path, status, timestamp);
            this.details = details;
        }
        
        // Getters and setters
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }
    
    /**
     * Get client IP address from request
     */
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