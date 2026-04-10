package com.example.zuora.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public class SubscribeRequest {

    @NotNull(message = "Please select a plan")
    private Long ratePlanId;

    // Optional start date for the subscription
    private String startDate;

    private String cardToken;

    // Optional add-on rate plan IDs
    private List<Long> addOnRatePlanIds;

    // Getters and Setters
    public Long getRatePlanId() { return ratePlanId; }
    public void setRatePlanId(Long ratePlanId) { this.ratePlanId = ratePlanId; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getCardToken() { return cardToken; }
    public void setCardToken(String cardToken) { this.cardToken = cardToken; }

    public List<Long> getAddOnRatePlanIds() { return addOnRatePlanIds; }
    public void setAddOnRatePlanIds(List<Long> addOnRatePlanIds) { this.addOnRatePlanIds = addOnRatePlanIds; }
}
