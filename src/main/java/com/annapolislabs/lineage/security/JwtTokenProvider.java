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
 * Central token utility that issues and validates HMAC-signed JWT access and refresh tokens, embedding identity
 * metadata used by filters and services to enforce authorization.
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
        } else {
            key = secret != null && !secret.isBlank() ? 
                secret : 
                "development-secret-key-for-jwt-signing-change-in-production";
        }
        return Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getValidationKey() {
        // Same logic as signing key for validation
        return getSigningKey();
    }
    
    /**
     * Builds a short-lived access token embedding the caller's identity and authorities for API authorization.
     *
     * @param user authenticated actor whose claims drive the token contents.
     * @return signed JWT flagged as an {@link TokenType#ACCESS} token.
     */
    public String generateAccessToken(User user) {
        return generateToken(user, TokenType.ACCESS);
    }
    
    /**
     * Issues a refresh token so clients can rehydrate access tokens without re-authenticating credentials.
     *
     * @param user authenticated actor whose identifier seeds the refresh token.
     * @return signed JWT flagged as {@link TokenType#REFRESH}.
     */
    public String generateRefreshToken(User user) {
        return generateToken(user, TokenType.REFRESH);
    }
    
    /**
     * Creates both access and refresh tokens so responses can return a complete authentication bundle.
     *
     * @param user authenticated actor used to derive claims for both tokens.
     * @return paired token container with coordinated expiry windows.
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
     * Validates the signature and expiry of a JWT, optionally acting as the enforcement point for blacklist checks.
     *
     * @param token signed JWT presented by the client.
     * @return {@code true} when the token parses cleanly and has not expired.
     */
    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parser()
                    .verifyWith(getValidationKey())
                    .build()
                    .parseSignedClaims(token);
            
            // Check if token is blacklisted (optional enhancement)
            String jti = claims.getPayload().getId();
            // You would implement blacklist checking here if needed
            
            return !claims.getPayload().getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Parses and returns the claims payload, throwing a domain-specific exception when validation fails.
     *
     * @param token signed JWT expected to be valid.
     * @return decoded {@link Claims} payload for downstream use.
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
     * Reads the internal UUID stored inside the {@code user_id} claim for audit logging.
     *
     * @param token signed JWT string.
     * @return canonical user identifier extracted from claims.
     */
    public String getUserIdFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims.get("user_id", String.class);
    }
    
    /**
     * Returns the subject/email bound to the JWT for building {@link org.springframework.security.core.Authentication}.
     *
     * @param token signed JWT string.
     * @return subject value stored in the token.
     */
    public String getEmailFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims.getSubject();
    }
    
    /**
     * Resolves the global user role persisted at issuance time so services can gate features consistently.
     *
     * @param token signed JWT string.
     * @return {@link UserRole} enumerated from the {@code role} claim.
     */
    public UserRole getRoleFromToken(String token) {
        Claims claims = extractClaims(token);
        String roleName = claims.get("role", String.class);
        return UserRole.valueOf(roleName);
    }
    
    /**
     * Converts the expiration claim into a {@link LocalDateTime} for schedulers and audits.
     *
     * @param token signed JWT string.
     * @return token expiration expressed in the system zone.
     */
    public LocalDateTime getExpiryDateFromToken(String token) {
        Claims claims = extractClaims(token);
        Date expiration = claims.getExpiration();
        return LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault());
    }
    
    /**
     * Indicates whether the JWT is already expired, defaulting to {@code true} when parsing fails.
     *
     * @param token signed JWT string.
     * @return {@code true} once the expiration instant is in the past.
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
     * Reads the custom {@code type} claim to distinguish access tokens from refresh tokens.
     *
     * @param token signed JWT string.
     * @return {@link TokenType} enum derived from the claim.
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