package com.example.zuora.dto;

import java.time.LocalDate;

public class CancellationOptions {

    private String policy;
    private String label;
    private LocalDate effectiveDate;
    private Double estimatedCredit;
    private Double invoiceBalance;
    private Double debitAmount;
    private String description;

    public CancellationOptions(String policy, String label, LocalDate effectiveDate, Double estimatedCredit) {
        this.policy = policy;
        this.label = label;
        this.effectiveDate = effectiveDate;
        this.estimatedCredit = estimatedCredit;
    }

    public CancellationOptions(String policy, String label, LocalDate effectiveDate, Double estimatedCredit,
                               Double invoiceBalance, Double debitAmount, String description) {
        this.policy = policy;
        this.label = label;
        this.effectiveDate = effectiveDate;
        this.estimatedCredit = estimatedCredit;
        this.invoiceBalance = invoiceBalance;
        this.debitAmount = debitAmount;
        this.description = description;
    }

    // Getters
    public String getPolicy() { return policy; }
    public String getLabel() { return label; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public Double getEstimatedCredit() { return estimatedCredit; }
    public Double getInvoiceBalance() { return invoiceBalance; }
    public Double getDebitAmount() { return debitAmount; }
    public String getDescription() { return description; }

    // Setters
    public void setInvoiceBalance(Double invoiceBalance) { this.invoiceBalance = invoiceBalance; }
    public void setDebitAmount(Double debitAmount) { this.debitAmount = debitAmount; }
    public void setDescription(String description) { this.description = description; }
}
