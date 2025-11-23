package com.annapolislabs.lineage.config;

import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.service.PermissionEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnhancedJwtConfigTest {

    private PermissionEvaluationService permissionEvaluationService;
    private EnhancedJwtConfig enhancedJwtConfig;

    private UUID userId;
    private UUID resourceId;
    private final String email = "security@example.com";

    @BeforeEach
    void setUp() {
        permissionEvaluationService = mock(PermissionEvaluationService.class);
        enhancedJwtConfig = new EnhancedJwtConfig(permissionEvaluationService);

        ReflectionTestUtils.setField(enhancedJwtConfig, "jwtSecret", "unit-test-secret-key-should-be-very-long-1234567890");
        ReflectionTestUtils.setField(enhancedJwtConfig, "jwtExpirationMs", 3_600_000);

        userId = UUID.randomUUID();
        resourceId = UUID.randomUUID();

        when(permissionEvaluationService.getEffectivePermissions(eq(userId)))
                .thenReturn(Set.of("requirement.read", "project.update"));
        when(permissionEvaluationService.getEffectivePermissions(eq(userId), eq(resourceId)))
                .thenReturn(Set.of("resource.scope.read"));
    }

    @Test
    void generateTokenWithPermissionsShouldEmbedClaimsAndValidate() {
        String token = enhancedJwtConfig.generateTokenWithPermissions(userId, email, UserRole.ADMINISTRATOR);
        UserDetails userDetails = new User(email, "password", Collections.emptyList());

        assertTrue(enhancedJwtConfig.validateEnhancedToken(token, userDetails));

        List<String> permissions = enhancedJwtConfig.extractPermissions(token);
        assertTrue(permissions.contains("project.update"));
        assertEquals(UserRole.ADMINISTRATOR.name(), enhancedJwtConfig.extractRole(token));
    }

    @Test
    void generateScopedTokenShouldEmbedResourceClaims() {
        String token = enhancedJwtConfig.generateScopedToken(userId, email, UserRole.DEVELOPER, resourceId);

        List<String> scopedPermissions = enhancedJwtConfig.extractScopedPermissions(token);
        assertEquals(Set.of("resource.scope.read"), Set.copyOf(scopedPermissions));

        Map<String, Object> claimMap = enhancedJwtConfig.parseClaims(token);
        assertEquals(resourceId.toString(), claimMap.get("resource_id"));
        assertEquals("scoped_access", claimMap.get("token_type"));
    }

    @Test
    void parseClaimsShouldExposeReadableMap() {
        String token = enhancedJwtConfig.generateTokenWithPermissions(userId, email, UserRole.USER);

        Map<String, Object> claims = enhancedJwtConfig.parseClaims(token);
        assertEquals(email, claims.get("subject"));
        assertEquals(userId.toString(), claims.get("user_id"));
        assertNotNull(claims.get("expires_at"));
    }

    @Test
    void validateEnhancedTokenShouldRejectDifferentUser() {
        String token = enhancedJwtConfig.generateTokenWithPermissions(userId, email, UserRole.USER);
        UserDetails otherUser = new User("other@example.com", "password", Collections.emptyList());

        assertFalse(enhancedJwtConfig.validateEnhancedToken(token, otherUser));
    }
}
