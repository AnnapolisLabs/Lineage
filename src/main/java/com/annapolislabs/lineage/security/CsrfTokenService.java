package com.annapolislabs.lineage.security;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Iterator;
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

    /**
     * Maximum lifetime for a CSRF token in milliseconds. Tokens older than this
     * are treated as expired and rejected during validation. This TTL is
     * intentionally short-lived and should roughly align with the authenticated
     * session window.
     */
    private static final long TOKEN_TTL_MILLIS = 30 * 60 * 1000L; // 30 minutes

    /**
     * Value and creation timestamp for a single-use CSRF token. Storing the
     * timestamp locally lets us enforce a hard upper bound on token lifetime
     * even if the client never uses or refreshes it.
     */
    private static final class StoredToken {
        final String value;
        final long createdAtMillis;

        StoredToken(String value, long createdAtMillis) {
            this.value = value;
            this.createdAtMillis = createdAtMillis;
        }
    }

    // Store active CSRF tokens by identifier (tokenId -> StoredToken)
    private final Map<String, StoredToken> activeTokens = new ConcurrentHashMap<>();
    
    /**
     * Container for the generated token and its corresponding identifier so
     * callers can safely obtain both values without relying on shared mutable
     * state such as {@code lastGeneratedTokenId}. Returning this pair removes
     * the race condition where concurrent callers could observe the wrong id
     * for a given token value.
     */
    public record CsrfTokenPair(CsrfToken token, String tokenId) {}

    /**
     * Generates a new CSRF token/ID pair and caches it for one-time validation.
     *
     * @return token/value pair containing the Spring Security wrapper and the
     * associated opaque identifier.
     */
    public CsrfTokenPair generateTokenPair() {
        String tokenValue = generateSecureToken();
        String tokenId = generateTokenId();

        // Store the token for validation with its creation time
        activeTokens.put(tokenId, new StoredToken(tokenValue, System.currentTimeMillis()));

        DefaultCsrfToken token = new DefaultCsrfToken(
            CSRF_TOKEN_HEADER,
            CSRF_TOKEN_PARAMETER,
            tokenValue
        );

        logger.debug("Generated new CSRF token with ID: {}", tokenId);
        return new CsrfTokenPair(token, tokenId);
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
        
        StoredToken stored = activeTokens.get(tokenId);

        if (stored == null) {
            logger.warn("CSRF token validation failed for token ID: {} (no entry)", tokenId);
            return false;
        }

        long ageMillis = System.currentTimeMillis() - stored.createdAtMillis;
        if (ageMillis > TOKEN_TTL_MILLIS) {
            // Drop expired token and treat as invalid
            activeTokens.remove(tokenId);
            logger.warn("CSRF token validation failed for token ID: {} (expired after {} ms)", tokenId, ageMillis);
            return false;
        }

        boolean isValid = stored.value.equals(tokenValue);
        
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
     * Performs opportunistic cleanup when the token cache grows beyond a safe threshold to avoid memory leaks. This
     * method also eagerly removes entries that have exceeded their TTL, even if the cache size is below the
     * threshold, to keep the in-memory store healthy over time.
     */
    public void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();

        // Remove explicit expirations first
        Iterator<Map.Entry<String, StoredToken>> iterator = activeTokens.entrySet().iterator();
        int expiredCount = 0;

        while (iterator.hasNext()) {
            Map.Entry<String, StoredToken> entry = iterator.next();
            if (now - entry.getValue().createdAtMillis > TOKEN_TTL_MILLIS) {
                iterator.remove();
                expiredCount++;
            }
        }

        if (expiredCount > 0) {
            logger.debug("Cleaned up {} expired CSRF tokens", expiredCount);
        }

        // Safety valve: if we still have an unexpectedly large map, clear it
        if (activeTokens.size() > 1000) { // Prevent memory leaks in pathological cases
            int sizeBeforeClear = activeTokens.size();
            activeTokens.clear();
            logger.info("Cleaned up CSRF tokens due to size limit ({} -> 0)", sizeBeforeClear);
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