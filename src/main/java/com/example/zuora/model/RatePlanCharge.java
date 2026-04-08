package com.example.zuora.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rate_plan_charges")
public class RatePlanCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zuora_charge_id")
    private String zuoraChargeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_plan_id", nullable = false)
    private RatePlan ratePlan;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", nullable = false)
    private ChargeType chargeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_model")
    private ChargeModel chargeModel;

    private Double amount;

    private String currency = "USD";

    @Column(name = "billing_timing")
    @Enumerated(EnumType.STRING)
    private BillingTiming billingTiming;

    @Column(name = "billing_day")
    private String billingDay;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum ChargeType {
        Recurring, OneTime, Usage
    }

    public enum ChargeModel {
        FlatFee, PerUnit, Tiered
    }

    public enum BillingTiming {
        InAdvance, InArrears
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getZuoraChargeId() { return zuoraChargeId; }
    public void setZuoraChargeId(String zuoraChargeId) { this.zuoraChargeId = zuoraChargeId; }

    public RatePlan getRatePlan() { return ratePlan; }
    public void setRatePlan(RatePlan ratePlan) { this.ratePlan = ratePlan; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ChargeType getChargeType() { return chargeType; }
    public void setChargeType(ChargeType chargeType) { this.chargeType = chargeType; }

    public ChargeModel getChargeModel() { return chargeModel; }
    public void setChargeModel(ChargeModel chargeModel) { this.chargeModel = chargeModel; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BillingTiming getBillingTiming() { return billingTiming; }
    public void setBillingTiming(BillingTiming billingTiming) { this.billingTiming = billingTiming; }

    public String getBillingDay() { return billingDay; }
    public void setBillingDay(String billingDay) { this.billingDay = billingDay; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
