package com.example.zuora.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zuora_invoice_id")
    private String zuoraInvoiceId;

    @Column(name = "zuora_invoice_number")
    private String zuoraInvoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "account_id")
    private Long accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(nullable = false)
    private Double amount;

    private Double balance;

    @Column(name = "tax_amount")
    private Double taxAmount;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum InvoiceStatus {
        Draft, Posted, Paid, Voided, WriteOff
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (invoiceDate == null) {
            invoiceDate = LocalDate.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getZuoraInvoiceId() { return zuoraInvoiceId; }
    public void setZuoraInvoiceId(String zuoraInvoiceId) { this.zuoraInvoiceId = zuoraInvoiceId; }

    public String getZuoraInvoiceNumber() { return zuoraInvoiceNumber; }
    public void setZuoraInvoiceNumber(String zuoraInvoiceNumber) { this.zuoraInvoiceNumber = zuoraInvoiceNumber; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Subscription getSubscription() { return subscription; }
    public void setSubscription(Subscription subscription) { this.subscription = subscription; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }

    public Double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(Double taxAmount) { this.taxAmount = taxAmount; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }

    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDate getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDate paidDate) { this.paidDate = paidDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isPaid() {
        return status == InvoiceStatus.Paid;
    }

    public boolean isOverdue() {
        return !isPaid() && dueDate != null && dueDate.isBefore(LocalDate.now());
    }

    public String getInvoiceNumber() {
        return zuoraInvoiceNumber != null ? zuoraInvoiceNumber : "INV-" + id;
    }
}
