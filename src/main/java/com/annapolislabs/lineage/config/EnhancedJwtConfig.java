package com.annapolislabs.lineage.config;

import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.service.PermissionEvaluationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Enhanced JWT configuration with role and permission context
 * Provides comprehensive token management for the RBAC system
 */
@Slf4j
@Component
public class EnhancedJwtConfig {

    private final PermissionEvaluationService permissionEvaluationService;
    private final AtomicReference<SecretKey> cachedSigningKey = new AtomicReference<>();

    @Value("${app.jwt.secret:mySecretKey123456789012345678901234567890}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}") // 24 hours
    private int jwtExpirationMs;

    public EnhancedJwtConfig(PermissionEvaluationService permissionEvaluationService) {
        this.permissionEvaluationService = permissionEvaluationService;
    }

    private SecretKey getSigningKey() {
        SecretKey currentKey = cachedSigningKey.get();
        if (currentKey == null) {
            if (jwtSecret == null || jwtSecret.isBlank()) {
                throw new IllegalStateException("JWT secret must be configured");
            }
            SecretKey computed = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            if (cachedSigningKey.compareAndSet(null, computed)) {
                currentKey = computed;
            } else {
                currentKey = cachedSigningKey.get();
            }
        }
        return currentKey;
    }

    private Map<String, Object> buildBaseClaims(UUID userId, String email, UserRole role) {
        Map<String, Object> claims = new HashMap<>();
        if (userId != null) {
            claims.put("user_id", userId.toString());
        }
        claims.put("email", email);
        claims.put("role", role.name());
        claims.put("role_level", getRoleLevel(role));
        claims.put("is_admin", role == UserRole.ADMINISTRATOR || role == UserRole.OWNER);
        claims.put("token_type", "access");
        claims.put("token_version", "enhanced");
        claims.put("issued_at", new Date());

        Set<String> globalPermissions = permissionEvaluationService.getEffectivePermissions(userId);
        claims.put("global_permissions", new ArrayList<>(globalPermissions));

        return claims;
    }

    /**
     * Generate enhanced JWT token with role and permission context
     */
    public String generateTokenWithPermissions(UUID userId, String email, UserRole role) {
        Map<String, Object> claims = buildBaseClaims(userId, email, role);
        return createToken(claims, email);
    }

    /**
     * Generate token with scoped permissions for specific resource
     */
    public String generateScopedToken(UUID userId, String email, UserRole role, UUID resourceId) {
        Map<String, Object> claims = new HashMap<>(buildBaseClaims(userId, email, role));
        Set<String> scopedPermissions = permissionEvaluationService.getEffectivePermissions(userId, resourceId);
        claims.put("scoped_permissions", new ArrayList<>(scopedPermissions));
        claims.put("resource_id", resourceId != null ? resourceId.toString() : null);
        claims.put("token_type", "scoped_access");
        return createToken(claims, email);
    }

    /**
     * Validate enhanced JWT token
     */
    public boolean validateEnhancedToken(String token, UserDetails userDetails) {
        if (token == null || token.isBlank() || userDetails == null) {
            return false;
        }
        try {
            Claims claims = getClaimsFromToken(token);
            String email = claims.getSubject();
            if (!Objects.equals(email, userDetails.getUsername())) {
                return false;
            }

            String userId = claims.get("user_id", String.class);
            String role = claims.get("role", String.class);
            if (userId == null || userId.isBlank() || role == null || role.isBlank()) {
                log.warn("Token missing required claims for subject {}", email);
                return false;
            }

            return !isTokenExpired(claims);
        } catch (IllegalStateException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract permissions from token
     */
    public List<String> extractPermissions(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return extractStringListClaim(claims, "global_permissions");
        } catch (Exception e) {
            log.error("Failed to extract permissions from token: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Extract role from token
     */
    public String extractRole(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.get("role", String.class);
        } catch (Exception e) {
            log.error("Failed to extract role from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract scoped permissions from token
     */
    public List<String> extractScopedPermissions(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return extractStringListClaim(claims, "scoped_permissions");
        } catch (Exception e) {
            log.error("Failed to extract scoped permissions from token: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> extractStringListClaim(Claims claims, String key) {
        Object value = claims.get(key);
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Check if token contains specific permission
     */
    public boolean hasPermission(String token, String permission) {
        List<String> permissions = extractPermissions(token);
        return permissions.contains(permission);
    }

    /**
     * Check if token has role or higher
     */
    public boolean hasRoleOrHigher(String token, UserRole requiredRole) {
        try {
            Claims claims = getClaimsFromToken(token);
            String userRoleStr = claims.get("role", String.class);
            if (userRoleStr == null) {
                return false;
            }
            UserRole userRole = UserRole.valueOf(userRoleStr);
            return getRoleLevel(userRole) >= getRoleLevel(requiredRole);
        } catch (Exception e) {
            log.error("Failed to check role hierarchy: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Refresh token with updated permissions
     */
    public String refreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String email = claims.getSubject();
            String userId = claims.get("user_id", String.class);
            String role = claims.get("role", String.class);
            if (email == null || userId == null || role == null) {
                throw new IllegalStateException("Token missing required claims for refresh");
            }

            return generateTokenWithPermissions(
                    UUID.fromString(userId),
                    email,
                    UserRole.valueOf(role)
            );
        } catch (Exception e) {
            log.error("Failed to refresh token: {}", e.getMessage());
            throw new RuntimeException("Token refresh failed", e);
        }
    }

    private Claims getClaimsFromToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Token must not be null or blank");
        }
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalStateException("Unable to parse token", e);
        }
    }

    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExpirationMs);
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration == null || expiration.before(new Date());
    }

    public Map<String, Object> parseClaims(String token) {
        Claims claims = getClaimsFromToken(token);
        Map<String, Object> claimsMap = new LinkedHashMap<>();
        claims.forEach(claimsMap::put);
        claimsMap.put("subject", claims.getSubject());
        claimsMap.put("issued_at", claims.getIssuedAt());
        claimsMap.put("expires_at", claims.getExpiration());
        return claimsMap;
    }

    private int getRoleLevel(UserRole role) {
        if (role == null) {
            return 0;
        }
        return switch (role) {
            case OWNER -> 3;
            case ADMINISTRATOR -> 2;
            case PROJECT_MANAGER -> 2;
            case DEVELOPER -> 1;
            case USER -> 1;
        };
    }

    /**
     * Get token metadata for monitoring
     */
    public Map<String, Object> getTokenMetadata(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Map<String, Object> metadata = new HashMap<>();

            metadata.put("user_id", claims.get("user_id"));
            metadata.put("role", claims.get("role"));
            metadata.put("issued_at", claims.getIssuedAt());
            metadata.put("expires_at", claims.getExpiration());
            metadata.put("is_admin", claims.get("is_admin", Boolean.class));

            Object permissions = claims.get("global_permissions");
            if (permissions instanceof Collection<?> collection) {
                metadata.put("permission_count", collection.size());
            }

            return metadata;
        } catch (Exception e) {
            log.error("Failed to extract token metadata: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
