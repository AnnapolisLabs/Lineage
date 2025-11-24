package com.annapolislabs.lineage.config;

import com.annapolislabs.lineage.dto.request.RegisterUserRequest;
import com.annapolislabs.lineage.dto.response.UserProfileResponse;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.entity.UserStatus;
import com.annapolislabs.lineage.repository.UserRepository;
import com.annapolislabs.lineage.service.UserService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Set;

/**
 * Command-line bootstrapper that seeds an initial admin account when none exists, honoring configured credentials
 * or generating secure defaults on first run.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    private static final String DEFAULT_ADMIN_EMAIL = "admin@lineage.app";
    private static final int GENERATED_PASSWORD_LENGTH = 24;

    private final UserRepository userRepository;
    private final UserService userService;
    private final Validator validator;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${lineage.admin.email:}")
    private String adminEmail;

    @Value("${lineage.admin.password:}")
    private String adminPassword;

    /**
     * Constructs the loader with repository, service, and validation collaborators used during bootstrap.
     *
     * @param userRepository persistence accessor used for existence checks and elevation saves.
     * @param userService application service leveraged for standard registration flows.
     * @param validator bean validation facade used to vet configured admin emails.
     */
    public DataLoader(UserRepository userRepository, UserService userService, Validator validator) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.validator = validator;
    }

    /**
     * Executes on application startup to create an admin account when none exist, preferring configured credentials
     * and falling back to generated secure defaults.
     *
     * @param args command-line arguments passed to the Spring Boot runner (unused but required by interface).
     */
    @Override
    public void run(String... args) {
        logger.info("DataLoader: Checking for existing admin user...");

        // If any admin already exists, do nothing (idempotent behavior)
        if (userRepository.existsByGlobalRole(UserRole.ADMINISTRATOR)) {
            logger.info("DataLoader: Admin user already exists. Skipping initial admin creation.");
            return;
        }

        String effectiveEmail = resolveAdminEmail();
        if (effectiveEmail == null) {
            logger.error("DataLoader: No valid admin email could be determined. Skipping initial admin creation.");
            return;
        }

        boolean passwordProvided = adminPassword != null && !adminPassword.trim().isEmpty();
        String effectivePassword = passwordProvided ? adminPassword : generateSecureAdminPassword();

        try {
            logger.info("DataLoader: Creating initial admin user via standard registration flow for email: {}", effectiveEmail);

            RegisterUserRequest request = new RegisterUserRequest();
            request.setEmail(effectiveEmail);
            request.setPassword(effectivePassword);
            request.setFirstName("System");
            request.setLastName("Administrator");
            request.setPhoneNumber(null);
            // Use default global role (VIEWER) during registration, then elevate to ADMIN

            UserProfileResponse profile = userService.registerUser(request);

            // Elevate to admin and ensure account is immediately usable
            User adminUser = userService.getUserById(profile.getId());
            adminUser.setGlobalRole(UserRole.ADMINISTRATOR);
            adminUser.setStatus(UserStatus.ACTIVE);
            adminUser.setEmailVerified(true);
            adminUser.setEmailVerificationToken(null);
            adminUser.setEmailVerificationExpiry(null);
            userRepository.save(adminUser);

            if (passwordProvided) {
                logger.info("DataLoader: Initial admin user created using configured environment password. " +
                        "Password is not logged for security reasons. Email: {}", effectiveEmail);
            } else {
                logGeneratedAdminCredentials(effectiveEmail, effectivePassword);
            }

        } catch (Exception e) {
            logger.error("DataLoader: Failed to create initial admin user", e);
        }
    }

    /**
     * Determines which email should be used for the initial admin by validating configured values and falling back to
     * defaults as required.
     *
     * @return sanitized admin email or {@code null} when no valid address can be established.
     */
    private String resolveAdminEmail() {
        String rawConfigured = (adminEmail != null && !adminEmail.trim().isEmpty())
                ? adminEmail.trim()
                : null;

        String sanitizedConfigured = sanitizeAdminEmail(rawConfigured);

        if (sanitizedConfigured != null && isValidAdminEmail(sanitizedConfigured)) {
            if (rawConfigured != null && !sanitizedConfigured.equalsIgnoreCase(rawConfigured)) {
                // Avoid logging potentially sensitive raw configuration values; only log sanitized form.
                logger.warn("DataLoader: Admin email configuration contained extra data; using '{}' as the admin email.", sanitizedConfigured);
            }
            return sanitizedConfigured.toLowerCase();
        }

        if (sanitizedConfigured != null) {
            logger.error("DataLoader: Configured admin email value is invalid according to @Email; falling back to default admin email.");
        }

        String defaultEmail = sanitizeAdminEmail(DEFAULT_ADMIN_EMAIL);
        if (isValidAdminEmail(defaultEmail)) {
            logger.info("DataLoader: Using default admin email '{}'", defaultEmail);
            return defaultEmail.toLowerCase();
        }

        logger.error("DataLoader: Default admin email '{}' is invalid according to @Email; initial admin user will not be created.", defaultEmail);
        return null;
    }

    /**
     * Performs defensive trimming and tokenization on the configured admin email to remove commas/whitespace that may
     * hide additional sensitive content.
     *
     * @param rawEmail raw configuration value.
     * @return sanitized lowercase email or {@code null} when no token remains.
     */
    private String sanitizeAdminEmail(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            return null;
        }

        String email = rawEmail.trim();
        boolean containsSensitive = email.toLowerCase().contains("password");

        int commaIndex = email.indexOf(',');
        if (commaIndex > -1) {
            String beforeComma = email.substring(0, commaIndex).trim();
            if (containsSensitive) {
                logger.warn("DataLoader: Admin email configuration contained additional data after a comma; extra data has been ignored.");
            } else {
                logger.warn("DataLoader: Admin email '{}' contained additional data after a comma; using '{}' as the admin email.", email, beforeComma);
            }
            email = beforeComma;
        }

        int spaceIndex = email.indexOf(' ');
        if (spaceIndex > -1) {
            String beforeSpace = email.substring(0, spaceIndex).trim();
            if (containsSensitive) {
                logger.warn("DataLoader: Admin email configuration contained whitespace and additional data; only the first token will be used as the email.");
            } else {
                logger.warn("DataLoader: Admin email '{}' contained whitespace; using '{}' as the admin email.", email, beforeSpace);
            }
            email = beforeSpace;
        }

        return email.toLowerCase();
    }

    /**
     * Validates the email using Jakarta Bean Validation constraints defined on {@link User#email}.
     *
     * @param email candidate address.
     * @return {@code true} when validation passes.
     */
    private boolean isValidAdminEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        Set<ConstraintViolation<User>> violations =
                validator.validateValue(User.class, "email", email);

        if (!violations.isEmpty()) {
            ConstraintViolation<User> violation = violations.iterator().next();
            logger.warn("DataLoader: Admin email '{}' failed validation: {}", email, violation.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Generates a high-entropy password when no admin password is provided via configuration.
     *
     * @return randomly generated password string meeting complexity expectations.
     */
    private String generateSecureAdminPassword() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String specials = "@$!%*?&";
        String all = upper + lower + digits + specials;

        StringBuilder sb = new StringBuilder(GENERATED_PASSWORD_LENGTH);

        // Ensure at least one of each required type
        sb.append(upper.charAt(secureRandom.nextInt(upper.length())));
        sb.append(lower.charAt(secureRandom.nextInt(lower.length())));
        sb.append(digits.charAt(secureRandom.nextInt(digits.length())));
        sb.append(specials.charAt(secureRandom.nextInt(specials.length())));

        for (int i = 4; i < GENERATED_PASSWORD_LENGTH; i++) {
            sb.append(all.charAt(secureRandom.nextInt(all.length())));
        }

        // Shuffle characters to avoid predictable positions
        char[] chars = sb.toString().toCharArray();
        for (int i = 0; i < chars.length; i++) {
            int j = secureRandom.nextInt(chars.length);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }

        String password = new String(chars);
        logger.debug("DataLoader: Generated secure admin password of length {}", password.length());
        return password;
    }

    /**
     * Emits the generated credentials to logs so operators can capture them before enforcing a password change.
     *
     * @param email provisioned admin email.
     * @param password random password emitted for first login.
     */
    private void logGeneratedAdminCredentials(String email, String password) {
        logger.warn("====================================================");
        logger.warn("Initial admin user created");
        logger.warn("  Email: {}", email);
        logger.warn("  Temporary password: {}", password);
        logger.warn("  Please log in and change this password immediately.");
        logger.warn("====================================================");
    }
}
