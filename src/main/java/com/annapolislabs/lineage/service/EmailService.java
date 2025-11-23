package com.annapolislabs.lineage.service;

/**
 * Declares the transactional email operations emitted by the platform. Each method documents the
 * business trigger and the tokens that must be passed to the downstream templating layer so
 * implementations can enforce rate limits, capture telemetry, and surface delivery failures.
 */
public interface EmailService {

    /**
     * Sends a personalized onboarding email immediately after account creation.
     *
     * @param email     validated recipient address
     * @param firstName optional given name for greeting personalization; may be {@code null}
     */
    void sendWelcomeEmail(String email, String firstName);

    /**
     * Sends an email verification link to activate a pending account.
     *
     * @param email             recipient awaiting verification
     * @param verificationToken opaque token embedded in the verification URL (callers enforce TTL)
     */
    void sendEmailVerificationEmail(String email, String verificationToken);

    /**
     * Sends a password-reset message containing a short-lived reset token.
     *
     * @param email     account owner requesting the reset
     * @param resetToken secure token that must be provided alongside the new password
     */
    void sendPasswordResetEmail(String email, String resetToken);

    /**
     * Sends an invitation that allows another user to join a project or workspace.
     *
     * @param email           invitee email address
     * @param firstName       optional friendly name shown within the invitation body
     * @param invitationToken unique token consumed when the invite is accepted
     */
    void sendInvitationEmail(String email, String firstName, String invitationToken);

    /**
     * Sends an MFA setup email containing a QR provisioning URL (or equivalent instructions).
     *
     * @param email     account address enabling MFA
     * @param qrCodeUrl link to the QR image/provisioning URI compatible with authenticator apps
     */
    void sendMfaSetupEmail(String email, String qrCodeUrl);

    // Team invitation methods
    /**
     * Sends a team invitation email
     */
    void sendTeamInvitation(String userEmail, String teamName, String roleDisplayName, 
                           String message, String teamId, String invitationId);

    // Task assignment methods
    /**
     * Sends task assignment notification email
     */
    void sendTaskAssignmentNotification(String assigneeEmail, String taskTitle, 
                                      String taskDescription, String projectId);

    // Peer review methods
    /**
     * Sends peer review invitation email
     */
    void sendPeerReviewInvitation(String reviewerEmail, String requirementTitle, 
                                 String authorName, String reviewType, String deadline);
    
    /**
     * Sends peer review approval notification email
     */
    void sendPeerReviewApprovalNotification(String authorEmail, String requirementTitle, 
                                          String reviewerName, String comments);
    
    /**
     * Sends peer review rejection notification email
     */
    void sendPeerReviewRejectionNotification(String authorEmail, String requirementTitle, 
                                           String reviewerName, String comments);
    
    /**
     * Sends peer review revision request notification email
     */
    void sendPeerReviewRevisionRequestNotification(String authorEmail, String requirementTitle, 
                                                 String reviewerName, String feedback);
}
