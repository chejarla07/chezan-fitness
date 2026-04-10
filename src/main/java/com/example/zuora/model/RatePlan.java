package com.example.zuora.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rate_plans")
public class RatePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zuora_rate_plan_id")
    private String zuoraRatePlanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private Status status = Status.Active;

    @Column(name = "effective_start_date")
    private LocalDate effectiveStartDate;

    @Column(name = "effective_end_date")
    private LocalDate effectiveEndDate;

    @Column(name = "billing_period")
    @Enumerated(EnumType.STRING)
    private BillingPeriod billingPeriod;

    @Column(name = "billing_period_value")
    private Integer billingPeriodValue;

    @Column(name = "discount_eligible")
    private Boolean discountEligible = true;

    @OneToMany(mappedBy = "ratePlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RatePlanCharge> charges = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Status {
        Active, Inactive
    }

    public enum BillingPeriod {
        Month, Quarter, Annual
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getZuoraRatePlanId() { return zuoraRatePlanId; }
    public void setZuoraRatePlanId(String zuoraRatePlanId) { this.zuoraRatePlanId = zuoraRatePlanId; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDate getEffectiveStartDate() { return effectiveStartDate; }
    public void setEffectiveStartDate(LocalDate effectiveStartDate) { this.effectiveStartDate = effectiveStartDate; }

    public LocalDate getEffectiveEndDate() { return effectiveEndDate; }
    public void setEffectiveEndDate(LocalDate effectiveEndDate) { this.effectiveEndDate = effectiveEndDate; }

    public BillingPeriod getBillingPeriod() { return billingPeriod; }
    public void setBillingPeriod(BillingPeriod billingPeriod) { this.billingPeriod = billingPeriod; }

    public Integer getBillingPeriodValue() { return billingPeriodValue; }
    public void setBillingPeriodValue(Integer billingPeriodValue) { this.billingPeriodValue = billingPeriodValue; }

    public List<RatePlanCharge> getCharges() { return charges; }
    public void setCharges(List<RatePlanCharge> charges) { this.charges = charges; }

    public void addCharge(RatePlanCharge charge) {
        charges.add(charge);
        charge.setRatePlan(this);
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public RatePlanCharge getRecurringCharge() {
        return charges.stream()
            .filter(c -> c.getChargeType() == RatePlanCharge.ChargeType.Recurring)
            .findFirst()
            .orElse(null);
    }

    public Double getPrice() {
        RatePlanCharge charge = getRecurringCharge();
        return charge != null ? charge.getAmount() : 0.0;
    }

    public Boolean getDiscountEligible() { return discountEligible; }
    public void setDiscountEligible(Boolean discountEligible) { this.discountEligible = discountEligible; }
}
