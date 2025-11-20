package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.dto.response.MfaSetupResponse;
import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserSecurity;
import com.annapolislabs.lineage.repository.UserSecurityRepository;
import com.annapolislabs.lineage.repository.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;

import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Base64;
import java.util.Random;

/**
 * Service for Multi-Factor Authentication (MFA) operations
 * Supports TOTP-based authentication using Google Authenticator
 */
@Service
@Transactional
public class MfaService {

    private static final Logger logger = LoggerFactory.getLogger(MfaService.class);
    private static final int BACKUP_CODES_COUNT = 10;
    private static final int RECOVERY_CODE_LENGTH = 8;
    private static final String USER_SECURITY_SETTINGS_NOT_FOUND = "User security settings not found: ";
    private static final Random RANDOM = new Random();

    private final GoogleAuthenticator googleAuthenticator;
    private final UserSecurityRepository userSecurityRepository;
    private final UserRepository userRepository;

    @Autowired
    public MfaService(UserSecurityRepository userSecurityRepository, UserRepository userRepository) {
        this.userSecurityRepository = userSecurityRepository;
        this.userRepository = userRepository;
        
        // Configure Google Authenticator with 30-second time step
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setTimeStepSizeInMillis(30000L) // 30 seconds
                .setWindowSize(3)
                .setCodeDigits(6)
                .build();
        this.googleAuthenticator = new GoogleAuthenticator(config);
    }

