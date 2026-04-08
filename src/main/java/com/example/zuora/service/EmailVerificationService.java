package com.example.zuora.service;

import com.example.zuora.model.User;
import com.example.zuora.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for handling email verification workflow
 */
@Service
public class EmailVerificationService {

    private final UserRepository userRepository;

    @Value("${app.email.verification.required:true}")
    private boolean emailVerificationRequired;

    @Value("${app.email.verification.token-expiry-hours:24}")
    private int tokenExpiryHours;

    public EmailVerificationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Generate a verification token for a user
     */
    @Transactional
    public String generateVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setVerificationTokenExpires(LocalDateTime.now().plusHours(tokenExpiryHours));
        userRepository.save(user);
        return token;
    }

    /**
     * Verify email using the token
     * @return true if verification successful, false otherwise
     */
    @Transactional
    public boolean verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token).orElse(null);

        if (user == null) {
            return false;
        }

        if (user.getVerificationTokenExpires() == null ||
            user.getVerificationTokenExpires().isBefore(LocalDateTime.now())) {
            return false;
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpires(null);
        userRepository.save(user);
        return true;
    }

    /**
     * Check if email verification is required
     */
    public boolean isEmailVerificationRequired() {
        return emailVerificationRequired;
    }

    /**
     * Check if a user has verified their email
     */
    public boolean isUserVerified(User user) {
        if (!emailVerificationRequired) {
            return true;
        }
        return Boolean.TRUE.equals(user.getEmailVerified());
    }

    /**
     * Resend verification email (generates new token)
     */
    @Transactional
    public String resendVerification(User user) {
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return null; // Already verified
        }
        return generateVerificationToken(user);
    }

    /**
     * Check if a token is valid (not expired)
     */
    public boolean isTokenValid(String token) {
        User user = userRepository.findByVerificationToken(token).orElse(null);
        if (user == null) {
            return false;
        }
        return user.getVerificationTokenExpires() != null &&
               user.getVerificationTokenExpires().isAfter(LocalDateTime.now());
    }
}