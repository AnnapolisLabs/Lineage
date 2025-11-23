package com.annapolislabs.lineage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Placeholder implementation that logs when each type of email would be delivered. The class keeps
 * the contract injectable while SMTP credentials and templates are finalized, ensuring callers know
 * that no real emails are dispatched yet. Every method should eventually route through a
 * {@code JavaMailSender} configured with provider credentials and structured templates.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    /**
     * {@inheritDoc}
     *
     * <p>Currently logs the intent to send a welcome email until SMTP delivery is wired up. The log
     * output acts as an audit trail in lower environments so onboarding flows can be validated
     * without dispatching real emails.</p>
     */
    @Override
    public void sendWelcomeEmail(String email, String firstName) {
        logger.info("Welcome email would be sent to: {} for user: {}", email, firstName);
        // TODO: Implement actual email sending functionality with SMTP configuration
        // Required: Add SMTP server configuration properties (host, port, username, password)
        // Required: Configure JavaMailSender bean in configuration class
        // Required: Implement actual email content templating
    }

    /**
     * {@inheritDoc}
     *
     * <p>The implementation currently logs the verification token rather than dispatching an email.
     * Once SMTP support is added the method should embed the token within a secure link that expires
     * according to {@link EmailService} guidance and store telemetry for audit purposes.</p>
     */
    @Override
    public void sendEmailVerificationEmail(String email, String verificationToken) {
        logger.info("Email verification would be sent to: {} with token: {}", email, verificationToken);
        // TODO: Implement actual email sending functionality with SMTP configuration
        // Required: Add SMTP server configuration properties (host, port, username, password)
        // Required: Configure JavaMailSender bean in configuration class
        // Required: Implement actual email content templating
    }

    /**
     * {@inheritDoc}
     *
     * <p>Logs issuance of a password-reset token so QA can confirm the workflow without dispatching
     * sensitive credentials. Production deployments should replace the logger with SMTP delivery and
     * ensure the token is masked in logs to avoid leaking secrets.</p>
     */
    @Override
    public void sendPasswordResetEmail(String email, String resetToken) {
        logger.info("Password reset email would be sent to: {} with token: {}", email, resetToken);
        // TODO: Implement actual email sending functionality with SMTP configuration
        // Required: Add SMTP server configuration properties (host, port, username, password)
        // Required: Configure JavaMailSender bean in configuration class
        // Required: Implement actual email content templating
    }

    /**
     * {@inheritDoc}
     *
     * <p>Logs that an invitation email would be dispatched. When SMTP support is added this method
     * should embed the invitation token into a signed URL and capture delivery telemetry for audits.</p>
     */
    @Override
    public void sendInvitationEmail(String email, String firstName, String invitationToken) {
        logger.info("Invitation email would be sent to: {} for user: {} with token: {}", email, firstName, invitationToken);
        // TODO: Implement actual email sending functionality with SMTP configuration
        // Required: Add SMTP server configuration properties (host, port, username, password)
        // Required: Configure JavaMailSender bean in configuration class
        // Required: Implement actual email content templating
    }

    /**
     * {@inheritDoc}
     *
     * <p>Logs that MFA setup instructions would be delivered, including the QR provisioning URL. The
     * eventual SMTP-backed implementation must ensure the URL is short-lived and delivered over
     * TLS-only channels to avoid exposing TOTP secrets.</p>
     */
    @Override
    public void sendMfaSetupEmail(String email, String qrCodeUrl) {
        logger.info("MFA setup email would be sent to: {} with QR code URL: {}", email, qrCodeUrl);
        // TODO: Implement actual email sending functionality with SMTP configuration
        // Required: Add SMTP server configuration properties (host, port, username, password)
        // Required: Configure JavaMailSender bean in configuration class
        // Required: Implement actual email content templating
    }

    @Override
    public void sendTeamInvitation(String userEmail, String teamName, String roleDisplayName, 
                                 String message, String teamId, String invitationId) {
        logger.info("Team invitation email would be sent to: {} for team '{}' with role '{}'", 
                userEmail, teamName, roleDisplayName);
        // TODO: Implement actual email sending functionality with SMTP configuration
    }

    @Override
    public void sendTaskAssignmentNotification(String assigneeEmail, String taskTitle, 
                                             String taskDescription, String projectId) {
        logger.info("Task assignment notification would be sent to: {} for task '{}' in project {}", 
                assigneeEmail, taskTitle, projectId);
        // TODO: Implement actual email sending functionality with SMTP configuration
    }

    @Override
    public void sendPeerReviewInvitation(String reviewerEmail, String requirementTitle, 
                                       String authorName, String reviewType, String deadline) {
        logger.info("Peer review invitation would be sent to: {} for requirement '{}' by {} with type '{}'", 
                reviewerEmail, requirementTitle, authorName, reviewType);
        // TODO: Implement actual email sending functionality with SMTP configuration
    }

    @Override
    public void sendPeerReviewApprovalNotification(String authorEmail, String requirementTitle, 
                                                 String reviewerName, String comments) {
        logger.info("Peer review approval notification would be sent to: {} for requirement '{}' approved by {}", 
                authorEmail, requirementTitle, reviewerName);
        // TODO: Implement actual email sending functionality with SMTP configuration
    }

    @Override
    public void sendPeerReviewRejectionNotification(String authorEmail, String requirementTitle, 
                                                  String reviewerName, String comments) {
        logger.info("Peer review rejection notification would be sent to: {} for requirement '{}' rejected by {}", 
                authorEmail, requirementTitle, reviewerName);
        // TODO: Implement actual email sending functionality with SMTP configuration
    }

    @Override
    public void sendPeerReviewRevisionRequestNotification(String authorEmail, String requirementTitle, 
                                                       String reviewerName, String feedback) {
        logger.info("Peer review revision request notification would be sent to: {} for requirement '{}' by {}", 
                authorEmail, requirementTitle, reviewerName);
        // TODO: Implement actual email sending functionality with SMTP configuration
    }
}