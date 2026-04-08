package com.example.zuora.controller;

import com.example.zuora.dto.ApplyDiscountRequest;
import com.example.zuora.dto.CreateDiscountRequest;
import com.example.zuora.model.Discount;
import com.example.zuora.model.User;
import com.example.zuora.service.DiscountService;
import com.example.zuora.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/discounts")
public class DiscountController {

    private final DiscountService discountService;
    private final UserService userService;

    public DiscountController(DiscountService discountService, UserService userService) {
        this.discountService = discountService;
        this.userService = userService;
    }

    private User getCurrentUser(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName());
    }

    // ========== Discount CRUD ==========

    @GetMapping
    public ResponseEntity<List<Discount>> getAllDiscounts() {
        List<Discount> discounts = discountService.getAllDiscounts();
        return ResponseEntity.ok(discounts);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Discount>> getActiveDiscounts() {
        List<Discount> discounts = discountService.getActiveDiscounts();
        return ResponseEntity.ok(discounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Discount> getDiscount(@PathVariable Long id) {
        Discount discount = discountService.getDiscountById(id);
        return ResponseEntity.ok(discount);
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<?> getDiscountByCode(@PathVariable String code) {
        try {
            Discount discount = discountService.validateDiscountCode(code);
            return ResponseEntity.ok(discount);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Discount>> getDiscountsForProduct(@PathVariable Long productId) {
        List<Discount> discounts = discountService.getDiscountsForProduct(productId);
        return ResponseEntity.ok(discounts);
    }

    @GetMapping("/rateplan/{ratePlanId}")
    public ResponseEntity<List<Discount>> getDiscountsForRatePlan(@PathVariable Long ratePlanId) {
        List<Discount> discounts = discountService.getDiscountsForRatePlan(ratePlanId);
        return ResponseEntity.ok(discounts);
    }

    @PostMapping
    public ResponseEntity<?> createDiscount(@RequestBody CreateDiscountRequest request) {
        try {
            Discount discount = discountService.createDiscount(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(discount);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDiscount(@PathVariable Long id,
                                             @RequestBody CreateDiscountRequest request) {
        try {
            Discount discount = discountService.updateDiscount(id, request);
            return ResponseEntity.ok(discount);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<?> activateDiscount(@PathVariable Long id) {
        try {
            Discount discount = discountService.activateDiscount(id);
            return ResponseEntity.ok(discount);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateDiscount(@PathVariable Long id) {
        try {
            Discount discount = discountService.deactivateDiscount(id);
            return ResponseEntity.ok(discount);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDiscount(@PathVariable Long id) {
        try {
            discountService.deleteDiscount(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Discount deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ========== Discount Application ==========

    @PostMapping("/apply")
    public ResponseEntity<?> applyDiscountToSubscription(Authentication authentication,
                                                         @RequestBody ApplyDiscountRequest request) {
        try {
            User user = getCurrentUser(authentication);
            var subscription = discountService.applyDiscountToSubscription(user.getId(), request);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Discount applied successfully");
            response.put("subscriptionId", subscription.getZuoraSubscriptionId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateDiscountCode(@RequestParam String code) {
        try {
            Discount discount = discountService.validateDiscountCode(code);
            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("discount", discount);
            response.put("discountType", discount.getDiscountType());
            response.put("discountValue", discount.getDiscountValue());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/calculate")
    public ResponseEntity<?> calculateDiscount(@RequestParam Double originalPrice,
                                                 @RequestParam Long discountId) {
        try {
            Discount discount = discountService.getDiscountById(discountId);
            Double discountedPrice = discountService.calculateDiscountedPrice(originalPrice, discount);
            Double discountAmount = discountService.calculateDiscountAmount(originalPrice, discount);

            Map<String, Object> response = new HashMap<>();
            response.put("originalPrice", originalPrice);
            response.put("discountedPrice", discountedPrice);
            response.put("discountAmount", discountAmount);
            response.put("discountType", discount.getDiscountType());
            response.put("discountValue", discount.getDiscountValue());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/applicable/{ratePlanId}")
    public ResponseEntity<List<Discount>> getApplicableDiscounts(@PathVariable Long ratePlanId) {
        List<Discount> discounts = discountService.getApplicableDiscounts(ratePlanId);
        return ResponseEntity.ok(discounts);
    }
}