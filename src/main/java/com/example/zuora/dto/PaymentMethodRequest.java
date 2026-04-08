package com.example.zuora.dto;

import jakarta.validation.constraints.*;

public class PaymentMethodRequest {

    @NotBlank(message = "Payment method type is required")
    private String type;

    private String cardToken;

    @Min(value = 1, message = "Invalid expiration month")
    @Max(value = 12, message = "Invalid expiration month")
    private Integer expirationMonth;

    @Min(value = 2024, message = "Invalid expiration year")
    private Integer expirationYear;

    private String lastFour;

    private String brand;

    private boolean makeDefault = false;

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCardToken() { return cardToken; }
    public void setCardToken(String cardToken) { this.cardToken = cardToken; }

    public Integer getExpirationMonth() { return expirationMonth; }
    public void setExpirationMonth(Integer expirationMonth) { this.expirationMonth = expirationMonth; }

    public Integer getExpirationYear() { return expirationYear; }
    public void setExpirationYear(Integer expirationYear) { this.expirationYear = expirationYear; }

    public String getLastFour() { return lastFour; }
    public void setLastFour(String lastFour) { this.lastFour = lastFour; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public boolean isMakeDefault() { return makeDefault; }
    public void setMakeDefault(boolean makeDefault) { this.makeDefault = makeDefault; }
}
