package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.dto.request.LoginRequest;
import com.annapolislabs.lineage.dto.response.AuthResponse;
import com.annapolislabs.lineage.dto.response.UserProfileResponse;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.entity.UserStatus;
import com.annapolislabs.lineage.repository.UserRepository;
import com.annapolislabs.lineage.security.JwtTokenProvider;
import com.annapolislabs.lineage.security.SecurityAuditService;
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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserService userService;

    @Mock
    private SecurityAuditService securityAuditService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;
    private UserProfileResponse userProfile;

    @BeforeEach
    void setUp() {
        testUser = new User("test@example.com", "password", "Test User", UserRole.USER);
        testUser.setId(UUID.randomUUID());
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setEmailVerified(true);
        testUser.setLastLoginAt(LocalDateTime.now());

        loginRequest = new LoginRequest("test@example.com", "password");

        userProfile = new UserProfileResponse();
        userProfile.setId(testUser.getId());
        userProfile.setEmail(testUser.getEmail());
        userProfile.setName(testUser.getName());
        userProfile.setGlobalRole(testUser.getGlobalRole());

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void login_Success() {
        // Arrange
        JwtTokenProvider.TokenPair tokenPair =
                new JwtTokenProvider.TokenPair("access-token-12345", "refresh-token-67890");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userService.getUserByEmail(anyString())).thenReturn(testUser);
        when(jwtTokenProvider.generateTokenPair(testUser)).thenReturn(tokenPair);
        when(userService.getUserProfile(testUser.getId())).thenReturn(userProfile);

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Login successful", response.getMessage());
        assertEquals(testUser.getId(), response.getUserId());
        assertEquals(testUser.getEmail(), response.getEmail());
        assertEquals("access-token-12345", response.getToken());
        assertEquals("refresh-token-67890", response.getRefreshToken());
        assertNotNull(response.getUser());
        assertEquals(testUser.getEmail(), response.getUser().getEmail());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(securityContext).setAuthentication(authentication);
        verify(userService).getUserByEmail(anyString());
        verify(jwtTokenProvider).generateTokenPair(testUser);
        verify(userService).resetFailedLoginAttempts(testUser);
        verify(userService).getUserProfile(testUser.getId());
    }

    @Test
    void login_UserNotFound() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userService.getUserByEmail(anyString()))
                .thenThrow(new UserService.UserNotFoundException("User not found"));

        // Act & Assert
        assertThrows(UserService.UserNotFoundException.class, () -> authService.login(loginRequest));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userService).getUserByEmail(anyString());
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
        User adminUser = new User("admin@example.com", "password", "Admin User", UserRole.ADMINISTRATOR);
        adminUser.setId(UUID.randomUUID());
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser.setEmailVerified(true);

        LoginRequest adminLogin = new LoginRequest("admin@example.com", "password");
        JwtTokenProvider.TokenPair tokenPair =
                new JwtTokenProvider.TokenPair("admin-access-token", "admin-refresh-token");

        UserProfileResponse adminProfile = new UserProfileResponse();
        adminProfile.setId(adminUser.getId());
        adminProfile.setEmail(adminUser.getEmail());
        adminProfile.setName(adminUser.getName());
        adminProfile.setGlobalRole(adminUser.getGlobalRole());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userService.getUserByEmail(anyString())).thenReturn(adminUser);
        when(jwtTokenProvider.generateTokenPair(adminUser)).thenReturn(tokenPair);
        when(userService.getUserProfile(adminUser.getId())).thenReturn(adminProfile);

        // Act
        AuthResponse response = authService.login(adminLogin);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getUser());
        // Updated RBAC model exposes ADMINISTRATOR as the global role name
        assertEquals("ADMINISTRATOR", response.getUser().getGlobalRole().name());
        assertEquals("admin-access-token", response.getToken());
        assertEquals("admin-refresh-token", response.getRefreshToken());
    }
}
