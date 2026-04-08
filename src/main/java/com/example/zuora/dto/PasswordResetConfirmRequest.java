package com.example.zuora.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Password reset confirmation DTO
 * Used when user submits the new password with the reset token
 */
public class PasswordResetConfirmRequest {

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).*$",
             message = "Password must contain at least one uppercase letter, one lowercase letter, and one number")
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;

    // ==================== VALIDATION ====================

    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordsMatch() {
        if (newPassword == null || confirmPassword == null) {
            return false;
        }
        return newPassword.equals(confirmPassword);
    }

    // ==================== GETTERS AND SETTERS ====================

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}