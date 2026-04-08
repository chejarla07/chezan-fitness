package com.example.zuora.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Rate plan name is required")
    private String ratePlanName;

    private String ratePlanDescription;

    @NotBlank(message = "Billing period is required")
    private String billingPeriod;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    private String currency = "USD";

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getRatePlanName() { return ratePlanName; }
    public void setRatePlanName(String ratePlanName) { this.ratePlanName = ratePlanName; }

    public String getRatePlanDescription() { return ratePlanDescription; }
    public void setRatePlanDescription(String ratePlanDescription) { this.ratePlanDescription = ratePlanDescription; }

    public String getBillingPeriod() { return billingPeriod; }
    public void setBillingPeriod(String billingPeriod) { this.billingPeriod = billingPeriod; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
