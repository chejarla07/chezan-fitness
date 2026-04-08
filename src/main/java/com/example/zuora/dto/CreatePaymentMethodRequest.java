package com.example.zuora.dto;

import jakarta.validation.constraints.NotBlank;

public class CreatePaymentMethodRequest {

    @NotBlank(message = "Payment method type is required")
    private String type; // CreditCard, ACH, PayPal, ApplePay, GooglePay

    // Credit card fields
    private String cardNumber;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String cardHolderName;
    private String securityCode;

    // ACH fields
    private String bankName;
    private String routingNumber;
    private String accountNumber;
    private String accountType; // Checking, Savings

    // PayPal fields
    private String paypalEmail;
    private String paypalBaid; // PayPal Billing Agreement ID

    // Apple Pay / Google Pay
    private String paymentToken;
    private String billingFirstName;
    private String billingLastName;
    private String billingAddress1;
    private String billingAddress2;
    private String billingCity;
    private String billingState;
    private String billingCountry;
    private String billingZipCode;

    private boolean setAsDefault;

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public Integer getExpiryMonth() { return expiryMonth; }
    public void setExpiryMonth(Integer expiryMonth) { this.expiryMonth = expiryMonth; }

    public Integer getExpiryYear() { return expiryYear; }
    public void setExpiryYear(Integer expiryYear) { this.expiryYear = expiryYear; }

    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }

    public String getSecurityCode() { return securityCode; }
    public void setSecurityCode(String securityCode) { this.securityCode = securityCode; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getRoutingNumber() { return routingNumber; }
    public void setRoutingNumber(String routingNumber) { this.routingNumber = routingNumber; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getPaypalEmail() { return paypalEmail; }
    public void setPaypalEmail(String paypalEmail) { this.paypalEmail = paypalEmail; }

    public String getPaypalBaid() { return paypalBaid; }
    public void setPaypalBaid(String paypalBaid) { this.paypalBaid = paypalBaid; }

    public String getPaymentToken() { return paymentToken; }
    public void setPaymentToken(String paymentToken) { this.paymentToken = paymentToken; }

    public String getBillingFirstName() { return billingFirstName; }
    public void setBillingFirstName(String billingFirstName) { this.billingFirstName = billingFirstName; }

    public String getBillingLastName() { return billingLastName; }
    public void setBillingLastName(String billingLastName) { this.billingLastName = billingLastName; }

    public String getBillingAddress1() { return billingAddress1; }
    public void setBillingAddress1(String billingAddress1) { this.billingAddress1 = billingAddress1; }

    public String getBillingAddress2() { return billingAddress2; }
    public void setBillingAddress2(String billingAddress2) { this.billingAddress2 = billingAddress2; }

    public String getBillingCity() { return billingCity; }
    public void setBillingCity(String billingCity) { this.billingCity = billingCity; }

    public String getBillingState() { return billingState; }
    public void setBillingState(String billingState) { this.billingState = billingState; }

    public String getBillingCountry() { return billingCountry; }
    public void setBillingCountry(String billingCountry) { this.billingCountry = billingCountry; }

    public String getBillingZipCode() { return billingZipCode; }
    public void setBillingZipCode(String billingZipCode) { this.billingZipCode = billingZipCode; }

    public boolean isSetAsDefault() { return setAsDefault; }
    public void setSetAsDefault(boolean setAsDefault) { this.setAsDefault = setAsDefault; }
}