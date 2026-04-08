package com.example.zuora.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zuora_subscription_id")
    private String zuoraSubscriptionId;

    @Column(name = "zuora_subscription_number")
    private String zuoraSubscriptionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "account_id")
    private Long accountId;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status = SubscriptionStatus.Active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_plan_id", nullable = false)
    private RatePlan ratePlan;

    @Column(name = "contract_effective_date", nullable = false)
    private LocalDate contractEffectiveDate;

    @Column(name = "service_activation_date")
    private LocalDate serviceActivationDate;

    @Column(name = "term_start_date", nullable = false)
    private LocalDate termStartDate;

    @Column(name = "term_end_date")
    private LocalDate termEndDate;

    @Column(name = "initial_term")
    private Integer initialTerm;

    @Column(name = "initial_term_period_type")
    private String initialTermPeriodType;

    @Column(name = "renewal_term")
    private Integer renewalTerm;

    @Column(name = "auto_renew")
    private Boolean autoRenew = true;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "cancellation_date")
    private LocalDate cancellationDate;

    @Column(name = "suspend_end_date")
    private LocalDate suspendEndDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum SubscriptionStatus {
        Active, Cancelled, Suspended, Expired
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (contractEffectiveDate == null) {
            contractEffectiveDate = LocalDate.now();
        }
        if (termStartDate == null) {
            termStartDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getZuoraSubscriptionId() { return zuoraSubscriptionId; }
    public void setZuoraSubscriptionId(String zuoraSubscriptionId) { this.zuoraSubscriptionId = zuoraSubscriptionId; }

    public String getZuoraSubscriptionNumber() { return zuoraSubscriptionNumber; }
    public void setZuoraSubscriptionNumber(String zuoraSubscriptionNumber) { this.zuoraSubscriptionNumber = zuoraSubscriptionNumber; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }

    public RatePlan getRatePlan() { return ratePlan; }
    public void setRatePlan(RatePlan ratePlan) { this.ratePlan = ratePlan; }

    public LocalDate getContractEffectiveDate() { return contractEffectiveDate; }
    public void setContractEffectiveDate(LocalDate contractEffectiveDate) { this.contractEffectiveDate = contractEffectiveDate; }

    public LocalDate getServiceActivationDate() { return serviceActivationDate; }
    public void setServiceActivationDate(LocalDate serviceActivationDate) { this.serviceActivationDate = serviceActivationDate; }

    public LocalDate getTermStartDate() { return termStartDate; }
    public void setTermStartDate(LocalDate termStartDate) { this.termStartDate = termStartDate; }

    public LocalDate getTermEndDate() { return termEndDate; }
    public void setTermEndDate(LocalDate termEndDate) { this.termEndDate = termEndDate; }

    public Integer getInitialTerm() { return initialTerm; }
    public void setInitialTerm(Integer initialTerm) { this.initialTerm = initialTerm; }

    public String getInitialTermPeriodType() { return initialTermPeriodType; }
    public void setInitialTermPeriodType(String initialTermPeriodType) { this.initialTermPeriodType = initialTermPeriodType; }

    public Integer getRenewalTerm() { return renewalTerm; }
    public void setRenewalTerm(Integer renewalTerm) { this.renewalTerm = renewalTerm; }

    public Boolean getAutoRenew() { return autoRenew; }
    public void setAutoRenew(Boolean autoRenew) { this.autoRenew = autoRenew; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public LocalDate getCancellationDate() { return cancellationDate; }
    public void setCancellationDate(LocalDate cancellationDate) { this.cancellationDate = cancellationDate; }

    public LocalDate getSuspendEndDate() { return suspendEndDate; }
    public void setSuspendEndDate(LocalDate suspendEndDate) { this.suspendEndDate = suspendEndDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() {
        return status == SubscriptionStatus.Active;
    }

    public String getPlanName() {
        return ratePlan != null ? ratePlan.getName() : "Unknown Plan";
    }

    public Double getMonthlyPrice() {
        if (ratePlan == null || ratePlan.getRecurringCharge() == null) {
            return 0.0;
        }
        return ratePlan.getPrice();
    }
}
