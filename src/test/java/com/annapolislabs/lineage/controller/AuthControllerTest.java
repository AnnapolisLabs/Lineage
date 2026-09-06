package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.dto.request.LoginRequest;
import com.annapolislabs.lineage.dto.response.AuthResponse;
import com.annapolislabs.lineage.dto.response.UserProfileResponse;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.entity.UserStatus;
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
import static org.mockito.Mockito.*;

// AuthController.login() is a thin delegate to AuthService.login(...), so the controller test
// only needs to mock that single collaborator rather than the lower-level dependencies
// (UserService/JwtTokenProvider/AuthenticationManager) that AuthService itself now encapsulates.
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private User testUser;
    private UserProfileResponse userProfile;

    @BeforeEach
    void setUp() {
        testUser = new User("test@example.com", "hash", "Test User", UserRole.ADMINISTRATOR);
        testUser.setId(UUID.randomUUID());
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setEmailVerified(true);
        
        userProfile = new UserProfileResponse();
        userProfile.setId(testUser.getId());
        userProfile.setEmail(testUser.getEmail());
        userProfile.setName(testUser.getName());
        userProfile.setGlobalRole(testUser.getGlobalRole());
    }

    @Test
    void login_Success() {
        // Arrange
        LoginRequest request = new LoginRequest("test@example.com", "password");

        AuthResponse authResponse = new AuthResponse(true, "Login successful", testUser.getId(),
                testUser.getEmail(), "token123", "refresh456", userProfile);
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // Act
        ResponseEntity<AuthResponse> response = authController.login(request);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        AuthResponse body = response.getBody();
        assertEquals("token123", body.getToken());
        assertEquals(testUser.getEmail(), body.getEmail());
    }

    // Removed getCurrentUser_Success test as AuthController no longer has that method
}
