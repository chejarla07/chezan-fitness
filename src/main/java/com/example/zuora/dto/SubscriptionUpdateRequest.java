package com.example.zuora.dto;

import jakarta.validation.constraints.*;

public class SubscriptionUpdateRequest {

    @NotNull(message = "Subscription ID is required")
    private Long subscriptionId;

    @NotNull(message = "New rate plan ID is required")
    private Long newRatePlanId;

    private String reason;

    private boolean effectiveImmediately = false;

    private String effectiveDate; // Optional date in ISO format (YYYY-MM-DD), defaults to today

    // Getters and Setters
    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }

    public Long getNewRatePlanId() { return newRatePlanId; }
    public void setNewRatePlanId(Long newRatePlanId) { this.newRatePlanId = newRatePlanId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public boolean isEffectiveImmediately() { return effectiveImmediately; }
    public void setEffectiveImmediately(boolean effectiveImmediately) { this.effectiveImmediately = effectiveImmediately; }

    public String getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; }
}
