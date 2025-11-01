package com.annapolislabs.lineage.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-for-jwt-must-be-at-least-256-bits-long");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);

        userDetails = new User("test@example.com", "password", new ArrayList<>());
    }

    @Test
    void generateToken_Success() {
        // Act
        String token = jwtUtil.generateToken("test@example.com");

        // Assert
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void extractUsername_Success() {
        // Arrange
        String token = jwtUtil.generateToken("test@example.com");

        // Act
        String username = jwtUtil.extractUsername(token);

        // Assert
        assertEquals("test@example.com", username);
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        // Arrange
        String token = jwtUtil.generateToken("test@example.com");

        // Act
        Boolean isValid = jwtUtil.validateToken(token, userDetails);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void validateToken_WrongUser_ReturnsFalse() {
        // Arrange
        String token = jwtUtil.generateToken("test@example.com");
        UserDetails wrongUser = new User("wrong@example.com", "password", new ArrayList<>());

        // Act
        Boolean isValid = jwtUtil.validateToken(token, wrongUser);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void extractExpiration_Success() {
        // Arrange
        String token = jwtUtil.generateToken("test@example.com");

        // Act
        var expiration = jwtUtil.extractExpiration(token);

        // Assert
        assertNotNull(expiration);
        assertTrue(expiration.getTime() > System.currentTimeMillis());
    }
}
