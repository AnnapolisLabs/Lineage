package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.dto.request.RegisterUserRequest;
import com.annapolislabs.lineage.dto.request.UpdateUserRequest;
import com.annapolislabs.lineage.dto.request.ChangePasswordRequest;
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
 * Core user management service providing CRUD operations and user lifecycle management
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
     * Register a new user with email verification
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
     * Update user profile information
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
            if (updater.getGlobalRole() == UserRole.ADMIN) {
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
     * Change user password
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
     * Verify user email
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
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }
    
    /**
     * Get user by email
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }
    
    /**
     * Get user profile by ID
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(UUID userId) {
        User user = getUserById(userId);
        return convertToUserProfileResponse(user);
    }
    
    /**
     * Search users with pagination and filtering
     */
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> searchUsers(String searchTerm, UserStatus status, 
                                                UserRole role, Boolean verified, Pageable pageable) {
        Page<User> users = userRepository.findUsersWithFilters(status, role, verified, pageable);
        return users.map(this::convertToUserProfileResponse);
    }
    
    /**
     * Get all users with pagination
     */
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::convertToUserProfileResponse);
    }
    
    /**
     * Deactivate user account
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
     * Reactivate user account
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
     * Suspend user account
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
     * Get user statistics
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
     * Increment failed login attempts and potentially lock account
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
     * Reset failed login attempts after successful login
     */
    public void resetFailedLoginAttempts(User user) {
        user.resetFailedLoginAttempts();
        userRepository.save(user);
    }
    
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
    
    private String generateSecureToken() {
        return java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(java.security.SecureRandom.getSeed(32));
    }
    
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