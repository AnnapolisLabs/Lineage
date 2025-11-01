package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.dto.request.LoginRequest;
import com.annapolislabs.lineage.dto.response.AuthResponse;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("test@example.com", "hash", "Test User", UserRole.ADMIN);
        testUser.setId(UUID.randomUUID());
    }

    @Test
    void login_Success() {
        // Arrange
        LoginRequest request = new LoginRequest("test@example.com", "password");
        AuthResponse expectedResponse = new AuthResponse("token123", "test@example.com", "Test User", "ADMIN");
        when(authService.login(any(LoginRequest.class))).thenReturn(expectedResponse);

        // Act
        ResponseEntity<AuthResponse> response = authController.login(request);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("token123", response.getBody().getToken());
        assertEquals("test@example.com", response.getBody().getEmail());
        assertEquals("ADMIN", response.getBody().getRole());
    }

    @Test
    void getCurrentUser_Success() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(testUser);

        // Act
        ResponseEntity<User> response = authController.getCurrentUser();

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("test@example.com", response.getBody().getEmail());
        assertEquals("Test User", response.getBody().getName());
    }
}
