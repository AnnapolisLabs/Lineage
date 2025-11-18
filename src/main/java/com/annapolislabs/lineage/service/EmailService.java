package com.annapolislabs.lineage.service;

public interface EmailService {
    
    void sendWelcomeEmail(String email, String firstName);
    
    void sendEmailVerificationEmail(String email, String verificationToken);
    
    void sendPasswordResetEmail(String email, String resetToken);
    
    void sendInvitationEmail(String email, String firstName, String invitationToken);
    
    void sendMfaSetupEmail(String email, String qrCodeUrl);
}