package com.example.zuora.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class UpdatePriceRequest {

    @NotNull(message = "Rate plan ID is required")
    private Long ratePlanId;

    @NotNull(message = "New price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal newPrice;

    private String reason;

    // Getters and Setters
    public Long getRatePlanId() { return ratePlanId; }
    public void setRatePlanId(Long ratePlanId) { this.ratePlanId = ratePlanId; }

    public BigDecimal getNewPrice() { return newPrice; }
    public void setNewPrice(BigDecimal newPrice) { this.newPrice = newPrice; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
