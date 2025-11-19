package com.annapolislabs.lineage.security;

import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JWT Token Provider for generating and validating JWT tokens with RS256 signature
 */
@Component
public class JwtTokenProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    
    @Value("${jwt.access.token.expiry:900}") // 15 minutes
    private long accessTokenExpiry;
    
    @Value("${jwt.refresh.token.expiry:604800}") // 7 days
    private long refreshTokenExpiry;
    
    @Value("${jwt.secret.key:#{null}}")
    private String secretKey;

    @Value("${jwt.secret:#{null}}")
    private String secret;
    
    private SecretKey getSigningKey() {
        // Use the same fallback secret as JwtUtil to ensure consistency
        String key;
        if (secretKey != null && !secretKey.isBlank()) {
            key = secretKey;
        } else if (secret != null && !secret.isBlank()) {
            key = secret;
        } else {
            key = "development-secret-key-for-jwt-signing-change-in-production";
        }
        return Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getValidationKey() {
        // Same logic as signing key for validation
        return getSigningKey();
    }
    
    /**
     * Generate access token for user
     */
    public String generateAccessToken(User user) {
        return generateToken(user, TokenType.ACCESS);
    }
    
    /**
     * Generate refresh token for user
     */
    public String generateRefreshToken(User user) {
        return generateToken(user, TokenType.REFRESH);
    }
    
    /**
     * Generate both access and refresh tokens
     */
    public TokenPair generateTokenPair(User user) {
        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);
        return new TokenPair(accessToken, refreshToken);
    }
    
    private String generateToken(User user, TokenType tokenType) {
        long expiryTime = tokenType == TokenType.ACCESS ? accessTokenExpiry : refreshTokenExpiry;
        Date expiryDate = new Date(System.currentTimeMillis() + expiryTime * 1000);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", tokenType.name().toLowerCase());
        claims.put("user_id", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("role", user.getGlobalRole().name());
        claims.put("authorities", getAuthorities(user));
        
        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parser()
                    .verifyWith(getValidationKey())
                    .build()
                    .parseSignedClaims(token);
            
            // Check if token is blacklisted (optional enhancement)
            claims.getPayload().getId();
            // You would implement blacklist checking here if needed
            
            return !claims.getPayload().getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Extract claims from token
     */
    public Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getValidationKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Failed to extract JWT claims: {}", e.getMessage());
            throw new JwtTokenException("Invalid or expired token");
        }
    }
    
    /**
     * Extract user ID from token
     */
    public String getUserIdFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims.get("user_id", String.class);
    }
    
    /**
     * Extract email from token
     */
    public String getEmailFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims.getSubject();
    }
    
    /**
     * Extract role from token
     */
    public UserRole getRoleFromToken(String token) {
        Claims claims = extractClaims(token);
        String roleName = claims.get("role", String.class);
        return UserRole.valueOf(roleName);
    }
    
    /**
     * Get token expiry date
     */
    public LocalDateTime getExpiryDateFromToken(String token) {
        Claims claims = extractClaims(token);
        Date expiration = claims.getExpiration();
        return LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault());
    }
    
    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Get token type from token
     */
    public TokenType getTokenType(String token) {
        Claims claims = extractClaims(token);
        String type = claims.get("type", String.class);
        return TokenType.valueOf(type.toUpperCase());
    }
    
    private List<String> getAuthorities(User user) {
        return List.of("ROLE_" + user.getGlobalRole().name());
    }
    
    /**
     * Token type enumeration
     */
    public enum TokenType {
        ACCESS, REFRESH
    }
    
    /**
     * Token pair container
     */
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;
        
        public TokenPair(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
        
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
    }
    
    /**
     * Custom JWT exception
     */
    public static class JwtTokenException extends RuntimeException {
        public JwtTokenException(String message) {
            super(message);
        }
    }
}