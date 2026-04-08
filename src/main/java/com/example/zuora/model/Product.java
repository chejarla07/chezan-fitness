package com.example.zuora.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zuora_product_id")
    private String zuoraProductId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    private Status status = Status.Active;

    @Column(name = "effective_start_date")
    private LocalDate effectiveStartDate;

    @Column(name = "effective_end_date")
    private LocalDate effectiveEndDate;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RatePlan> ratePlans = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Category {
        MEMBERSHIP, CLASS, ADD_ON, PERSONAL_TRAINING, SERVICE, PRODUCT
    }

    public enum Status {
        Active, Inactive
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (effectiveStartDate == null) {
            effectiveStartDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getZuoraProductId() { return zuoraProductId; }
    public void setZuoraProductId(String zuoraProductId) { this.zuoraProductId = zuoraProductId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDate getEffectiveStartDate() { return effectiveStartDate; }
    public void setEffectiveStartDate(LocalDate effectiveStartDate) { this.effectiveStartDate = effectiveStartDate; }

    public LocalDate getEffectiveEndDate() { return effectiveEndDate; }
    public void setEffectiveEndDate(LocalDate effectiveEndDate) { this.effectiveEndDate = effectiveEndDate; }

    public List<RatePlan> getRatePlans() { return ratePlans; }
    public void setRatePlans(List<RatePlan> ratePlans) { this.ratePlans = ratePlans; }

    public void addRatePlan(RatePlan ratePlan) {
        ratePlans.add(ratePlan);
        ratePlan.setProduct(this);
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() {
        return status == Status.Active;
    }
}
