package com.example.zuora.dto;

import com.example.zuora.model.RatePlan.BillingPeriod;

public class UpdateRatePlanRequest {
    private Long ratePlanId;
    private String name;
    private String description;
    private BillingPeriod billingPeriod;
    private Double price;

    // Getters and Setters
    public Long getRatePlanId() {
        return ratePlanId;
    }

    public void setRatePlanId(Long ratePlanId) {
        this.ratePlanId = ratePlanId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BillingPeriod getBillingPeriod() {
        return billingPeriod;
    }

    public void setBillingPeriod(BillingPeriod billingPeriod) {
        this.billingPeriod = billingPeriod;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
}