    /**
     * Generate MFA setup information for a user
     */
    public MfaSetupResponse generateMfaSetup(UUID userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            // Generate secret key using GoogleAuthenticator's credentials method
            GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
            String secretKey = key.getKey();

            // Generate QR code URL
            String qrCodeUrl = generateQrCodeUrl(user.getEmail(), secretKey);

            // Generate backup codes
            List<String> backupCodes = generateBackupCodes();

            // Store security settings (but don't enable MFA yet)
            UserSecurity userSecurity = userSecurityRepository.findByUserId(userId)
                    .orElse(new UserSecurity());

            userSecurity.setUserId(userId);
            userSecurity.setSecretKey(encryptSecretKey(secretKey));
            userSecurity.setMfaBackupCodes(encryptBackupCodes(backupCodes));
            userSecurity.setMfaEnabled(false);
            userSecurity.setMfaEnabledAt(null);
            userSecurity.setCreatedAt(LocalDateTime.now());
            userSecurity.setUpdatedAt(LocalDateTime.now());

            userSecurityRepository.save(userSecurity);

            logger.info("MFA setup generated for user: {}", userId);

            return new MfaSetupResponse(secretKey, qrCodeUrl, backupCodes);

        } catch (Exception e) {
            logger.error("Failed to generate MFA setup for user: {}", userId, e);
            throw new com.annapolislabs.lineage.exception.auth.MfaVerificationException("Failed to generate MFA setup", e);
        }
    }

    /**
     * Verify a TOTP code
     */
    public boolean verifyCode(UUID userId, String code) {
        try {
            UserSecurity userSecurity = userSecurityRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException(USER_SECURITY_SETTINGS_NOT_FOUND + userId));

            if (!userSecurity.isMfaEnabled()) {
                logger.warn("MFA verification attempted but MFA not enabled for user: {}", userId);
                return false;
            }

            String secretKey = decryptSecretKey(userSecurity.getSecretKey());
            
            // Verify TOTP code
            boolean isValid = googleAuthenticator.authorize(secretKey, Integer.parseInt(code));
            
            // Log verification attempt
            logger.info("MFA verification for user {}: {} (code: {})", 
                    userId, isValid ? "SUCCESS" : "FAILED", maskCode(code));
            
            if (isValid) {
                // Update last security check
                userSecurity.setLastSecurityCheck(LocalDateTime.now());
                userSecurityRepository.save(userSecurity);
            }

            return isValid;

        } catch (Exception e) {
            logger.error("MFA verification failed for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Enable MFA after successful verification
     */
    public void enableMfa(UUID userId, String verificationCode) {
        try {
            // First verify the code
            if (!verifyCode(userId, verificationCode)) {
                throw new RuntimeException("Invalid MFA verification code");
            }

            // Enable MFA
            UserSecurity userSecurity = userSecurityRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException(USER_SECURITY_SETTINGS_NOT_FOUND + userId));

            userSecurity.setMfaEnabled(true);
            userSecurity.setMfaEnabledAt(LocalDateTime.now());
            userSecurity.setLastSecurityCheck(LocalDateTime.now());
            userSecurity.setUpdatedAt(LocalDateTime.now());

            userSecurityRepository.save(userSecurity);

            logger.info("MFA enabled for user: {}", userId);

        } catch (Exception e) {
            logger.error("Failed to enable MFA for user: {}", userId, e);
            throw new com.annapolislabs.lineage.exception.auth.MfaVerificationException("Failed to enable MFA", e);
        }
    }

    /**
     * Disable MFA (requires verification)
     */
    public void disableMfa(UUID userId) {
        try {
            UserSecurity userSecurity = userSecurityRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException(USER_SECURITY_SETTINGS_NOT_FOUND + userId));

            userSecurity.setMfaEnabled(false);
            userSecurity.setMfaEnabledAt(null);
            userSecurity.setUpdatedAt(LocalDateTime.now());

            userSecurityRepository.save(userSecurity);

            logger.info("MFA disabled for user: {}", userId);

        } catch (Exception e) {
            logger.error("Failed to disable MFA for user: {}", userId, e);
            throw new com.annapolislabs.lineage.exception.auth.MfaVerificationException("Failed to disable MFA", e);
        }
    }

    /**
     * Get backup codes for a user
     */
    public List<String> getBackupCodes(UUID userId) {
        try {
            UserSecurity userSecurity = userSecurityRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException(USER_SECURITY_SETTINGS_NOT_FOUND + userId));

            return decryptBackupCodes(userSecurity.getMfaBackupCodes());

        } catch (Exception e) {
            logger.error("Failed to get backup codes for user: {}", userId, e);
            throw new com.annapolislabs.lineage.exception.auth.MfaVerificationException("Failed to get backup codes", e);
        }
    }

    /**
     * Generate new backup codes
     */
    public List<String> generateNewBackupCodes(UUID userId) {
        try {
            List<String> newBackupCodes = generateBackupCodes();

            UserSecurity userSecurity = userSecurityRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException(USER_SECURITY_SETTINGS_NOT_FOUND + userId));

            userSecurity.setMfaBackupCodes(encryptBackupCodes(newBackupCodes));
            userSecurity.setUpdatedAt(LocalDateTime.now());

            userSecurityRepository.save(userSecurity);

            logger.info("New backup codes generated for user: {}", userId);

            return newBackupCodes;

        } catch (Exception e) {
            logger.error("Failed to generate new backup codes for user: {}", userId, e);
            throw new com.annapolislabs.lineage.exception.auth.MfaVerificationException("Failed to generate backup codes", e);
        }
    }

    /**
     * Check if MFA is enabled for a user
     */
    public boolean isMfaEnabled(UUID userId) {
        try {
            UserSecurity userSecurity = userSecurityRepository.findByUserId(userId)
                    .orElse(null);
            
            return userSecurity != null && userSecurity.isMfaEnabled();

        } catch (Exception e) {
            logger.error("Failed to check MFA status for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Use a backup code
     */
    public boolean useBackupCode(UUID userId, String backupCode) {
        try {
            UserSecurity userSecurity = userSecurityRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException(USER_SECURITY_SETTINGS_NOT_FOUND + userId));

            if (!userSecurity.isMfaEnabled()) {
                return false;
            }

            List<String> codes = decryptBackupCodes(userSecurity.getMfaBackupCodes());
            
            // Remove the used code from the list
            boolean codeFound = codes.remove(backupCode);
            if (codeFound) {
                // Save the updated list with the used code removed
                userSecurity.setMfaBackupCodes(encryptBackupCodes(codes));
                userSecurity.setUpdatedAt(LocalDateTime.now());
                userSecurityRepository.save(userSecurity);
                
                logger.info("Backup code used for user: {}", userId);
                return true;
            }
            
            return false;

        } catch (Exception e) {
            logger.error("Failed to use backup code for user: {}", userId, e);
            return false;
        }
    }

    // Private helper methods

    private String generateQrCodeUrl(String email, String secretKey) {
        return "otpauth://totp/Lineage:" + encodeEmailForQr(email) + 
               "?secret=" + secretKey + "&issuer=Lineage&algorithm=SHA1&digits=6&period=30";
    }

    private String encodeEmailForQr(String email) {
        return email.replace("@", "%40").replace("+", "%2B");
    }

    private List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>();
        
        for (int i = 0; i < BACKUP_CODES_COUNT; i++) {
            // Generate 8-character alphanumeric codes
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < RECOVERY_CODE_LENGTH; j++) {
                code.append(switch (RANDOM.nextInt(3)) {
                    case 0 -> (char) ('A' + RANDOM.nextInt(26));
                    case 1 -> (char) ('a' + RANDOM.nextInt(26));
                    case 2 -> (char) ('0' + RANDOM.nextInt(10));
                    default -> 'A';
                });
            }
            codes.add(code.toString());
        }
        
        return codes;
    }

    private String encryptSecretKey(String secretKey) {
        // In production, use a proper encryption library like JCE
        // For now, use simple Base64 encoding (NOT secure for production)
        return Base64.getEncoder().encodeToString(secretKey.getBytes());
    }

    private String decryptSecretKey(String encryptedKey) {
        // In production, use proper decryption
        return new String(Base64.getDecoder().decode(encryptedKey));
    }

    private String encryptBackupCodes(List<String> codes) {
        // In production, use proper encryption
        String joinedCodes = String.join(",", codes);
        return Base64.getEncoder().encodeToString(joinedCodes.getBytes());
    }

    private List<String> decryptBackupCodes(String encryptedCodes) {
        // In production, use proper decryption
        String decoded = new String(Base64.getDecoder().decode(encryptedCodes));
        return List.of(decoded.split(","));
    }

    private String maskCode(String code) {
        if (code == null || code.length() <= 2) {
            return "****";
        }
        return code.substring(0, 2) + "****";
    }
}