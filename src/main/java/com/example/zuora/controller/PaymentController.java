package com.example.zuora.controller;

import com.example.zuora.dto.CreatePaymentMethodRequest;
import com.example.zuora.dto.CreatePaymentRequest;
import com.example.zuora.dto.RefundRequest;
import com.example.zuora.model.Payment;
import com.example.zuora.model.PaymentMethod;
import com.example.zuora.model.User;
import com.example.zuora.service.PaymentService;
import com.example.zuora.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    public PaymentController(PaymentService paymentService, UserService userService) {
        this.paymentService = paymentService;
        this.userService = userService;
    }

    private User getCurrentUser(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName());
    }

    // ========== Payment Methods ==========

    @GetMapping("/methods")
    public ResponseEntity<List<PaymentMethod>> getPaymentMethods(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<PaymentMethod> methods = paymentService.getUserPaymentMethods(user.getId());
        return ResponseEntity.ok(methods);
    }

    @GetMapping("/methods/{id}")
    public ResponseEntity<PaymentMethod> getPaymentMethod(@PathVariable Long id) {
        PaymentMethod method = paymentService.getPaymentMethod(id);
        return ResponseEntity.ok(method);
    }

    @PostMapping("/methods")
    public ResponseEntity<?> createPaymentMethod(Authentication authentication,
                                                  @RequestBody CreatePaymentMethodRequest request) {
        try {
            User user = getCurrentUser(authentication);
            PaymentMethod method = paymentService.createPaymentMethod(user.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(method);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/methods/{id}/default")
    public ResponseEntity<?> setDefaultPaymentMethod(@PathVariable Long id) {
        try {
            paymentService.setAsDefaultPaymentMethod(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Payment method set as default");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/methods/{id}")
    public ResponseEntity<?> deletePaymentMethod(@PathVariable Long id) {
        try {
            paymentService.deletePaymentMethod(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Payment method deleted");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ========== Payments ==========

    @GetMapping
    public ResponseEntity<List<Payment>> getPayments(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<Payment> payments = paymentService.getUserPayments(user.getId());
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(@PathVariable Long id) {
        Payment payment = paymentService.getPayment(id);
        return ResponseEntity.ok(payment);
    }

    @PostMapping
    public ResponseEntity<?> createPayment(Authentication authentication,
                                            @RequestBody CreatePaymentRequest request) {
        try {
            User user = getCurrentUser(authentication);
            Payment payment = paymentService.createPayment(user.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(payment);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/electronic")
    public ResponseEntity<?> createElectronicPayment(Authentication authentication,
                                                      @RequestParam String paymentMethodId,
                                                      @RequestParam Double amount,
                                                      @RequestParam(required = false) String currency,
                                                      @RequestParam(required = false) String comment) {
        try {
            User user = getCurrentUser(authentication);
            Payment payment = paymentService.createElectronicPayment(
                    user.getId(),
                    paymentMethodId,
                    amount,
                    currency,
                    comment
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(payment);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<?> refundPayment(@PathVariable Long id,
                                            @RequestBody RefundRequest request) {
        try {
            Payment payment = paymentService.refundPayment(id, request);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncPayments(Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            paymentService.syncPaymentsFromZuora(user.getId());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Payments synced successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}