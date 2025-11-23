package com.annapolislabs.lineage.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Issues stateless CSRF tokens for SPA/JWT flows, caching them in-memory until the paired header is validated and
 * discarded to keep replay windows narrow.
 */
@Service
public class CsrfTokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(CsrfTokenService.class);
    private static final String CSRF_TOKEN_HEADER = "X-CSRF-TOKEN";
    private static final String CSRF_TOKEN_PARAMETER = "_csrf";
    private static final SecureRandom random = new SecureRandom();
    
    // Store active CSRF tokens by session/request identifier
    private final Map<String, String> activeTokens = new ConcurrentHashMap<>();
    
    // Track the last generated token ID for retrieval
    private volatile String lastGeneratedTokenId;
    
    /**
     * Generates a new CSRF token/ID pair and caches it for one-time validation.
     *
     * @return Spring Security token wrapper containing the header metadata and opaque value.
     */
    public CsrfToken generateToken() {
        String tokenValue = generateSecureToken();
        String tokenId = generateTokenId();
        
        // Store the token for validation
        activeTokens.put(tokenId, tokenValue);
        
        // Track the last generated token ID
        lastGeneratedTokenId = tokenId;
        
        DefaultCsrfToken token = new DefaultCsrfToken(
            CSRF_TOKEN_HEADER,
            CSRF_TOKEN_PARAMETER,
            tokenValue
        );
        
        logger.debug("Generated new CSRF token with ID: {}", tokenId);
        return token;
    }
    /**
     * @return identifier associated with the most recently minted token so callers can pair it with the header value.
     */
    public String getLastGeneratedTokenId() {
        return lastGeneratedTokenId;
    }
    
    /**
     * Validates the provided token value against the cached entry for the identifier and removes it when matched to
     * enforce single use semantics.
     *
     * @param tokenValue Base64 string provided by the client.
     * @param tokenId identifier used as the lookup key.
     * @return {@code true} when validation succeeds.
     */
    public boolean validateToken(String tokenValue, String tokenId) {
        if (tokenValue == null || tokenId == null) {
            logger.debug("CSRF token validation failed: missing token value or ID");
            return false;
        }
        
        String storedToken = activeTokens.get(tokenId);
        boolean isValid = storedToken != null && storedToken.equals(tokenValue);
        
        if (isValid) {
            logger.debug("CSRF token validation successful for token ID: {}", tokenId);
            // Remove used token to prevent replay attacks
            activeTokens.remove(tokenId);
        } else {
            logger.warn("CSRF token validation failed for token ID: {}", tokenId);
        }
        
        return isValid;
    }
    
     /**
     * Writes the CSRF value into the response header so front-end clients can store it and attach on protected calls.
     *
     * @param response outgoing response.
     * @param token CSRF token to expose; ignored when {@code null}.
     */
    public void addTokenToResponse(HttpServletResponse response, CsrfToken token) {
        if (token != null) {
            response.setHeader(token.getHeaderName(), token.getToken());
            logger.debug("Added CSRF token header to response: {}", token.getHeaderName());
        }
    }
    
     /**
     * Performs opportunistic cleanup when the token cache grows beyond a safe threshold to avoid memory leaks.
     */
    public void cleanupExpiredTokens() {
        // In a real implementation, you might want to implement token expiration
        // For now, we'll implement a simple cleanup strategy
        if (activeTokens.size() > 1000) { // Prevent memory leaks
            activeTokens.clear();
            logger.info("Cleaned up CSRF tokens due to size limit");
        }
    }
    
    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    private String generateTokenId() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}