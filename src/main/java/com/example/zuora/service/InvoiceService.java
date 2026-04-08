package com.example.zuora.service;

import com.example.zuora.model.*;
import com.example.zuora.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ZuoraApiService zuoraApiService;

    public InvoiceService(InvoiceRepository invoiceRepository,
                         PaymentRepository paymentRepository,
                         ZuoraApiService zuoraApiService) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.zuoraApiService = zuoraApiService;
    }

    public List<Invoice> getUserInvoices(Long userId) {
        return invoiceRepository.findByUserId(userId);
    }

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public List<Invoice> getUserInvoicesByStatus(Long userId, Invoice.InvoiceStatus status) {
        return invoiceRepository.findByUserIdAndStatus(userId, status);
    }

    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + id));
    }

    public List<Payment> getUserPayments(Long userId) {
        return paymentRepository.findByUserId(userId);
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));
    }

    /**
     * Sync invoices from Zuora
     * Uses accountNumber (preferred) or accountId to fetch invoices
     */
    public void syncInvoicesFromZuora(User user) throws Exception {
        // Try account number first (more reliable), fallback to account ID
        String accountKey = user.getZuoraAccountNumber() != null ?
                user.getZuoraAccountNumber() : user.getZuoraAccountId();

        if (accountKey == null) {
            System.out.println("Skipping invoice sync: No Zuora account identifier for user " + user.getEmail());
            return;
        }

        System.out.println("Syncing invoices from Zuora for account: " + accountKey);
        JsonNode zuoraResponse = zuoraApiService.getInvoices(accountKey);

        System.out.println("Zuora response structure: " + zuoraResponse.toString().substring(0, Math.min(1000, zuoraResponse.toString().length())));

        // Handle different response structures
        // Zuora Billing Documents API returns "documents" array
        JsonNode invoicesNode = null;

        if (zuoraResponse.has("documents")) {
            invoicesNode = zuoraResponse.get("documents");
            System.out.println("Found 'documents' array in billing documents response");
        } else if (zuoraResponse.has("invoices")) {
            invoicesNode = zuoraResponse.get("invoices");
            System.out.println("Found 'invoices' array in response");
        } else if (zuoraResponse.isArray()) {
            // Sometimes Zuora returns a direct array
            invoicesNode = zuoraResponse;
            System.out.println("Response is a direct array");
        } else if (zuoraResponse.has("data") && zuoraResponse.get("data").isArray()) {
            // Some APIs wrap in 'data'
            invoicesNode = zuoraResponse.get("data");
            System.out.println("Found 'data' array in response");
        } else {
            System.out.println("No invoices found in response structure. Keys: " + zuoraResponse.fieldNames().toString());
        }

        if (invoicesNode != null && invoicesNode.isArray()) {
            System.out.println("Processing " + invoicesNode.size() + " invoices from Zuora");

            for (JsonNode inv : invoicesNode) {
                try {
                    String zuoraInvoiceId = inv.has("id") ? inv.get("id").asText() : null;

                    if (zuoraInvoiceId == null) {
                        System.out.println("Skipping invoice - no ID found");
                        continue;
                    }

                    // Check if already exists
                    if (invoiceRepository.findByZuoraInvoiceId(zuoraInvoiceId).isPresent()) {
                        System.out.println("Invoice already exists: " + zuoraInvoiceId);
                        continue;
                    }

                    // Check document type - only process invoices (not credit memos or debit memos)
                    String documentType = inv.has("documentType") ? inv.get("documentType").asText() : "Invoice";
                    if (!"Invoice".equals(documentType)) {
                        System.out.println("Skipping non-invoice document: " + documentType + " - ID: " + zuoraInvoiceId);
                        continue;
                    }

                    Invoice invoice = new Invoice();
                    invoice.setUser(user);
                    invoice.setZuoraInvoiceId(zuoraInvoiceId);

                    // Billing Documents API uses documentNumber, fallback to other field names
                    String invoiceNumber = null;
                    if (inv.has("documentNumber")) invoiceNumber = inv.get("documentNumber").asText();
                    else if (inv.has("invoiceNumber")) invoiceNumber = inv.get("invoiceNumber").asText();
                    else if (inv.has("InvoiceNumber")) invoiceNumber = inv.get("InvoiceNumber").asText();
                    invoice.setZuoraInvoiceNumber(invoiceNumber);

                    // Try different field names for amount
                    Double amount = 0.0;
                    if (inv.has("amount")) amount = inv.get("amount").asDouble();
                    else if (inv.has("Amount")) amount = inv.get("Amount").asDouble();
                    else if (inv.has("totalAmount")) amount = inv.get("totalAmount").asDouble();
                    else if (inv.has("TotalAmount")) amount = inv.get("TotalAmount").asDouble();
                    invoice.setAmount(amount);
                    invoice.setTotalAmount(amount);

                    // Balance
                    Double balance = 0.0;
                    if (inv.has("balance")) balance = inv.get("balance").asDouble();
                    else if (inv.has("Balance")) balance = inv.get("Balance").asDouble();
                    invoice.setBalance(balance);

                    // Tax amount
                    Double taxAmount = 0.0;
                    if (inv.has("taxAmount")) taxAmount = inv.get("taxAmount").asDouble();
                    else if (inv.has("TaxAmount")) taxAmount = inv.get("TaxAmount").asDouble();
                    invoice.setTaxAmount(taxAmount);

                    // Status
                    String status = "Draft";
                    if (inv.has("status")) status = inv.get("status").asText();
                    else if (inv.has("Status")) status = inv.get("Status").asText();
                    invoice.setStatus(parseInvoiceStatus(status));

                    // Invoice Date - Billing Documents API uses documentDate
                    String dateStr = null;
                    if (inv.has("documentDate")) dateStr = inv.get("documentDate").asText();
                    else if (inv.has("invoiceDate")) dateStr = inv.get("invoiceDate").asText();
                    else if (inv.has("InvoiceDate")) dateStr = inv.get("InvoiceDate").asText();

                    if (dateStr != null) {
                        invoice.setInvoiceDate(parseDate(dateStr));
                    }

                    // Due Date - not available in Billing Documents API, use documentDate as fallback
                    if (inv.has("dueDate")) {
                        invoice.setDueDate(parseDate(inv.get("dueDate").asText()));
                    } else if (inv.has("DueDate")) {
                        invoice.setDueDate(parseDate(inv.get("DueDate").asText()));
                    } else if (dateStr != null) {
                        // Billing Documents API doesn't have dueDate, use documentDate as fallback
                        invoice.setDueDate(parseDate(dateStr));
                    }

                    invoiceRepository.save(invoice);
                    System.out.println("Saved invoice: " + zuoraInvoiceId + " - " + invoice.getZuoraInvoiceNumber());
                } catch (Exception e) {
                    System.err.println("Error processing invoice: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("No invoices array found in response");
        }
    }

    private LocalDate parseDate(String dateStr) {
        try {
            // Try ISO_DATE_TIME format (2024-01-15T00:00:00.000Z)
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e1) {
            try {
                // Try ISO_DATE format (2024-01-15)
                return LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
            } catch (Exception e2) {
                try {
                    // Try with just date part
                    return LocalDate.parse(dateStr.substring(0, 10));
                } catch (Exception e3) {
                    System.err.println("Could not parse date: " + dateStr);
                    return LocalDate.now();
                }
            }
        }
    }

    /**
     * Sync payments from Zuora
     * Uses accountNumber (preferred) or accountId to fetch payments
     */
    public void syncPaymentsFromZuora(User user) throws Exception {
        // Try account number first (more reliable), fallback to account ID
        String accountKey = user.getZuoraAccountNumber() != null ?
                user.getZuoraAccountNumber() : user.getZuoraAccountId();

        if (accountKey == null) {
            System.out.println("Skipping payment sync: No Zuora account identifier for user " + user.getEmail());
            return;
        }

        System.out.println("Syncing payments from Zuora for account: " + accountKey);
        JsonNode zuoraPayments = zuoraApiService.getPayments(accountKey);

        if (zuoraPayments.has("payments")) {
            for (JsonNode pay : zuoraPayments.get("payments")) {
                String zuoraPaymentId = pay.get("id").asText();

                // Check if already exists
                if (paymentRepository.findByZuoraPaymentId(zuoraPaymentId).isPresent()) {
                    continue;
                }

                Payment payment = new Payment();
                payment.setUser(user);
                payment.setZuoraPaymentId(zuoraPaymentId);
                payment.setZuoraPaymentNumber(pay.has("paymentNumber") ? pay.get("paymentNumber").asText() : null);
                payment.setAmount(pay.has("amount") ? pay.get("amount").asDouble() : 0.0);
                payment.setCurrency(pay.has("currency") ? pay.get("currency").asText() : "USD");
                payment.setStatus(parsePaymentStatus(pay.has("status") ? pay.get("status").asText() : "Processing"));

                if (pay.has("paymentDate")) {
                    payment.setPaymentDate(LocalDate.parse(pay.get("paymentDate").asText(), DateTimeFormatter.ISO_DATE));
                }

                if (pay.has("reference")) {
                    payment.setReferenceNumber(pay.get("reference").asText());
                }

                paymentRepository.save(payment);
            }
        }
    }

    private Invoice.InvoiceStatus parseInvoiceStatus(String status) {
        return switch (status.toLowerCase()) {
            case "posted" -> Invoice.InvoiceStatus.Posted;
            case "paid" -> Invoice.InvoiceStatus.Paid;
            case "voided" -> Invoice.InvoiceStatus.Voided;
            case "writeoff" -> Invoice.InvoiceStatus.WriteOff;
            default -> Invoice.InvoiceStatus.Draft;
        };
    }

    private Payment.PaymentStatus parsePaymentStatus(String status) {
        return switch (status.toLowerCase()) {
            case "processed" -> Payment.PaymentStatus.Processed;
            case "error" -> Payment.PaymentStatus.Error;
            case "voided" -> Payment.PaymentStatus.Voided;
            default -> Payment.PaymentStatus.Processing;
        };
    }

    /**
     * Get invoice PDF from Zuora using invoice number
     * @param invoiceNumber The Zuora invoice number (e.g., INV00000073)
     */
    public byte[] getInvoicePdf(String invoiceNumber) throws Exception {
        return zuoraApiService.getInvoicePdf(invoiceNumber);
    }
}
