package com.example.zuora.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Liability Waiver acceptance request DTO
 */
public class WaiverRequest {

    @NotBlank(message = "Please type your full name as signature")
    @Size(min = 2, max = 100, message = "Signature name must be between 2-100 characters")
    private String signatureName;

    // Base64 encoded signature image (optional - for drawn signatures)
    private String signatureData;

    @NotBlank(message = "Waiver version is required")
    private String waiverVersion;

    @AssertTrue(message = "You must agree to the liability waiver")
    private boolean agreedToWaiver;

    // ==================== GETTERS AND SETTERS ====================

    public String getSignatureName() { return signatureName; }
    public void setSignatureName(String signatureName) { this.signatureName = signatureName; }

    public String getSignatureData() { return signatureData; }
    public void setSignatureData(String signatureData) { this.signatureData = signatureData; }

    public String getWaiverVersion() { return waiverVersion; }
    public void setWaiverVersion(String waiverVersion) { this.waiverVersion = waiverVersion; }

    public boolean isAgreedToWaiver() { return agreedToWaiver; }
    public void setAgreedToWaiver(boolean agreedToWaiver) { this.agreedToWaiver = agreedToWaiver; }
}