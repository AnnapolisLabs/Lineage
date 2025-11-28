package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.dto.request.AdminCreateUserRequest;
import com.annapolislabs.lineage.dto.request.AdminSetPasswordRequest;
import com.annapolislabs.lineage.dto.request.ChangePasswordRequest;
import com.annapolislabs.lineage.dto.request.RegisterUserRequest;
import com.annapolislabs.lineage.dto.request.UpdateUserRequest;
import com.annapolislabs.lineage.dto.response.UserProfileResponse;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.entity.UserStatus;
import com.annapolislabs.lineage.repository.UserRepository;
import com.annapolislabs.lineage.repository.UserSecurityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core user management façade that coordinates repository access, security auditing,
 * and email notifications for the entire user lifecycle. All public methods either
 * participate in the class-level transactional scope or declare read-only access to
 * ensure consistent persistence semantics and alignment with the service JavaDoc audit.
 */
@Service
@Transactional
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final int ACCOUNT_LOCKOUT_DURATION_MINUTES = 30;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserSecurityRepository userSecurityRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private com.annapolislabs.lineage.security.SecurityAuditService securityAuditService;
    
    @Autowired
    private EmailService emailService;
    
    /**
     * Registers a new user account, ensuring password strength, uniqueness of email,
     * and issuance of an email verification token. The method logs both operational
     * telemetry and security audit events while sending the verification email best-effort.
     *
     * @param request payload containing the email, password, and optional profile metadata
     * @return {@link UserProfileResponse} reflecting the newly created user state
     * @throws UserAlreadyExistsException if another user already owns the supplied email
     * @throws IllegalArgumentException if the password does not satisfy {@link #validatePassword(String)} rules
     */
    public UserProfileResponse registerUser(RegisterUserRequest request) {
        logger.info("Registering new user with email: {}", request.getEmail());
        
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }
        
        // Validate password
        validatePassword(request.getPassword());
        
        // Create new user
        User user = new User();
        user.setEmail(request.getEmail().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setGlobalRole(request.getGlobalRole());
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setEmailVerified(false);
        
        // Generate email verification token
        String verificationToken = generateSecureToken();
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationExpiry(LocalDateTime.now().plusHours(24));
        
        // Save user
        User savedUser = userRepository.save(user);
        
        // Send verification email
        try {
            emailService.sendEmailVerificationEmail(savedUser.getEmail(), verificationToken);
        } catch (Exception e) {
            logger.error("Failed to send verification email to {}", savedUser.getEmail(), e);
            // Don't fail registration if email fails
        }
        
        // Audit the registration
        securityAuditService.logSecurityEvent(savedUser.getId().toString(), "USER_REGISTERED", "USER", savedUser.getId().toString(),
            com.annapolislabs.lineage.entity.AuditSeverity.INFO,
            java.util.Map.of("email", savedUser.getEmail(), "status", "PENDING_VERIFICATION"));
        
        logger.info("User registered successfully with ID: {}", savedUser.getId());
        return convertToUserProfileResponse(savedUser);
    }

    /**
     * Creates a new user account from the administrative console.
     *
     * <p>This flow is similar to {@link #registerUser(RegisterUserRequest)} but
     * does not require the admin to supply a password. Instead a secure random
     * password is generated and the account is placed into
     * {@link UserStatus#PENDING_VERIFICATION}. Optionally, an invitation /
     * verification email is sent to the new user.</p>
     *
     * @param request   admin create-user payload
     * @param createdBy admin performing the operation (for audit fields)
     * @return {@link UserProfileResponse} for the created user
     */
    public UserProfileResponse adminCreateUser(AdminCreateUserRequest request, UUID createdBy) {
        logger.info("Admin {} creating new user with email: {}", createdBy, request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }

        // Generate a strong random password that satisfies policy. We reuse
        // validatePassword to ensure the generated password is acceptable.
        String rawPassword = generateAdminInitialPassword();
        validatePassword(rawPassword);

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setGlobalRole(request.getGlobalRole());
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setEmailVerified(false);
        user.setCreatedBy(createdBy);

        String verificationToken = generateSecureToken();
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationExpiry(LocalDateTime.now().plusHours(24));

        User savedUser = userRepository.save(user);

        if (request.isSendInvitation()) {
            try {
                emailService.sendEmailVerificationEmail(savedUser.getEmail(), verificationToken);
            } catch (Exception e) {
                logger.error("Failed to send admin invitation email to {}", savedUser.getEmail(), e);
            }
        }

        securityAuditService.logSecurityEvent(
                createdBy != null ? createdBy.toString() : savedUser.getId().toString(),
                "ADMIN_USER_CREATED",
                "USER",
                savedUser.getId().toString(),
                com.annapolislabs.lineage.entity.AuditSeverity.INFO,
                java.util.Map.of("email", savedUser.getEmail(), "role", savedUser.getGlobalRole().name())
        );

        logger.info("Admin user creation successful for ID: {}", savedUser.getId());
        return convertToUserProfileResponse(savedUser);
    }
    
    /**
     * Updates mutable portions of a user's profile while enforcing admin-only role
     * changes and capturing audit events for compliance review.
     *
     * @param userId identifier of the profile being edited
     * @param request container with optional field updates (null/blank values are ignored)
     * @param updatedBy identifier of the operator performing the change, used for authorization/audit trails
     * @return {@link UserProfileResponse} snapshot reflecting persisted changes
     * @throws UserNotFoundException if the target user cannot be located
     */
    public UserProfileResponse updateUser(UUID userId, UpdateUserRequest request, UUID updatedBy) {
        logger.info("Updating user profile for user ID: {}", userId);
        
        User user = getUserById(userId);
        
        // Update allowed fields
        if (StringUtils.hasText(request.getFirstName())) {
            user.setFirstName(request.getFirstName());
        }
        
        if (StringUtils.hasText(request.getLastName())) {
            user.setLastName(request.getLastName());
        }
        
        if (StringUtils.hasText(request.getPhoneNumber())) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        
        if (StringUtils.hasText(request.getBio())) {
            user.setBio(request.getBio());
        }
        
        if (StringUtils.hasText(request.getAvatarUrl())) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        
        if (request.getPreferences() != null) {
            user.setPreferences(request.getPreferences());
        }
        
        // Only admins can change global role
        if (request.getGlobalRole() != null && updatedBy != null) {
            User updater = getUserById(updatedBy);
            if (updater.getGlobalRole() == UserRole.ADMINISTRATOR) {
                user.setGlobalRole(request.getGlobalRole());
            }
        }
        
        user.setUpdatedBy(updatedBy);
        User savedUser = userRepository.save(user);
        
        // Audit the update
        securityAuditService.logDataAccessEvent(updatedBy.toString(), "USER_PROFILE_UPDATED", "USER", userId.toString(),
            com.annapolislabs.lineage.entity.AuditSeverity.INFO,
            java.util.Map.of("updated_fields", request.toString()));
        
        logger.info("User profile updated successfully for user ID: {}", userId);
        return convertToUserProfileResponse(savedUser);
    }
    
    /**
     * Changes a user's password after verifying the current hash, re-validating the
     * new credential against policy, and logging security events. Failed verifications
     * increment the lockout counter and may trigger account locking.
     *
     * @param userId identifier of the account whose password is being rotated
     * @param request wrapper containing the current and new password values
     * @param clientIp best-known client IP for inclusion in audit metadata (may be null)
     * @throws InvalidPasswordException if the current password is wrong or the new password matches the old hash
     * @throws IllegalArgumentException if the new password violates {@link #validatePassword(String)}
     */
    public void changePassword(UUID userId, ChangePasswordRequest request, String clientIp) {
        logger.info("Changing password for user ID: {}", userId);
        
        User user = getUserById(userId);
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            incrementFailedLoginAttempts(user, clientIp);
            throw new InvalidPasswordException("Current password is incorrect");
        }
        
        // Validate new password
        validatePassword(request.getNewPassword());
        
        // Check if new password is different from current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("New password must be different from current password");
        }
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedLoginAttempts(0); // Reset failed attempts on successful password change
        user.setLockedUntil(null);
        
        userRepository.save(user);
        
        // Audit the password change
        securityAuditService.logAccountSecurityEvent(userId.toString(), "PASSWORD_CHANGED",
            com.annapolislabs.lineage.entity.AuditSeverity.INFO,
            java.util.Map.of("clientIp", clientIp != null ? clientIp : "unknown"));
        
        logger.info("Password changed successfully for user ID: {}", userId);
    }

    /**
     * Allows an administrator to directly set or reset a user's password without
     * requiring the old password. This is intended for support and compliance
     * workflows and is restricted to admin callers at the controller layer.
     *
     * <p>The new password is validated using the same policy as end-user
     * password changes and will clear any lockout state on the account.</p>
     *
     * @param userId      identifier of the account whose password is being set
     * @param newPassword clear-text new password to apply
     * @param adminId     admin performing the operation for audit trails
     */
    public void adminSetPassword(UUID userId, String newPassword, UUID adminId) {
        logger.warn("Admin {} is setting password for user ID: {}", adminId, userId);

        validatePassword(newPassword);

        User user = getUserById(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setUpdatedBy(adminId);

        userRepository.save(user);

        securityAuditService.logAccountSecurityEvent(
                adminId != null ? adminId.toString() : userId.toString(),
                "PASSWORD_RESET_ADMIN",
                com.annapolislabs.lineage.entity.AuditSeverity.WARNING,
                java.util.Map.of("targetUserId", userId.toString())
        );

        logger.info("Admin password set completed for user ID: {} by admin: {}", userId, adminId);
    }
    
    /**
     * Confirms ownership of the user's email address using the verification token
     * generated during registration. Successful verification activates the account
     * and clears token metadata; failures raise {@link InvalidTokenException}.
     *
     * @param token opaque verification token supplied by the email link
     * @throws InvalidTokenException if the token is missing, expired, or not associated with any user
     */
    public void verifyEmail(String token) {
        logger.info("Verifying email with token: {}", token);
        
        User user = userRepository.findByEmailVerificationToken(token)
            .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));
        
        if (user.getEmailVerificationExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Verification token has expired");
        }
        
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiry(null);
        user.setStatus(UserStatus.ACTIVE);
        
        userRepository.save(user);
        
        // Audit email verification
        securityAuditService.logSecurityEvent(user.getId().toString(), "EMAIL_VERIFIED", "USER", user.getId().toString(),
            com.annapolislabs.lineage.entity.AuditSeverity.INFO, java.util.Map.of());
        
        logger.info("Email verified successfully for user ID: {}", user.getId());
    }
    
    /**
     * Fetches a persistent user entity by identifier within a read-only transaction.
     *
     * @param userId identifier of the user to load
     * @return {@link User} entity managed by the persistence context
     * @throws UserNotFoundException if no user matches the provided identifier
     */
    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }
    
    /**
     * Loads a user entity by email address with read-only semantics. Email lookups
     * are case-sensitive to match repository behavior; callers should normalize input.
     *
     * @param email email address to query
     * @return {@link User} entity for downstream processing
     * @throws UserNotFoundException if no user record matches the email
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }
    
    /**
     * Returns a {@link UserProfileResponse} projection for the requested user, reusing
     * {@link #convertToUserProfileResponse(User)} to centralize mapping logic.
     *
     * @param userId identifier of the profile to load
     * @return response DTO safe for UI consumption
     * @throws UserNotFoundException if the user does not exist
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(UUID userId) {
        User user = getUserById(userId);
        return convertToUserProfileResponse(user);
    }
    
    /**
     * Searches users with pagination support and optional filtering by status, role,
     * and verification flag. Currently the repository ignores {@code searchTerm} but
     * the parameter is retained for future enhancements.
     *
     * @param searchTerm textual filter applied downstream (reserved)
     * @param status optional user status filter
     * @param role optional global role filter
     * @param verified optional email verification flag filter
     * @param pageable pagination information controlling page size and sort order
     * @return page of {@link UserProfileResponse} objects matching the supplied filters
     */
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> searchUsers(String searchTerm, UserStatus status, 
                                                UserRole role, Boolean verified, Pageable pageable) {
        Page<User> users = userRepository.findUsersWithFilters(status, role, verified, pageable);
        return users.map(this::convertToUserProfileResponse);
    }
    
    /**
     * Retrieves all users as a pageable result set using the repository default query.
     *
     * @param pageable pagination directives provided by callers
     * @return pageable {@link UserProfileResponse} collection for UI listing
     */
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::convertToUserProfileResponse);
    }
    
    /**
     * Deactivates a user account by setting its status to {@link UserStatus#DEACTIVATED}
     * and logging a data access event for future audits. Caller identity is stored
     * in {@code updatedBy} for traceability.
     *
     * @param userId identifier of the account being disabled
     * @param deactivatedBy operator performing the action
     * @throws UserNotFoundException if the target user does not exist
     */
    public void deactivateUser(UUID userId, UUID deactivatedBy) {
        logger.info("Deactivating user ID: {}", userId);
        
        User user = getUserById(userId);
        user.setStatus(UserStatus.DEACTIVATED);
        user.setUpdatedBy(deactivatedBy);
        
        userRepository.save(user);
        
        // Audit deactivation
        securityAuditService.logDataAccessEvent(deactivatedBy.toString(), "USER_DEACTIVATED", "USER", userId.toString(),
            com.annapolislabs.lineage.entity.AuditSeverity.WARNING, java.util.Map.of());
        
        logger.info("User deactivated successfully: {}", userId);
    }
    
    /**
     * Re-activates a previously deactivated or suspended user account, restoring
     * {@link UserStatus#ACTIVE} and logging the operator performing the action.
     *
     * @param userId identifier of the account to reactivate
     * @param reactivatedBy operator re-enabling the user
     * @throws UserNotFoundException if the user cannot be found
     */
    public void reactivateUser(UUID userId, UUID reactivatedBy) {
        logger.info("Reactivating user ID: {}", userId);
        
        User user = getUserById(userId);
        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedBy(reactivatedBy);
        
        userRepository.save(user);
        
        // Audit reactivation
        securityAuditService.logDataAccessEvent(reactivatedBy.toString(), "USER_REACTIVATED", "USER", userId.toString(),
            com.annapolislabs.lineage.entity.AuditSeverity.INFO, java.util.Map.of());
        
        logger.info("User reactivated successfully: {}", userId);
    }
    
    /**
     * Suspends a user account, storing the reason for transparency and emitting an audit event.
     *
     * @param userId identifier of the account being suspended
     * @param suspendedBy operator invoking the suspension
     * @param reason explanation persisted in audit metadata to justify the action
     */
    public void suspendUser(UUID userId, UUID suspendedBy, String reason) {
        logger.info("Suspending user ID: {} with reason: {}", userId, reason);
        
        User user = getUserById(userId);
        user.setStatus(UserStatus.SUSPENDED);
        user.setUpdatedBy(suspendedBy);
        
        userRepository.save(user);
        
        // Audit suspension
        securityAuditService.logDataAccessEvent(suspendedBy.toString(), "USER_SUSPENDED", "USER", userId.toString(),
            com.annapolislabs.lineage.entity.AuditSeverity.WARNING, java.util.Map.of("reason", reason));
        
        logger.info("User suspended successfully: {}", userId);
    }

    /**
     * Performs a hard delete / purge operation on a user account for legal compliance.
     *
     * <p>This method anonymizes personally identifiable information on the user record
     * (email, name, phone, IPs, tokens) while preserving referential integrity and
     * historic audit trails wherever possible.
     *
     * @param userId identifier of the account being purged
     * @param purgedBy operator performing the purge for audit tracking
     */
    public void purgeUser(UUID userId, UUID purgedBy) {
        logger.warn("Purging user data for ID: {} initiated by: {}", userId, purgedBy);

        User user = getUserById(userId);

        // Scrub personally identifiable information
        String anonymizedId = "anon-" + userId;
        user.setEmail(anonymizedId + "@deleted.local");
        user.setFirstName("Deleted");
        user.setLastName("User");
        user.setName("Deleted User");
        user.setPhoneNumber(null);
        user.setAvatarUrl(null);
        user.setBio(null);
        user.setPreferences(null);

        // Reset security-sensitive fields
        user.setEmailVerified(false);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiry(null);
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(null);
        user.setLastLoginIp(null);

        // Mark account as deactivated
        user.setStatus(UserStatus.DEACTIVATED);
        user.setUpdatedBy(purgedBy);

        userRepository.save(user);

        securityAuditService.logDataAccessEvent(purgedBy.toString(), "USER_PURGED", "USER", userId.toString(),
            com.annapolislabs.lineage.entity.AuditSeverity.CRITICAL, java.util.Map.of());

        logger.warn("User data purged (anonymized) successfully for ID: {}", userId);
    }
    
    /**
     * Computes aggregate user metrics (total, active, verified, newly created within 30 days)
     * for use in administrative dashboards.
     *
     * @return immutable {@link UserStatistics} snapshot of current repository counts
     */
    @Transactional(readOnly = true)
    public UserStatistics getUserStatistics() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countActiveUsers();
        long verifiedUsers = userRepository.countVerifiedUsers();
        long newUsers = userRepository.countUsersCreatedAfter(thirtyDaysAgo);
        
        return new UserStatistics(totalUsers, activeUsers, verifiedUsers, newUsers);
    }
    
    /**
     * Increments the user's failed login counter, issuing an account lock once the
     * configured threshold is reached and emitting a security audit record.
     *
     * @param user user entity whose counter should be incremented and possibly locked
     * @param clientIp optional client IP associated with the failed attempt
     */
    public void incrementFailedLoginAttempts(User user, String clientIp) {
        user.incrementFailedLoginAttempts();
        
        if (user.getFailedLoginAttempts() >= MAX_FAILED_LOGIN_ATTEMPTS) {
            user.lockAccount(java.time.Duration.ofMinutes(ACCOUNT_LOCKOUT_DURATION_MINUTES));
            securityAuditService.logAccountSecurityEvent(user.getId().toString(), "ACCOUNT_LOCKED",
                com.annapolislabs.lineage.entity.AuditSeverity.WARNING,
                java.util.Map.of("reason", "MAX_FAILED_ATTEMPTS", "clientIp", clientIp != null ? clientIp : "unknown"));
        }
        
        userRepository.save(user);
    }
    
    /**
     * Resets the failed login counter after successful authentication so future attempts
     * do not prematurely lock the account.
     *
     * @param user user entity whose counters should be cleared
     */
    public void resetFailedLoginAttempts(User user) {
        user.resetFailedLoginAttempts();
        userRepository.save(user);
    }

    /**
     * Explicitly locks a user account until manually unlocked by an administrator.
     * This is separate from automatic lockouts triggered by failed logins.
     *
     * @param userId identifier of the account to lock
     * @param lockedBy admin performing the lock action
     */
    public void lockUser(UUID userId, UUID lockedBy) {
        logger.warn("Locking user account ID: {} by admin: {}", userId, lockedBy);

        User user = getUserById(userId);
        user.lockAccount(java.time.Duration.ofDays(3650)); // effectively indefinite until unlocked
        user.setUpdatedBy(lockedBy);
        userRepository.save(user);

        securityAuditService.logAccountSecurityEvent(lockedBy.toString(), "ACCOUNT_LOCKED_ADMIN",
            com.annapolislabs.lineage.entity.AuditSeverity.WARNING,
            java.util.Map.of("targetUserId", userId.toString()));
    }

    /**
     * Unlocks a previously locked user account and clears failed login counters.
     *
     * @param userId identifier of the account to unlock
     * @param unlockedBy admin performing the unlock
     */
    public void unlockUser(UUID userId, UUID unlockedBy) {
        logger.info("Unlocking user account ID: {} by admin: {}", userId, unlockedBy);

        User user = getUserById(userId);
        // Clear lock state and failed attempts
        user.resetFailedLoginAttempts();
        user.setUpdatedBy(unlockedBy);
        userRepository.save(user);

        securityAuditService.logAccountSecurityEvent(unlockedBy.toString(), "ACCOUNT_UNLOCKED_ADMIN",
            com.annapolislabs.lineage.entity.AuditSeverity.INFO,
            java.util.Map.of("targetUserId", userId.toString()));
    }
    
    /**
     * Validates password strength enforcing length and character diversity requirements.
     *
     * @param password clear-text password to inspect
     * @throws IllegalArgumentException if the password violates any policy rule
     */
    private void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        
        if (password.length() < 12) {
            throw new IllegalArgumentException("Password must be at least 12 characters long");
        }
        
        if (password.length() > 128) {
            throw new IllegalArgumentException("Password must not exceed 128 characters");
        }
        
        // Check for required character types
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one number");
        }
        
        if (!password.matches(".*[@$!%*?&].*")) {
            throw new IllegalArgumentException("Password must contain at least one special character (@$!%*?&)");
        }
    }
    /**
     * Generates a URL-safe random token leveraging {@link java.security.SecureRandom}
     * for verification and reset flows.
     *
     * @return encoded token suitable for links/emails
     */
    private String generateSecureToken() {
        return java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(java.security.SecureRandom.getSeed(32));
    }

    /**
     * Generates a strong initial password for admin-created users. The password
     * is random and intended to be changed by the user after first login.
     */
    private String generateAdminInitialPassword() {
        // 16 characters from a rich character set; validatePassword will enforce policy
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@$!%*?&";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    /**
     * Converts a {@link User} entity into a {@link UserProfileResponse} DTO to centralize
     * mapping rules for API responses.
     *
     * @param user entity fetched from persistence
     * @return populated client-facing DTO
     */

    
    /**
     * Converts a {@link User} entity into a {@link UserProfileResponse} DTO to centralize
     * mapping rules for API responses.
     *
     * @param user entity fetched from persistence
     * @return populated client-facing DTO
     */
    private UserProfileResponse convertToUserProfileResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setName(user.getName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setBio(user.getBio());
        response.setPreferences(user.getPreferences());
        response.setStatus(user.getStatus());
        response.setGlobalRole(user.getGlobalRole());
        response.setEmailVerified(user.isEmailVerified());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setLastLoginAt(user.getLastLoginAt());
        return response;
    }
    
    /**
     * User statistics data class
     */
    public static class UserStatistics {
        private final long totalUsers;
        private final long activeUsers;
        private final long verifiedUsers;
        private final long newUsers;
        
        public UserStatistics(long totalUsers, long activeUsers, long verifiedUsers, long newUsers) {
            this.totalUsers = totalUsers;
            this.activeUsers = activeUsers;
            this.verifiedUsers = verifiedUsers;
            this.newUsers = newUsers;
        }
        
        public long getTotalUsers() { return totalUsers; }
        public long getActiveUsers() { return activeUsers; }
        public long getVerifiedUsers() { return verifiedUsers; }
        public long getNewUsers() { return newUsers; }
        
        public double getVerificationRate() {
            return totalUsers > 0 ? (double) verifiedUsers / totalUsers : 0.0;
        }
        
        public double getActiveRate() {
            return totalUsers > 0 ? (double) activeUsers / totalUsers : 0.0;
        }
    }
    
    // Exception classes
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) { super(message); }
    }
    
    public static class UserAlreadyExistsException extends RuntimeException {
        public UserAlreadyExistsException(String message) { super(message); }
    }
    
    public static class InvalidPasswordException extends RuntimeException {
        public InvalidPasswordException(String message) { super(message); }
    }
    
    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) { super(message); }
    }
}