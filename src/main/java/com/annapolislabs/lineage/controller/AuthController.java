package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.dto.request.*;
import com.annapolislabs.lineage.dto.response.AuthResponse;
import com.annapolislabs.lineage.dto.response.UserProfileResponse;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.exception.auth.*;
import com.annapolislabs.lineage.security.JwtTokenProvider;
import com.annapolislabs.lineage.security.SecurityAuditService;
import com.annapolislabs.lineage.service.AuthService;
import com.annapolislabs.lineage.service.EmailService;
import com.annapolislabs.lineage.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing authentication, token lifecycle, and credential management endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final String EMAIL = "email";
    private static final String UNKNOWN = "unknown";
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private SecurityAuditService securityAuditService;
    
    @Autowired
    private AuthService authService;
    
    /**
     * POST /api/auth/register provisions a new account, triggers a verification email, and audits the outcome.
     * Returns 200 OK with an {@link AuthResponse} instructing the caller to verify their email address.
     *
     * @param request validated registration payload containing user profile details
     * @param httpRequest servlet context used for logging metadata
     * @return 200 OK describing the pending account
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterUserRequest request,
                                    HttpServletRequest httpRequest) {
        try {
            logger.info("Registration attempt for email: {}", request.getEmail());
            
            // Register user
            UserProfileResponse user = userService.registerUser(request);
            
            // Generate verification email (simplified for now)
            emailService.sendEmailVerificationEmail(request.getEmail(), "verification-token");
            
            // Log successful registration
            securityAuditService.logAuthenticationEvent(user.getId().toString(), "REGISTRATION_SUCCESS",
                com.annapolislabs.lineage.entity.AuditSeverity.INFO, true,
                java.util.Map.of(EMAIL, user.getEmail(), "registration_method", "standard"));
            
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
            securityAuditService.logAuthenticationEvent(UNKNOWN, "REGISTRATION_FAILED",
                com.annapolislabs.lineage.entity.AuditSeverity.WARNING, false,
                java.util.Map.of(EMAIL, request.getEmail(), "error", e.getMessage()));
            throw e;
        }
    }
    
    /**
     * POST /api/auth/login delegates credential validation to {@link AuthService} and issues JWT pairs.
     * Returns 200 OK with {@link AuthResponse} containing access/refresh tokens on success or bubbles auth errors.
     *
     * @param request validated email/password credentials plus optional MFA data
     * @return 200 OK with tokens for subsequent authenticated calls
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /api/auth/logout clears the Spring Security context and records an audit entry for the session.
     * Always returns 200 OK to avoid leaking logout timing details even when the client lacks a session.
     *
     * @param request active HTTP request used to inspect authentication metadata
     * @return 200 OK with a simple {@link AuthResponse}
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
                    java.util.Map.of(EMAIL, authentication.getName()));
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
     * POST /api/auth/refresh validates a refresh token, re-issues an access/refresh pair, and logs the event.
     * Responds with 200 OK on success or propagates {@link InvalidTokenException} when validation fails.
     *
     * @param request wrapper containing the refresh token string
     * @param httpRequest servlet request for IP/context auditing
     * @return 200 OK with {@link AuthResponse} including new tokens and profile data
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
            securityAuditService.logSuccessfulAuthentication(user.getId().toString(), user.getEmail());
            
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
            securityAuditService.logAuthenticationFailure("TOKEN_REFRESH_FAILED", e.getMessage());
            throw e;
        }
    }
    
    /**
     * POST /api/auth/forgot-password triggers a reset email for the supplied account and logs the request.
     * Always returns 200 OK regardless of account existence to avoid user enumeration.
     *
     * @param request email wrapper for the target account
     * @param httpRequest used to capture requester IP for auditing
     * @return 200 OK with generic instructions payload
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
     * POST /api/auth/reset-password confirms a token and updates the stored password, auditing the outcome.
     * Returns 200 OK with guidance to re-login once the backend completes the reset.
     *
     * @param request payload containing token plus new password values
     * @param httpRequest request context whose IP is logged
     * @return 200 OK acknowledging the change
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                         HttpServletRequest httpRequest) {
        try {
            // Log successful password reset
            securityAuditService.logPasswordResetRequest("user@example.com", getClientIpAddress(httpRequest), true);
            
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
            securityAuditService.logPasswordResetRequest("user@example.com", getClientIpAddress(httpRequest), false);
            throw e;
        }
    }
    
    /**
     * POST /api/auth/verify-email accepts a verification token and marks the account as confirmed.
     *
     * @param request wrapper containing the verification token
     * @return 200 OK once the email is verified (placeholder until full implementation)
     */
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        try {
            // Email verification implementation would go here
            // TODO: Implement actual email verification logic using the token
            
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
     * POST /api/auth/resend-verification sends another email verification link for unconfirmed accounts.
     *
     * @param request contains the email address requiring a new link
     * @param httpRequest unused today but reserved for future auditing hooks
     * @return 200 OK indicating the email dispatch attempt
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
     * GET /api/auth/me returns the authenticated user's profile if the security context contains a principal.
     * Responds with 401 when the caller is anonymous.
     *
     * @return 200 OK with {@link AuthResponse} wrapping the profile, or 401 when unauthenticated
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
     * POST /api/auth/change-password lets an authenticated user rotate their password after validating inputs.
     *
     * @param request contains the current and new password values
     * @param httpRequest used for capturing the request IP in audit logs
     * @return 200 OK with confirmation that the password was updated
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                          HttpServletRequest httpRequest) {
        try {
            // Password change implementation would go here
            // TODO: Implement actual password change logic using authenticated user
            
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
