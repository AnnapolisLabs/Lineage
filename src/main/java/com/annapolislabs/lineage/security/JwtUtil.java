package com.annapolislabs.lineage.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility component for generating lightweight JWTs used in legacy flows and validating usernames against token
 * claims with the same symmetric secret as {@link JwtTokenProvider}.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret.key:#{null}}")
    private String secretKey;

    @Value("${jwt.secret:#{null}}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    private SecretKey getSigningKey() {
        // Use the same fallback secret as JwtTokenProvider to ensure consistency
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

    /**
     * Retrieves the subject (email/username) embedded in the JWT so downstream filters can resolve principals.
     *
     * @param token signed JWT extracted from the client.
     * @return subject value stored in the token's {@code sub} claim.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Reads the expiration instant from the token for callers that need to pre-expire sessions.
     *
     * @param token signed JWT extracted from the client.
     * @return expiration {@link Date} declared in the token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Allows callers to project arbitrary claims from the JWT payload while reusing the shared parsing logic.
     *
     * @param token signed JWT extracted from the client.
     * @param claimsResolver function that maps {@link Claims} to the desired type.
     * @param <T> resolved claim type.
     * @return extracted claim value supplied by {@code claimsResolver}.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Issues a short-lived token for legacy contexts that only need a subject without extended claims.
     *
     * @param username principal stored inside the {@code sub} claim.
     * @return signed JWT string suitable for header transmission.
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Confirms that the provided JWT belongs to the supplied {@link UserDetails} and is still within its lifetime.
     *
     * @param token JWT string from the client.
     * @param userDetails spring-security user representation to compare against the token subject.
     * @return {@code true} when the username matches and the token is not expired.
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
