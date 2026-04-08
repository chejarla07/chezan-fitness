package com.example.zuora.service;

import com.example.zuora.model.User;
import com.example.zuora.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for handling password reset workflow
 */
@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.password-reset.token-expiry-hours:1}")
    private int tokenExpiryHours;

    public PasswordResetService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Initiate password reset for an email address
     * @return the reset token (to be sent via email), or null if email doesn't exist
     */
    @Transactional
    public String initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // Return null but don't reveal if email exists or not
            return null;
        }

        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetExpires(LocalDateTime.now().plusHours(tokenExpiryHours));
        userRepository.save(user);

        return token;
    }

    /**
     * Reset password using the token
     * @return true if password reset successful, false otherwise
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token).orElse(null);

        if (user == null) {
            return false;
        }

        if (user.getPasswordResetExpires() == null ||
            user.getPasswordResetExpires().isBefore(LocalDateTime.now())) {
            return false;
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpires(null);
        userRepository.save(user);
        return true;
    }

    /**
     * Validate a password reset token
     */
    public boolean validateToken(String token) {
        User user = userRepository.findByPasswordResetToken(token).orElse(null);
        if (user == null) {
            return false;
        }
        return user.getPasswordResetExpires() != null &&
               user.getPasswordResetExpires().isAfter(LocalDateTime.now());
    }

    /**
     * Invalidate a password reset token
     */
    @Transactional
    public void invalidateToken(String token) {
        User user = userRepository.findByPasswordResetToken(token).orElse(null);
        if (user != null) {
            user.setPasswordResetToken(null);
            user.setPasswordResetExpires(null);
            userRepository.save(user);
        }
    }
}