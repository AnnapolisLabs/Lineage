package com.annapolislabs.lineage.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Custom CSRF validation filter for JWT-based REST API
 * Validates CSRF tokens for state-changing requests
 */
public class CsrfValidationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(CsrfValidationFilter.class);
    
    private static final String CSRF_TOKEN_HEADER = "X-CSRF-TOKEN";
    private static final String CSRF_TOKEN_ID_HEADER = "X-CSRF-TOKEN-ID";
    
    private final CsrfTokenService csrfTokenService;
    
    // Define which endpoints require CSRF validation
    private final RequestMatcher[] requireCsrfValidationMatchers = {
        new AntPathRequestMatcher("/api/projects/**"),
        new AntPathRequestMatcher("/api/requirements/**"),
        new AntPathRequestMatcher("/api/admin/**")
    };
    
    // Define which endpoints should bypass CSRF validation
    private final RequestMatcher[] bypassCsrfValidationMatchers = {
        new AntPathRequestMatcher("/api/csrf/**"),
        new AntPathRequestMatcher("/api/auth/**"),
        new AntPathRequestMatcher("/api/invitations/**"),
        new AntPathRequestMatcher("/api/security/**"),
        new AntPathRequestMatcher("/api/docs/**"),
        new AntPathRequestMatcher("/api/projects/test-import"),
        new AntPathRequestMatcher("/api/projects/import"),
        new AntPathRequestMatcher("/actuator/**")
    };
    
    public CsrfValidationFilter(CsrfTokenService csrfTokenService) {
        this.csrfTokenService = csrfTokenService;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        
        // Only validate for state-changing methods
        boolean isStateChanging = "POST".equalsIgnoreCase(method) || 
                                  "PUT".equalsIgnoreCase(method) || 
                                  "DELETE".equalsIgnoreCase(method) || 
                                  "PATCH".equalsIgnoreCase(method);
        
        if (!isStateChanging) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Check if this endpoint should bypass CSRF validation
        if (shouldBypassValidation(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Check if this endpoint requires CSRF validation
        if (!requiresCsrfValidation(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Extract CSRF token and token ID from headers
        String tokenValue = request.getHeader(CSRF_TOKEN_HEADER);
        String tokenId = request.getHeader(CSRF_TOKEN_ID_HEADER);
        
        if (tokenValue == null || tokenId == null) {
            logger.warn("CSRF token validation failed for {}: missing headers (token or tokenId)", requestUri);
            sendCsrfErrorResponse(response, "CSRF token or token ID missing");
            return;
        }
        
        logger.debug("Validating CSRF token for {} - Token ID: {}, Token length: {}", 
                    requestUri, tokenId, tokenValue.length());
        
        // Validate the token
        boolean isValid = csrfTokenService.validateToken(tokenValue, tokenId);
        
        if (isValid) {
            logger.debug("CSRF token validation successful for {}", requestUri);
            filterChain.doFilter(request, response);
        } else {
            logger.warn("CSRF token validation failed for {} with token ID: {}", requestUri, tokenId);
            sendCsrfErrorResponse(response, "Invalid CSRF token");
        }
    }
    
    private boolean shouldBypassValidation(HttpServletRequest request) {
        for (RequestMatcher matcher : bypassCsrfValidationMatchers) {
            if (matcher.matches(request)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean requiresCsrfValidation(HttpServletRequest request) {
        for (RequestMatcher matcher : requireCsrfValidationMatchers) {
            if (matcher.matches(request)) {
                return true;
            }
        }
        return false;
    }
    
    private void sendCsrfErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        
        String errorResponse = """
            {
                "error": {
                    "code": "CSRF_VALIDATION_FAILED",
                    "message": "%s",
                    "timestamp": "%s"
                }
            }
            """.formatted(message, java.time.Instant.now().toString());
        
        response.getWriter().write(errorResponse);
    }
}