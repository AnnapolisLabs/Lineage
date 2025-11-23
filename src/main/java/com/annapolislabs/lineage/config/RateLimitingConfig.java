package com.annapolislabs.lineage.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting configuration for collaboration endpoints
 * Implements token bucket algorithm for granular rate control
 */
@Slf4j
@Component
public class RateLimitingConfig {

    // Rate limits per endpoint category
    private static final int STANDARD_ENDPOINT_LIMIT = 1000; // requests per hour
    private static final int PERMISSION_EVALUATION_LIMIT = 10000; // requests per hour
    private static final int BULK_OPERATIONS_LIMIT = 100; // requests per hour
    private static final int ADMIN_ENDPOINTS_LIMIT = 500; // requests per hour

    // In-memory rate limiting storage (in production, use Redis)
    private final ConcurrentHashMap<String, RateLimitBucket> rateLimitBuckets = new ConcurrentHashMap<>();

    @Value("${app.rate-limiting.enabled:true}")
    private boolean rateLimitingEnabled;

    /**
     * Check if request should be rate limited
     */
    public RateLimitResult checkRateLimit(String userId, String endpointCategory) {
        if (!rateLimitingEnabled) {
            return RateLimitResult.ALLOWED;
        }

        String key = userId + ":" + endpointCategory;
        RateLimitBucket bucket = rateLimitBuckets.computeIfAbsent(key, k -> new RateLimitBucket());
        
        return bucket.consume(getLimitForCategory(endpointCategory));
    }

    /**
     * Get remaining requests for user in current window
     */
    public int getRemainingRequests(String userId, String endpointCategory) {
        String key = userId + ":" + endpointCategory;
        RateLimitBucket bucket = rateLimitBuckets.get(key);
        if (bucket == null) {
            return getLimitForCategory(endpointCategory);
        }
        
        return bucket.getRemainingTokens();
    }

    /**
     * Get rate limit reset time for user
     */
    public long getResetTime(String userId, String endpointCategory) {
        String key = userId + ":" + endpointCategory;
        RateLimitBucket bucket = rateLimitBuckets.get(key);
        if (bucket == null) {
            return System.currentTimeMillis();
        }
        
        return bucket.getResetTime();
    }

    private int getLimitForCategory(String category) {
        return switch (category.toLowerCase()) {
            case "permission" -> PERMISSION_EVALUATION_LIMIT;
            case "bulk" -> BULK_OPERATIONS_LIMIT;
            case "admin" -> ADMIN_ENDPOINTS_LIMIT;
            default -> STANDARD_ENDPOINT_LIMIT;
        };
    }

    /**
     * Rate limit bucket implementation (Token Bucket Algorithm)
     */
    private static class RateLimitBucket {
        private final AtomicInteger tokens = new AtomicInteger();
        private volatile long resetTime;
        
        public RateLimitBucket() {
            this.tokens.set(getInitialTokens());
            this.resetTime = System.currentTimeMillis() + 3600000; // 1 hour
        }

        public RateLimitResult consume(int limit) {
            long now = System.currentTimeMillis();
            
            // Reset bucket if time window has passed
            if (now > resetTime) {
                tokens.set(getInitialTokens());
                resetTime = now + 3600000;
            }
            
            // Try to consume a token
            int currentTokens = tokens.get();
            while (currentTokens > 0) {
                if (tokens.compareAndSet(currentTokens, currentTokens - 1)) {
                    return RateLimitResult.ALLOWED;
                }
                currentTokens = tokens.get();
            }
            
            // Rate limit exceeded
            return RateLimitResult.RATE_LIMITED;
        }

        public int getRemainingTokens() {
            long now = System.currentTimeMillis();
            if (now > resetTime) {
                return getInitialTokens();
            }
            return tokens.get();
        }

        public long getResetTime() {
            return resetTime;
        }

        private int getInitialTokens() {
            return 100; // Token bucket size
        }
    }

    /**
     * Rate limit result
     */
    public static class RateLimitResult {
        public static final RateLimitResult ALLOWED = new RateLimitResult(true, "allowed", 0);
        public static final RateLimitResult RATE_LIMITED = new RateLimitResult(false, "rate_limited", 3600000);
        
        private final boolean allowed;
        private final String reason;
        private final long retryAfterMs;

        private RateLimitResult(boolean allowed, String reason, long retryAfterMs) {
            this.allowed = allowed;
            this.reason = reason;
            this.retryAfterMs = retryAfterMs;
        }

        public boolean isAllowed() { return allowed; }
        public String getReason() { return reason; }
        public long getRetryAfterMs() { return retryAfterMs; }
    }
}