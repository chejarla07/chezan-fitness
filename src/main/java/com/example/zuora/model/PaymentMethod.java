package com.example.zuora.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_methods")
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zuora_payment_method_id")
    private String zuoraPaymentMethodId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "account_id")
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "card_last_four")
    private String cardLastFour;

    @Column(name = "card_brand")
    private String cardBrand;

    @Column(name = "card_expiration_month")
    private Integer cardExpirationMonth;

    @Column(name = "card_expiration_year")
    private Integer cardExpirationYear;

    @Column(name = "ach_bank_name")
    private String achBankName;

    @Column(name = "ach_account_last_four")
    private String achAccountLastFour;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum PaymentType {
        CreditCard, ACH, PayPal, ApplePay, GooglePay
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getZuoraPaymentMethodId() { return zuoraPaymentMethodId; }
    public void setZuoraPaymentMethodId(String zuoraPaymentMethodId) { this.zuoraPaymentMethodId = zuoraPaymentMethodId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public PaymentType getType() { return type; }
    public void setType(PaymentType type) { this.type = type; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }

    public String getCardLastFour() { return cardLastFour; }
    public void setCardLastFour(String cardLastFour) { this.cardLastFour = cardLastFour; }

    public String getCardBrand() { return cardBrand; }
    public void setCardBrand(String cardBrand) { this.cardBrand = cardBrand; }

    public Integer getCardExpirationMonth() { return cardExpirationMonth; }
    public void setCardExpirationMonth(Integer cardExpirationMonth) { this.cardExpirationMonth = cardExpirationMonth; }

    public Integer getCardExpirationYear() { return cardExpirationYear; }
    public void setCardExpirationYear(Integer cardExpirationYear) { this.cardExpirationYear = cardExpirationYear; }

    public String getAchBankName() { return achBankName; }
    public void setAchBankName(String achBankName) { this.achBankName = achBankName; }

    public String getAchAccountLastFour() { return achAccountLastFour; }
    public void setAchAccountLastFour(String achAccountLastFour) { this.achAccountLastFour = achAccountLastFour; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getMaskedCardNumber() {
        if (cardLastFour != null && !cardLastFour.isEmpty()) {
            return "**** **** **** " + cardLastFour;
        }
        return null;
    }

    public String getExpirationDisplay() {
        if (cardExpirationMonth != null && cardExpirationYear != null) {
            return String.format("%02d/%d", cardExpirationMonth, cardExpirationYear);
        }
        return null;
    }

    public boolean isCreditCard() {
        return type == PaymentType.CreditCard;
    }
}
