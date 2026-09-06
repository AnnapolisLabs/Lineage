package com.annapolislabs.lineage.security;

import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secret", "test-secret-key-for-jwt-must-be-at-least-256-bits-long");
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiry", 900L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpiry", 604800L);

        testUser = new User("test@example.com", "hashed-password", "Test", "User", UserRole.USER);
        testUser.setId(UUID.randomUUID());
    }

    @Test
    void validateSecretConfigured_throwsWhenSecretMissing() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secret", null);

        assertThrows(IllegalStateException.class, provider::validateSecretConfigured);
    }

    @Test
    void validateSecretConfigured_throwsWhenSecretTooShort() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secret", "too-short");

        assertThrows(IllegalStateException.class, provider::validateSecretConfigured);
    }

    @Test
    void validateSecretConfigured_passesWithValidSecret() {
        assertDoesNotThrow(jwtTokenProvider::validateSecretConfigured);
    }

    @Test
    void generateAccessToken_producesValidToken() {
        String token = jwtTokenProvider.generateAccessToken(testUser);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void generateTokenPair_producesDistinctAccessAndRefreshTokens() {
        JwtTokenProvider.TokenPair tokenPair = jwtTokenProvider.generateTokenPair(testUser);

        assertNotNull(tokenPair.getAccessToken());
        assertNotNull(tokenPair.getRefreshToken());
        assertNotEquals(tokenPair.getAccessToken(), tokenPair.getRefreshToken());
    }

    @Test
    void getEmailFromToken_returnsSubject() {
        String token = jwtTokenProvider.generateAccessToken(testUser);

        assertEquals("test@example.com", jwtTokenProvider.getEmailFromToken(token));
    }

    @Test
    void getRoleFromToken_returnsGlobalRole() {
        String token = jwtTokenProvider.generateAccessToken(testUser);

        assertEquals(UserRole.USER, jwtTokenProvider.getRoleFromToken(token));
    }

    @Test
    void validateToken_invalidTokenReturnsFalse() {
        assertFalse(jwtTokenProvider.validateToken("not-a-real-token"));
    }

    @Test
    void validateToken_tokenSignedWithDifferentSecretIsRejected() {
        JwtTokenProvider otherProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(otherProvider, "secret", "a-completely-different-secret-key-of-sufficient-length");
        ReflectionTestUtils.setField(otherProvider, "accessTokenExpiry", 900L);
        ReflectionTestUtils.setField(otherProvider, "refreshTokenExpiry", 604800L);

        String token = otherProvider.generateAccessToken(testUser);

        assertFalse(jwtTokenProvider.validateToken(token));
    }
}
