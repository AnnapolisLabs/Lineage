package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.dto.request.LoginRequest;
import com.annapolislabs.lineage.dto.response.AuthResponse;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.repository.UserRepository;
import com.annapolislabs.lineage.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new User("test@example.com", "password", "Test User", UserRole.VIEWER);
        testUser.setId(UUID.randomUUID());

        loginRequest = new LoginRequest("test@example.com", "password");

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void login_Success() {
        // Arrange
        String expectedToken = "jwt-token-12345";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(loginRequest.getEmail())).thenReturn(expectedToken);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(expectedToken, response.getToken());
        assertEquals(testUser.getEmail(), response.getEmail());
        assertEquals(testUser.getName(), response.getName());
        assertEquals(testUser.getRole().name(), response.getRole());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(securityContext).setAuthentication(authentication);
        verify(jwtUtil).generateToken(loginRequest.getEmail());
        verify(userRepository).findByEmail(loginRequest.getEmail());
    }

    @Test
    void login_UserNotFound() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(loginRequest.getEmail())).thenReturn("token");
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> authService.login(loginRequest));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail(loginRequest.getEmail());
    }

    @Test
    void getCurrentUser_Success() {
        // Arrange
        String userEmail = "test@example.com";

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(userEmail);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));

        // Act
        User result = authService.getCurrentUser();

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getName(), result.getName());

        verify(securityContext).getAuthentication();
        verify(authentication).getName();
        verify(userRepository).findByEmail(userEmail);
    }

    @Test
    void getCurrentUser_UserNotFound() {
        // Arrange
        String userEmail = "nonexistent@example.com";

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(userEmail);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> authService.getCurrentUser());

        verify(securityContext).getAuthentication();
        verify(authentication).getName();
        verify(userRepository).findByEmail(userEmail);
    }

    @Test
    void login_WithAdminRole() {
        // Arrange
        User adminUser = new User("admin@example.com", "password", "Admin User", UserRole.ADMIN);
        adminUser.setId(UUID.randomUUID());
        LoginRequest adminLogin = new LoginRequest("admin@example.com", "password");
        String expectedToken = "admin-jwt-token";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(adminLogin.getEmail())).thenReturn(expectedToken);
        when(userRepository.findByEmail(adminLogin.getEmail())).thenReturn(Optional.of(adminUser));

        // Act
        AuthResponse response = authService.login(adminLogin);

        // Assert
        assertNotNull(response);
        assertEquals("ADMIN", response.getRole());
        assertEquals(expectedToken, response.getToken());
    }
}
