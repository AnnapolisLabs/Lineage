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

/**
 * Centralizes authentication flows including password verification, lockout handling, audit logging,
 * and JWT issuance. The service intentionally hides whether an email exists in the system to prevent
 * user enumeration and to provide consistent responses to clients.
 */
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
     * Authenticates a user via email/password credentials and issues a signed access/refresh token
     * pair. The overload accepts client metadata for audit correlation.
     *
     * <p>The flow enforces lockout windows, email verification, and records audit events regardless
     * of success or failure to keep telemetry consistent.</p>
     *
     * @param request login payload containing email and password
     * @return authentication response including tokens and the hydrated {@link UserProfileResponse}
     */
    public AuthResponse login(LoginRequest request) {
        return login(request, UNKNOWN);
    }

    /**
     * Authenticates the user and issues access/refresh tokens. The overload accepts the client IP so
     * audit logs and lockout telemetry can correlate attempts to a network origin.
     *
     * @param request login payload containing email/password
     * @param clientIp best-effort IP address for the request (may be "unknown")
     * @return authentication response with tokens and profile info
     * @throws AccountLockedException    when the account is temporarily locked
     * @throws EmailNotVerifiedException when the user has not verified their email address
     * @throws BadCredentialsException   when credentials are invalid (message normalized)
     * @throws AuthenticationException   for other Spring Security failures
     */
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

        } catch (BadCredentialsException ex) {
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

    /**
     * Resolves the authenticated {@link User} from the Spring Security context so downstream
     * services can load additional domain data before performing authorization checks.
     *
     * @return persisted user entity representing the current principal
     * @throws RuntimeException when the user cannot be located (e.g., deleted after login)
     */
    /**
     * Resolves the {@link User} associated with the current Spring Security context. Callers use
     * this to hydrate domain models prior to running authorization checks.
     *
     * @return user entity for the authenticated principal
     * @throws RuntimeException when no matching user exists (e.g., deleted while logged in)
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Updates failed login tracking metrics and delegates lockout logic to {@link UserService}. This
     * helper intentionally swallows downstream exceptions to avoid leaking whether an email exists.
     *
     * @param email    normalized email address used to look up the {@link User}
     * @param clientIp best-effort IP address associated with the login attempt
     */
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
