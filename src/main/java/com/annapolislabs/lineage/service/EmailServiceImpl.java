package com.annapolislabs.lineage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Override
    public void sendWelcomeEmail(String email, String firstName) {
        logger.info("Welcome email would be sent to: {} for user: {}", email, firstName);
        // Note: Email sending not implemented - requires SMTP configuration
    }

    @Override
    public void sendEmailVerificationEmail(String email, String verificationToken) {
        logger.info("Email verification would be sent to: {} with token: {}", email, verificationToken);
        // Note: Email sending not implemented - requires SMTP configuration
    }

    @Override
    public void sendPasswordResetEmail(String email, String resetToken) {
        logger.info("Password reset email would be sent to: {} with token: {}", email, resetToken);
        // Note: Email sending not implemented - requires SMTP configuration
    }

    @Override
    public void sendInvitationEmail(String email, String firstName, String invitationToken) {
        logger.info("Invitation email would be sent to: {} for user: {} with token: {}", email, firstName, invitationToken);
        // Note: Email sending not implemented - requires SMTP configuration
    }

    @Override
    public void sendMfaSetupEmail(String email, String qrCodeUrl) {
        logger.info("MFA setup email would be sent to: {} with QR code URL: {}", email, qrCodeUrl);
        // Note: Email sending not implemented - requires SMTP configuration
    }
}