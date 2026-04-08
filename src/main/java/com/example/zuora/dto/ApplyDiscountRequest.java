package com.example.zuora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ApplyDiscountRequest {

    @NotNull(message = "Discount ID is required")
    private Long discountId;

    @NotBlank(message = "Subscription ID is required")
    private String subscriptionId;

    private String startDate; // yyyy-MM-dd - when discount starts

    private String endDate; // yyyy-MM-dd - when discount ends

    private Integer durationPeriods; // Number of periods for discount

    private String durationPeriodType; // Day, Week, Month, Year

    // Getters and Setters
    public Long getDiscountId() { return discountId; }
    public void setDiscountId(Long discountId) { this.discountId = discountId; }

    public String getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(String subscriptionId) { this.subscriptionId = subscriptionId; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public Integer getDurationPeriods() { return durationPeriods; }
    public void setDurationPeriods(Integer durationPeriods) { this.durationPeriods = durationPeriods; }

    public String getDurationPeriodType() { return durationPeriodType; }
    public void setDurationPeriodType(String durationPeriodType) { this.durationPeriodType = durationPeriodType; }
}