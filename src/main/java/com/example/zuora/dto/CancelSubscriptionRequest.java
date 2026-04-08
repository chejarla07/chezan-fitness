package com.example.zuora.dto;

import jakarta.validation.constraints.*;

public class CancelSubscriptionRequest {

    @NotNull(message = "Subscription ID is required")
    private Long subscriptionId;

    @NotBlank(message = "Cancellation reason is required")
    private String reason;

    private boolean cancelImmediately = false;

    private String cancellationDate;

    private String cancellationPolicy; // EndOfCurrentTerm, EndOfLastInvoicePeriod, SpecificDate

    // Getters and Setters
    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public boolean isCancelImmediately() { return cancelImmediately; }
    public void setCancelImmediately(boolean cancelImmediately) { this.cancelImmediately = cancelImmediately; }

    public String getCancellationDate() { return cancellationDate; }
    public void setCancellationDate(String cancellationDate) { this.cancellationDate = cancellationDate; }

    public String getCancellationPolicy() { return cancellationPolicy; }
    public void setCancellationPolicy(String cancellationPolicy) { this.cancellationPolicy = cancellationPolicy; }
}
