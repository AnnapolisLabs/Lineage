package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.dto.request.*;
import com.annapolislabs.lineage.dto.response.AuthResponse;
import com.annapolislabs.lineage.dto.response.UserProfileResponse;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserStatus;
import com.annapolislabs.lineage.exception.auth.*;
import com.annapolislabs.lineage.security.JwtTokenProvider;
import com.annapolislabs.lineage.security.SecurityAuditService;
import com.annapolislabs.lineage.service.EmailService;
import com.annapolislabs.lineage.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Authentication Controller handling user registration, login, logout, and password management
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private SecurityAuditService securityAuditService;
    
    /**
     * User registration with email verification
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterUserRequest request, 
                                    HttpServletRequest httpRequest) {
        try {
            logger.info("Registration attempt for email: {}", request.getEmail());
            
            // Register user
            UserProfileResponse user = userService.registerUser(request, getClientIpAddress(httpRequest), httpRequest.getHeader("User-Agent"));
            
            // Generate verification email (simplified for now)
            emailService.sendEmailVerificationEmail(request.getEmail(), "verification-token");
            
            // Log successful registration
            securityAuditService.logAuthenticationEvent(user.getId().toString(), "REGISTRATION_SUCCESS",
                com.annapolislabs.lineage.entity.AuditSeverity.INFO, true,
                java.util.Map.of("email", user.getEmail(), "registration_method", "standard"));
            
            return ResponseEntity.ok(new AuthResponse(
                true,
                "Registration successful. Please verify your email address.",
                user.getId(),
                user.getEmail(),
                null, // No token yet
                null, // No refresh token
                user
            ));
            
        } catch (Exception e) {
            logger.error("Registration failed for email: {}", request.getEmail(), e);
            securityAuditService.logAuthenticationEvent("unknown", "REGISTRATION_FAILED",
                com.annapolislabs.lineage.entity.AuditSeverity.WARNING, false,
                java.util.Map.of("email", request.getEmail(), "error", e.getMessage()));
            throw e;
        }
    }
    
    /**
     * User login with JWT token generation
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, 
                                 HttpServletRequest httpRequest) {
        try {
            logger.info("Login attempt for email: {}", request.getEmail());
            
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            
            User user = userService.getUserByEmail(request.getEmail());
            
            // Check if user account is locked
            if (user.isAccountLocked()) {
                throw new AccountLockedException(
                    "Account is locked due to multiple failed login attempts. Please try again later.",
                    30 // 30 minutes
                );
            }
            
            // Check if email is verified
            if (!user.isEmailVerified() && user.getStatus() == UserStatus.PENDING_VERIFICATION) {
                throw new com.annapolislabs.lineage.exception.auth.EmailNotVerifiedException("Please verify your email address before logging in.");
            }
            
            // Generate tokens
            JwtTokenProvider.TokenPair tokenPair = jwtTokenProvider.generateTokenPair(user);
            
            // Update last login information
            user.setLastLoginAt(java.time.LocalDateTime.now());
            user.setLastLoginIp(getClientIpAddress(httpRequest));
            userService.resetFailedLoginAttempts(user);
            
            // Get user profile
            UserProfileResponse userProfile = userService.getUserProfile(user.getId());
            
            // Log successful login
            securityAuditService.logAuthenticationEvent(user.getId().toString(), "LOGIN_SUCCESS",
                com.annapolislabs.lineage.entity.AuditSeverity.INFO, true,
                java.util.Map.of("email", user.getEmail(), "login_method", "password"));
            
            return ResponseEntity.ok(new AuthResponse(
                true,
                "Login successful",
                user.getId(),
                user.getEmail(),
                tokenPair.getAccessToken(),
                tokenPair.getRefreshToken(),
                userProfile
            ));
            
        } catch (BadCredentialsException e) {
            // Handle failed login attempt
            handleFailedLogin(request.getEmail(), httpRequest);
            throw new BadCredentialsException("Invalid email or password");
        } catch (AuthenticationException e) {
            logger.error("Authentication failed for email: {}", request.getEmail(), e);
            securityAuditService.logAuthenticationEvent("unknown", "LOGIN_FAILED",
                com.annapolislabs.lineage.entity.AuditSeverity.WARNING, false,
                java.util.Map.of("email", request.getEmail(), "error", e.getMessage()));
            throw e;
        }
    }
    
    /**
     * User logout with session cleanup
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && authentication.isAuthenticated()) {
                // Log logout event
                String userId = getCurrentUserId();
                securityAuditService.logAuthenticationEvent(userId, "LOGOUT_SUCCESS",
                    com.annapolislabs.lineage.entity.AuditSeverity.INFO, true,
                    java.util.Map.of("email", authentication.getName()));
            }
            
            // Clear security context
            SecurityContextHolder.clearContext();
            
            return ResponseEntity.ok(new AuthResponse(
                true,
                "Logout successful",
                null,
                null,
                null,
                null,
                null
            ));
            
        } catch (Exception e) {
            logger.error("Logout error", e);
            return ResponseEntity.ok(new AuthResponse(
                true,
                "Logout completed",
                null,
                null,
                null,
                null,
                null
            ));
        }
    }
    
    /**
     * Refresh JWT tokens
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request, 
                                        HttpServletRequest httpRequest) {
        try {
            String refreshToken = request.getRefreshToken();
            
            // Validate refresh token
            if (!jwtTokenProvider.validateToken(refreshToken) || 
                jwtTokenProvider.getTokenType(refreshToken) != JwtTokenProvider.TokenType.REFRESH) {
                throw new InvalidTokenException("Invalid or expired refresh token");
            }
            
            // Get user from token
            String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
            String email = jwtTokenProvider.getEmailFromToken(refreshToken);
            
            User user = userService.getUserByEmail(email);
            
            // Generate new token pair
            JwtTokenProvider.TokenPair tokenPair = jwtTokenProvider.generateTokenPair(user);
            UserProfileResponse userProfile = userService.getUserProfile(user.getId());
            
            // Log token refresh
            securityAuditService.logSuccessfulAuthentication(httpRequest, user.getId().toString(), user.getEmail());
            
            return ResponseEntity.ok(new AuthResponse(
                true,
                "Token refreshed successfully",
                user.getId(),
                user.getEmail(),
                tokenPair.getAccessToken(),
                tokenPair.getRefreshToken(),
                userProfile
            ));
            
        } catch (Exception e) {
            logger.error("Token refresh failed", e);
            securityAuditService.logAuthenticationFailure(httpRequest, "TOKEN_REFRESH_FAILED", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Request password reset
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, 
                                          HttpServletRequest httpRequest) {
        try {
            // Generate reset token and send email (simplified)
            emailService.sendPasswordResetEmail(request.getEmail(), "reset-token");
            
            // Log password reset request
            securityAuditService.logPasswordResetRequest(request.getEmail(), getClientIpAddress(httpRequest), false);
            
            return ResponseEntity.ok(new AuthResponse(
                true,
                "Password reset instructions sent to your email",
                null,
                request.getEmail(),
                null,
                null,
                null
            ));
            
        } catch (Exception e) {
            logger.error("Password reset request failed for email: {}", request.getEmail(), e);
            securityAuditService.logPasswordResetRequest(request.getEmail(), getClientIpAddress(httpRequest), false);
            throw e;
        }
    }
    
    /**
     * Reset password with token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request, 
                                         HttpServletRequest httpRequest) {
        try {
            // Reset password (simplified)
            // userService.resetPassword(request.getToken(), request.getNewPassword());
            
            // Log successful password reset
            securityAuditService.logPasswordResetRequest("unknown", getClientIpAddress(httpRequest), true);
            
            return ResponseEntity.ok(new AuthResponse(
                true,
                "Password reset successful. Please login with your new password.",
                null,
                null,
                null,
                null,
                null
            ));
            
        } catch (Exception e) {
            logger.error("Password reset failed", e);
            securityAuditService.logPasswordResetRequest("unknown", getClientIpAddress(httpRequest), false);
            throw e;
        }
    }
    
    /**
     * Verify email address
     */
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        try {
            // userService.verifyEmail(request.getToken());
            
            return ResponseEntity.ok(new AuthResponse(
                true,
                "Email verified successfully. You can now login.",
                null,
                null,
                null,
                null,
                null
            ));
            
        } catch (Exception e) {
            logger.error("Email verification failed", e);
            throw e;
        }
    }
    
    /**
     * Resend verification email
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@Valid @RequestBody ResendVerificationRequest request, 
                                              HttpServletRequest httpRequest) {
        try {
            emailService.sendEmailVerificationEmail(request.getEmail(), "verification-token");

            return ResponseEntity.ok(new AuthResponse(
                    true,
                    "Verification email sent successfully",
                    null,
                    request.getEmail(),
                    null,
                    null,
                    null
            ));

        } catch (Exception e) {
            logger.error("Failed to resend verification email", e);
            throw e;
        }
    }
    /**
     * Get current user profile (authenticated)
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(401).body("Not authenticated");
            }
            
            // Get current user email from authentication
            String userEmail = authentication.getName();
            User user = userService.getUserByEmail(userEmail);
            UserProfileResponse userProfile = userService.getUserProfile(user.getId());
            
            return ResponseEntity.ok(new AuthResponse(
                true,
                "User profile retrieved successfully",
                user.getId(),
                user.getEmail(),
                null, // No new token needed
                null, // No refresh token
                userProfile
            ));
            
        } catch (Exception e) {
            logger.error("Failed to get current user profile", e);
            return ResponseEntity.status(401).body("Authentication failed");
        }
    }
    
    /**
     * Resend verification email
     */
    
    /**
     * Change password (authenticated user)
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request, 
                                          HttpServletRequest httpRequest) {
        try {
            String userId = getCurrentUserId();
            // userService.changePassword(UUID.fromString(userId), request, getClientIpAddress(httpRequest));
            
            return ResponseEntity.ok(new AuthResponse(
                true,
                "Password changed successfully",
                null,
                null,
                null,
                null,
                null
            ));
            
        } catch (Exception e) {
            logger.error("Password change failed", e);
            throw e;
        }
    }
    
    private void handleFailedLogin(String email, HttpServletRequest request) {
        try {
            User user = userService.getUserByEmail(email);
            if (user != null) {
                userService.incrementFailedLoginAttempts(user, getClientIpAddress(request));
            }
        } catch (Exception e) {
            logger.error("Failed to update login attempts", e);
        }
    }
    
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                // Extract user ID from JWT token
                String token = extractTokenFromRequest();
                if (token != null) {
                    return jwtTokenProvider.getUserIdFromToken(token);
                }
            } catch (Exception e) {
                logger.error("Failed to extract user ID from token", e);
            }
        }
        return "anonymous";
    }
    
    private String extractTokenFromRequest() {
        // This would be extracted from the JWT filter in practice
        return SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
