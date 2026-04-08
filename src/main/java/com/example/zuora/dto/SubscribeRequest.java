package com.example.zuora.dto;

import jakarta.validation.constraints.*;

public class SubscribeRequest {

    @NotNull(message = "Please select a plan")
    private Long ratePlanId;

    // Optional start date for the subscription
    private String startDate;

    private String cardToken;

    // Getters and Setters
    public Long getRatePlanId() { return ratePlanId; }
    public void setRatePlanId(Long ratePlanId) { this.ratePlanId = ratePlanId; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getCardToken() { return cardToken; }
    public void setCardToken(String cardToken) { this.cardToken = cardToken; }
}
