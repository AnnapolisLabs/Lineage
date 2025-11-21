package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.dto.request.LoginRequest;
import com.annapolislabs.lineage.dto.response.AuthResponse;
import com.annapolislabs.lineage.dto.response.UserProfileResponse;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserStatus;
import com.annapolislabs.lineage.exception.auth.AccountLockedException;
import com.annapolislabs.lineage.exception.auth.EmailNotVerifiedException;
import com.annapolislabs.lineage.repository.UserRepository;
import com.annapolislabs.lineage.security.JwtTokenProvider;
import com.annapolislabs.lineage.security.SecurityAuditService;
import com.annapolislabs.lineage.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String UNKNOWN = "unknown";
    private static final int ACCOUNT_LOCKOUT_DURATION_MINUTES = 30;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final SecurityAuditService securityAuditService;

    @Autowired
    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       JwtTokenProvider jwtTokenProvider,
                       UserService userService,
                       SecurityAuditService securityAuditService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
        this.securityAuditService = securityAuditService;
    }

    /**
     * Authenticate user and generate JWT token pair.
     * Centralizes all login-related logic including:
     * - credential validation
     * - account lockout and email verification checks
     * - audit logging
     * - last login tracking and failed-attempt reset
     */
    public AuthResponse login(LoginRequest request) {
        return login(request, UNKNOWN);
    }

    public AuthResponse login(LoginRequest request, String clientIp) {
        String ipAddress = (clientIp != null && !clientIp.isBlank()) ? clientIp : UNKNOWN;
        String email = request.getEmail() != null ? request.getEmail().toLowerCase() : null;

        try {
            logger.info("Login attempt for email: {}", email);

            // Authenticate user credentials via AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Load user entity
            User user = userService.getUserByEmail(email);

            // Check account lock status
            if (user.isAccountLocked()) {
                throw new AccountLockedException(
                        "Account is locked due to multiple failed login attempts. Please try again later.",
                        ACCOUNT_LOCKOUT_DURATION_MINUTES
                );
            }

            // Check email verification status
            if (!user.isEmailVerified() && user.getStatus() == UserStatus.PENDING_VERIFICATION) {
                throw new EmailNotVerifiedException("Please verify your email address before logging in.");
            }

            // Generate access + refresh tokens
            JwtTokenProvider.TokenPair tokenPair = jwtTokenProvider.generateTokenPair(user);

            // Update login metadata and reset failed attempts
            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(ipAddress);
            userService.resetFailedLoginAttempts(user);

            // Build user profile response
            UserProfileResponse userProfile = userService.getUserProfile(user.getId());

            // Log successful login
            securityAuditService.logAuthenticationEvent(
                    user.getId().toString(),
                    "LOGIN_SUCCESS",
                    com.annapolislabs.lineage.entity.AuditSeverity.INFO,
                    true,
                    java.util.Map.of("email", user.getEmail(), "login_method", "password")
            );

            return new AuthResponse(
                    true,
                    "Login successful",
                    user.getId(),
                    user.getEmail(),
                    tokenPair.getAccessToken(),
                    tokenPair.getRefreshToken(),
                    userProfile
            );

        } catch (BadCredentialsException _) {
            handleFailedLogin(email, ipAddress);
            // Normalize the error message so we don't leak whether the email exists
            throw new BadCredentialsException("Invalid email or password");
        } catch (AuthenticationException e) {
            logger.error("Authentication failed for email: {}", email, e);
            securityAuditService.logAuthenticationEvent(
                    UNKNOWN,
                    "LOGIN_FAILED",
                    com.annapolislabs.lineage.entity.AuditSeverity.WARNING,
                    false,
                    java.util.Map.of("email", email != null ? email : UNKNOWN, "error", e.getMessage())
            );
            throw e;
        }
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void handleFailedLogin(String email, String clientIp) {
        try {
            if (email == null) {
                return;
            }
            User user = userService.getUserByEmail(email);
            if (user != null) {
                userService.incrementFailedLoginAttempts(user, clientIp);
            }
        } catch (Exception ex) {
            logger.error("Failed to update login attempts for email: {}", email, ex);
        }
    }
}
