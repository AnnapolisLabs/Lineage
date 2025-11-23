package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.security.CsrfTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller that surfaces CSRF token lifecycle endpoints for SPA clients secured by JWT.
 */
@RestController
@RequestMapping("/api/csrf")
public class CsrfTokenController {
    
    private static final Logger logger = LoggerFactory.getLogger(CsrfTokenController.class);
    
    private final CsrfTokenService csrfTokenService;
    
    @Autowired
    public CsrfTokenController(CsrfTokenService csrfTokenService) {
        this.csrfTokenService = csrfTokenService;
    }
    
    /**
     * GET /api/csrf/token issues a CSRF token/tokenId pair, writes headers, and returns metadata for the caller.
     * Returns 200 OK when the token is created; unexpected failures return 500.
     *
     * @param request current request used for logging
     * @param response response used to append CSRF headers
     * @return 200 OK containing token value, id, and header names
     */
    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> generateToken(
            HttpServletRequest request, 
            HttpServletResponse response) {
        
        try {
            // Generate token and capture both token value and tokenId
            CsrfToken token = csrfTokenService.generateToken();
            String tokenId = csrfTokenService.getLastGeneratedTokenId();
            
            // Add token to response headers
            csrfTokenService.addTokenToResponse(response, token);
            
            logger.debug("Generated CSRF token for request to {} with ID: {}", request.getRequestURI(), tokenId);
            
            // Return both token and tokenId in response body for convenience
            return ResponseEntity.ok(Map.of(
                "token", token.getToken(),
                "tokenId", tokenId,
                "headerName", token.getHeaderName(),
                "parameterName", token.getParameterName()
            ));
            
        } catch (Exception e) {
            logger.error("Failed to generate CSRF token: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to generate CSRF token"));
        }
    }
    
    /**
     * POST /api/csrf/validate checks whether the supplied token/tokenId pair matches the server record.
     * Returns 200 OK with a boolean result; 400 when required fields are missing.
     *
     * @param tokenData map containing "token" and "tokenId"
     * @param request used to record the request URI for diagnostics
     * @return JSON body describing validation result
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(
            @RequestBody Map<String, String> tokenData,
            HttpServletRequest request) {
        
        String tokenValue = tokenData.get("token");
        String tokenId = tokenData.get("tokenId");
        
        if (tokenValue == null || tokenId == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("valid", false, "error", "Missing token or tokenId"));
        }
        
        boolean isValid = csrfTokenService.validateToken(tokenValue, tokenId);
        
        logger.debug("CSRF token validation for request to {}: {}", 
            request.getRequestURI(), isValid ? "SUCCESS" : "FAILED");
        
        return ResponseEntity.ok(Map.of(
            "valid", isValid,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * GET /api/csrf/health exposes a lightweight readiness probe for monitoring the CSRF service.
     *
     * @return 200 OK with status metadata
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "CSRF Token Service",
            "timestamp", System.currentTimeMillis()
        ));
    }
}