package com.example.zuora.service;

import com.example.zuora.dto.CreatePaymentRequest;
import com.example.zuora.dto.RefundRequest;
import com.example.zuora.dto.CreatePaymentMethodRequest;
import com.example.zuora.model.*;
import com.example.zuora.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final ZuoraApiService zuoraApiService;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentMethodRepository paymentMethodRepository,
                          InvoiceRepository invoiceRepository,
                          UserRepository userRepository,
                          ZuoraApiService zuoraApiService) {
        this.paymentRepository = paymentRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.zuoraApiService = zuoraApiService;
        this.objectMapper = new ObjectMapper();
    }

    // ========== Payment Methods ==========

    /**
     * Get all payment methods for a user
     */
    public List<PaymentMethod> getUserPaymentMethods(Long userId) {
        return paymentMethodRepository.findByUserId(userId);
    }

    /**
     * Get a specific payment method
     */
    public PaymentMethod getPaymentMethod(Long id) {
        return paymentMethodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment method not found: " + id));
    }

    /**
     * Create a payment method in Zuora and store locally
     */
    @Transactional
    public PaymentMethod createPaymentMethod(Long userId, CreatePaymentMethodRequest request) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (user.getZuoraAccountId() == null) {
            throw new RuntimeException("User does not have a Zuora account");
        }

        // Create payment method in Zuora
        JsonNode zuoraResponse = zuoraApiService.createPaymentMethod(
                user.getZuoraAccountId(),
                null, // Token would come from hosted payment page
                PaymentMethod.PaymentType.valueOf(request.getType().toUpperCase())
        );

        String zuoraPaymentMethodId = zuoraResponse.has("id") ?
                zuoraResponse.get("id").asText() :
                "pm-" + System.currentTimeMillis();

        // Create local payment method record
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setUser(user);
        paymentMethod.setZuoraPaymentMethodId(zuoraPaymentMethodId);
        paymentMethod.setType(PaymentMethod.PaymentType.valueOf(request.getType().toUpperCase()));
        paymentMethod.setIsDefault(request.isSetAsDefault());

        // Set type-specific fields
        if ("CreditCard".equalsIgnoreCase(request.getType())) {
            paymentMethod.setCardLastFour(request.getCardNumber() != null && request.getCardNumber().length() >= 4 ?
                    request.getCardNumber().substring(request.getCardNumber().length() - 4) : null);
            paymentMethod.setCardExpirationMonth(request.getExpiryMonth());
            paymentMethod.setCardExpirationYear(request.getExpiryYear());
            paymentMethod.setCardBrand(detectCardBrand(request.getCardNumber()));
        } else if ("ACH".equalsIgnoreCase(request.getType())) {
            paymentMethod.setAchBankName(request.getBankName());
            paymentMethod.setAchAccountLastFour(request.getAccountNumber() != null && request.getAccountNumber().length() >= 4 ?
                    request.getAccountNumber().substring(request.getAccountNumber().length() - 4) : null);
        }

        paymentMethod = paymentMethodRepository.save(paymentMethod);

        // Set as default if requested
        if (request.isSetAsDefault()) {
            setAsDefaultPaymentMethod(paymentMethod.getId());
        }

        return paymentMethod;
    }

    /**
     * Set a payment method as default
     */
    @Transactional
    public void setAsDefaultPaymentMethod(Long paymentMethodId) throws Exception {
        PaymentMethod paymentMethod = getPaymentMethod(paymentMethodId);
        User user = paymentMethod.getUser();

        // Remove default from all other payment methods
        List<PaymentMethod> allMethods = paymentMethodRepository.findByUserId(user.getId());
        for (PaymentMethod pm : allMethods) {
            if (pm.getIsDefault()) {
                pm.setIsDefault(false);
                paymentMethodRepository.save(pm);
            }
        }

        // Set this one as default
        paymentMethod.setIsDefault(true);

        // Update in Zuora if applicable
        if (paymentMethod.getZuoraPaymentMethodId() != null) {
            try {
                zuoraApiService.setDefaultPaymentMethod(paymentMethod.getZuoraPaymentMethodId());
            } catch (Exception e) {
                System.err.println("Warning: Failed to set default in Zuora: " + e.getMessage());
            }
        }

        paymentMethodRepository.save(paymentMethod);
    }

    /**
     * Delete a payment method
     */
    @Transactional
    public void deletePaymentMethod(Long paymentMethodId) throws Exception {
        PaymentMethod paymentMethod = getPaymentMethod(paymentMethodId);

        // Delete from Zuora
        if (paymentMethod.getZuoraPaymentMethodId() != null) {
            try {
                zuoraApiService.deletePaymentMethod(paymentMethod.getZuoraPaymentMethodId());
            } catch (Exception e) {
                System.err.println("Warning: Failed to delete from Zuora: " + e.getMessage());
            }
        }

        paymentMethodRepository.delete(paymentMethod);
    }

    // ========== Payments ==========

    /**
     * Get all payments for a user
     */
    public List<Payment> getUserPayments(Long userId) {
        return paymentRepository.findByUserId(userId);
    }

    /**
     * Get all payments (admin)
     */
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    /**
     * Get a specific payment
     */
    public Payment getPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));
    }

    /**
     * Create a payment
     */
    @Transactional
    public Payment createPayment(Long userId, CreatePaymentRequest request) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (user.getZuoraAccountId() == null && user.getZuoraAccountNumber() == null) {
            throw new RuntimeException("User does not have a Zuora account");
        }

        String accountKey = user.getZuoraAccountNumber() != null ?
                user.getZuoraAccountNumber() : user.getZuoraAccountId();

        // Prepare invoice applications
        List<Map<String, Object>> invoiceApplications = null;
        if (request.getInvoices() != null && !request.getInvoices().isEmpty()) {
            invoiceApplications = new ArrayList<>();
            for (CreatePaymentRequest.InvoiceApplication app : request.getInvoices()) {
                Map<String, Object> inv = new HashMap<>();
                if (app.getInvoiceId() != null) inv.put("invoiceId", app.getInvoiceId());
                if (app.getInvoiceNumber() != null) inv.put("invoiceNumber", app.getInvoiceNumber());
                if (app.getAmount() != null) inv.put("amount", app.getAmount());
                invoiceApplications.add(inv);
            }
        }

        // Create payment in Zuora
        JsonNode zuoraResponse = zuoraApiService.createPayment(
                accountKey,
                request.getPaymentMethodId(),
                request.getAmount(),
                request.getCurrency(),
                request.getType(),
                request.getEffectiveDate(),
                request.getComment(),
                invoiceApplications
        );

        // Create local payment record
        Payment payment = new Payment();
        payment.setUser(user);
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        payment.setPaymentDate(request.getEffectiveDate() != null ?
                LocalDate.parse(request.getEffectiveDate()) : LocalDate.now());
        payment.setStatus(Payment.PaymentStatus.Processing);

        if (zuoraResponse.has("id")) {
            payment.setZuoraPaymentId(zuoraResponse.get("id").asText());
        }
        if (zuoraResponse.has("paymentNumber")) {
            payment.setZuoraPaymentNumber(zuoraResponse.get("paymentNumber").asText());
        }
        if (request.getPaymentMethodId() != null) {
            paymentMethodRepository.findByZuoraPaymentMethodId(request.getPaymentMethodId())
                    .ifPresent(payment::setPaymentMethod);
        }

        return paymentRepository.save(payment);
    }

    /**
     * Create an electronic payment (processed through gateway)
     */
    @Transactional
    public Payment createElectronicPayment(Long userId, String paymentMethodId, Double amount,
                                            String currency, String comment) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        String accountKey = user.getZuoraAccountNumber() != null ?
                user.getZuoraAccountNumber() : user.getZuoraAccountId();

        JsonNode zuoraResponse = zuoraApiService.createElectronicPayment(
                accountKey, paymentMethodId, amount, currency, null, comment);

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setAmount(amount);
        payment.setCurrency(currency != null ? currency : "USD");
        payment.setPaymentDate(LocalDate.now());
        payment.setStatus(Payment.PaymentStatus.Processing);

        if (zuoraResponse.has("id")) {
            payment.setZuoraPaymentId(zuoraResponse.get("id").asText());
        }
        if (zuoraResponse.has("paymentNumber")) {
            payment.setZuoraPaymentNumber(zuoraResponse.get("paymentNumber").asText());
        }

        paymentMethodRepository.findByZuoraPaymentMethodId(paymentMethodId)
                .ifPresent(payment::setPaymentMethod);

        return paymentRepository.save(payment);
    }

    /**
     * Refund a payment
     */
    @Transactional
    public Payment refundPayment(Long paymentId, RefundRequest request) throws Exception {
        Payment payment = getPayment(paymentId);

        if (payment.getZuoraPaymentId() == null) {
            throw new RuntimeException("Payment does not have a Zuora payment ID");
        }

        // Process refund in Zuora
        JsonNode zuoraResponse = zuoraApiService.refundPayment(
                payment.getZuoraPaymentId(),
                request.getAmount(),
                request.getType(),
                request.getRefundDate(),
                request.getComment(),
                request.getMethodType(),
                request.getReasonCode()
        );

        // Update payment status
        payment.setStatus(Payment.PaymentStatus.Voided);
        paymentRepository.save(payment);

        return payment;
    }

    /**
     * Sync payments from Zuora for a user
     */
    @Transactional
    public void syncPaymentsFromZuora(Long userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        String accountKey = user.getZuoraAccountNumber() != null ?
                user.getZuoraAccountNumber() : user.getZuoraAccountId();

        if (accountKey == null) {
            return;
        }

        JsonNode zuoraPayments = zuoraApiService.getPayments(accountKey);

        if (zuoraPayments.has("payments")) {
            for (JsonNode pay : zuoraPayments.get("payments")) {
                String zuoraPaymentId = pay.get("id").asText();

                // Skip if already exists
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

    private Payment.PaymentStatus parsePaymentStatus(String status) {
        return switch (status.toLowerCase()) {
            case "processed" -> Payment.PaymentStatus.Processed;
            case "error" -> Payment.PaymentStatus.Error;
            case "voided" -> Payment.PaymentStatus.Voided;
            default -> Payment.PaymentStatus.Processing;
        };
    }

    private String detectCardBrand(String cardNumber) {
        if (cardNumber == null) return null;
        cardNumber = cardNumber.replaceAll("\\s", "");

        if (cardNumber.startsWith("4")) return "Visa";
        if (cardNumber.startsWith("5") || cardNumber.startsWith("2")) return "Mastercard";
        if (cardNumber.startsWith("34") || cardNumber.startsWith("37")) return "Amex";
        if (cardNumber.startsWith("6")) return "Discover";

        return "Unknown";
    }
}