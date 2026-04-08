package com.example.zuora.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "discounts")
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zuora_discount_id")
    private String zuoraDiscountId;

    @Column(name = "discount_code", unique = true)
    private String discountCode;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_percentage")
    private Double discountPercentage;

    @Column(name = "discount_amount")
    private Double discountAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_level")
    private DiscountLevel discountLevel = DiscountLevel.SUBSCRIPTION;

    @Enumerated(EnumType.STRING)
    @Column(name = "apply_to")
    private ApplyTo applyTo = ApplyTo.RECURRING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_plan_id")
    private RatePlan ratePlan;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    @Column(name = "current_redemptions")
    private Integer currentRedemptions = 0;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum DiscountType {
        PERCENTAGE, FIXED_AMOUNT
    }

    public enum DiscountLevel {
        RATEPLAN, SUBSCRIPTION, ACCOUNT
    }

    public enum ApplyTo {
        ONETIME, RECURRING, USAGE, ALL
    }

    public enum Status {
        ACTIVE, INACTIVE, EXPIRED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (startDate == null) {
            startDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getZuoraDiscountId() { return zuoraDiscountId; }
    public void setZuoraDiscountId(String zuoraDiscountId) { this.zuoraDiscountId = zuoraDiscountId; }

    public String getDiscountCode() { return discountCode; }
    public void setDiscountCode(String discountCode) { this.discountCode = discountCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }

    public Double getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(Double discountPercentage) { this.discountPercentage = discountPercentage; }

    public Double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(Double discountAmount) { this.discountAmount = discountAmount; }

    public DiscountLevel getDiscountLevel() { return discountLevel; }
    public void setDiscountLevel(DiscountLevel discountLevel) { this.discountLevel = discountLevel; }

    public ApplyTo getApplyTo() { return applyTo; }
    public void setApplyTo(ApplyTo applyTo) { this.applyTo = applyTo; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public RatePlan getRatePlan() { return ratePlan; }
    public void setRatePlan(RatePlan ratePlan) { this.ratePlan = ratePlan; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Integer getMaxRedemptions() { return maxRedemptions; }
    public void setMaxRedemptions(Integer maxRedemptions) { this.maxRedemptions = maxRedemptions; }

    public Integer getCurrentRedemptions() { return currentRedemptions; }
    public void setCurrentRedemptions(Integer currentRedemptions) { this.currentRedemptions = currentRedemptions; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() {
        if (status != Status.ACTIVE) return false;
        LocalDate now = LocalDate.now();
        if (startDate != null && now.isBefore(startDate)) return false;
        if (endDate != null && now.isAfter(endDate)) return false;
        if (maxRedemptions != null && currentRedemptions >= maxRedemptions) return false;
        return true;
    }

    public Double getDiscountValue() {
        if (discountType == DiscountType.PERCENTAGE) {
            return discountPercentage;
        }
        return discountAmount;
    }
}