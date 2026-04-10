package com.example.zuora.service;

/**
 * Service for sending emails.
 */
public interface EmailService {

    /**
     * Send an email verification link to the user.
     *
     * @param to The recipient email address
     * @param token The verification token
     */
    void sendVerificationEmail(String to, String token);

    /**
     * Send a password reset link to the user.
     *
     * @param to The recipient email address
     * @param token The reset token
     */
    void sendPasswordResetEmail(String to, String token);

    /**
     * Send a welcome email to a new user.
     *
     * @param to The recipient email address
     * @param accountNumber The user's account number
     */
    void sendWelcomeEmail(String to, String accountNumber);

    /**
     * Send a temporary password to a user (for admin-created accounts).
     *
     * @param to The recipient email address
     * @param temporaryPassword The temporary password
     * @param resetLink The link to change password
     */
    void sendTemporaryPasswordEmail(String to, String temporaryPassword, String resetLink);
}