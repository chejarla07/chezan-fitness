package com.example.zuora.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Password reset request DTO
 * Used when user requests a password reset link
 */
public class PasswordResetRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    private String email;

    // ==================== GETTERS AND SETTERS ====================

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}