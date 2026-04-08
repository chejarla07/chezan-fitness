package com.example.zuora.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zuora_payment_id")
    private String zuoraPaymentId;

    @Column(name = "zuora_payment_number")
    private String zuoraPaymentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private Double amount;

    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum PaymentStatus {
        Processing, Processed, Error, Voided
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (paymentDate == null) {
            paymentDate = LocalDate.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getZuoraPaymentId() { return zuoraPaymentId; }
    public void setZuoraPaymentId(String zuoraPaymentId) { this.zuoraPaymentId = zuoraPaymentId; }

    public String getZuoraPaymentNumber() { return zuoraPaymentNumber; }
    public void setZuoraPaymentNumber(String zuoraPaymentNumber) { this.zuoraPaymentNumber = zuoraPaymentNumber; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public String getGatewayResponse() { return gatewayResponse; }
    public void setGatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isSuccessful() {
        return status == PaymentStatus.Processed;
    }
}
