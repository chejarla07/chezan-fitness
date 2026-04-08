package com.example.zuora.service;

import com.example.zuora.dto.ApplyDiscountRequest;
import com.example.zuora.dto.CreateDiscountRequest;
import com.example.zuora.model.*;
import com.example.zuora.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class DiscountService {

    private final DiscountRepository discountRepository;
    private final ProductRepository productRepository;
    private final RatePlanRepository ratePlanRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final ZuoraApiService zuoraApiService;
    private final ObjectMapper objectMapper;

    public DiscountService(DiscountRepository discountRepository,
                           ProductRepository productRepository,
                           RatePlanRepository ratePlanRepository,
                           SubscriptionRepository subscriptionRepository,
                           UserRepository userRepository,
                           ZuoraApiService zuoraApiService) {
        this.discountRepository = discountRepository;
        this.productRepository = productRepository;
        this.ratePlanRepository = ratePlanRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.zuoraApiService = zuoraApiService;
        this.objectMapper = new ObjectMapper();
    }

    // ========== Discount Management ==========

    /**
     * Get all discounts
     */
    public List<Discount> getAllDiscounts() {
        return discountRepository.findAll();
    }

    /**
     * Get all active discounts
     */
    public List<Discount> getActiveDiscounts() {
        return discountRepository.findActiveDiscounts(Discount.Status.ACTIVE, LocalDate.now());
    }

    /**
     * Get discount by ID
     */
    public Discount getDiscountById(Long id) {
        return discountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Discount not found: " + id));
    }

    /**
     * Get discount by code
     */
    public Optional<Discount> getDiscountByCode(String code) {
        return discountRepository.findByDiscountCode(code);
    }

    /**
     * Get discounts for a product
     */
    public List<Discount> getDiscountsForProduct(Long productId) {
        return discountRepository.findByProductIdAndStatus(productId, Discount.Status.ACTIVE);
    }

    /**
     * Get discounts for a rate plan
     */
    public List<Discount> getDiscountsForRatePlan(Long ratePlanId) {
        return discountRepository.findByRatePlanIdAndStatus(ratePlanId, Discount.Status.ACTIVE);
    }

    /**
     * Create a new discount
     */
    @Transactional
    public Discount createDiscount(CreateDiscountRequest request) throws Exception {
        // Check if code already exists
        if (request.getDiscountCode() != null && discountRepository.existsByDiscountCode(request.getDiscountCode())) {
            throw new RuntimeException("Discount code already exists: " + request.getDiscountCode());
        }

        Discount discount = new Discount();
        discount.setName(request.getName());
        discount.setDescription(request.getDescription());
        discount.setDiscountCode(request.getDiscountCode());
        discount.setDiscountType(Discount.DiscountType.valueOf(request.getDiscountType().toUpperCase()));
        discount.setDiscountLevel(Discount.DiscountLevel.valueOf(request.getDiscountLevel().toUpperCase()));
        discount.setApplyTo(Discount.ApplyTo.valueOf(request.getApplyTo().toUpperCase()));

        // Set discount value based on type
        if ("PERCENTAGE".equalsIgnoreCase(request.getDiscountType())) {
            discount.setDiscountPercentage(request.getDiscountPercentage());
        } else {
            discount.setDiscountAmount(request.getDiscountAmount());
        }

        // Link to product/rate plan if specified
        if (request.getProductId() != null) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + request.getProductId()));
            discount.setProduct(product);
        }

        if (request.getRatePlanId() != null) {
            RatePlan ratePlan = ratePlanRepository.findById(request.getRatePlanId())
                    .orElseThrow(() -> new RuntimeException("Rate plan not found: " + request.getRatePlanId()));
            discount.setRatePlan(ratePlan);
        }

        // Set dates
        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
            discount.setStartDate(LocalDate.parse(request.getStartDate()));
        }
        if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
            discount.setEndDate(LocalDate.parse(request.getEndDate()));
        }

        // Set redemption limits
        discount.setMaxRedemptions(request.getMaxRedemptions());
        discount.setCurrentRedemptions(0);
        discount.setStatus(Discount.Status.ACTIVE);

        // Create discount charge in Zuora if linked to a rate plan
        if (discount.getRatePlan() != null && discount.getRatePlan().getZuoraRatePlanId() != null) {
            try {
                JsonNode zuoraResponse = zuoraApiService.createDiscountCharge(
                        discount.getName(),
                        discount.getRatePlan().getZuoraRatePlanId(),
                        discount.getDiscountPercentage(),
                        discount.getDiscountLevel().name(),
                        discount.getApplyTo().name()
                );

                if (zuoraResponse.has("id")) {
                    discount.setZuoraDiscountId(zuoraResponse.get("id").asText());
                }
            } catch (Exception e) {
                System.err.println("Warning: Failed to create discount in Zuora: " + e.getMessage());
                // Continue without Zuora integration
            }
        }

        return discountRepository.save(discount);
    }

    /**
     * Update a discount
     */
    @Transactional
    public Discount updateDiscount(Long id, CreateDiscountRequest request) throws Exception {
        Discount discount = getDiscountById(id);

        discount.setName(request.getName());
        discount.setDescription(request.getDescription());
        discount.setDiscountType(Discount.DiscountType.valueOf(request.getDiscountType().toUpperCase()));
        discount.setDiscountLevel(Discount.DiscountLevel.valueOf(request.getDiscountLevel().toUpperCase()));
        discount.setApplyTo(Discount.ApplyTo.valueOf(request.getApplyTo().toUpperCase()));

        if ("PERCENTAGE".equalsIgnoreCase(request.getDiscountType())) {
            discount.setDiscountPercentage(request.getDiscountPercentage());
            discount.setDiscountAmount(null);
        } else {
            discount.setDiscountAmount(request.getDiscountAmount());
            discount.setDiscountPercentage(null);
        }

        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
            discount.setStartDate(LocalDate.parse(request.getStartDate()));
        }
        if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
            discount.setEndDate(LocalDate.parse(request.getEndDate()));
        }

        discount.setMaxRedemptions(request.getMaxRedemptions());

        return discountRepository.save(discount);
    }

    /**
     * Activate a discount
     */
    @Transactional
    public Discount activateDiscount(Long id) {
        Discount discount = getDiscountById(id);
        discount.setStatus(Discount.Status.ACTIVE);
        return discountRepository.save(discount);
    }

    /**
     * Deactivate a discount
     */
    @Transactional
    public Discount deactivateDiscount(Long id) {
        Discount discount = getDiscountById(id);
        discount.setStatus(Discount.Status.INACTIVE);
        return discountRepository.save(discount);
    }

    /**
     * Delete a discount
     */
    @Transactional
    public void deleteDiscount(Long id) {
        Discount discount = getDiscountById(id);
        discountRepository.delete(discount);
    }

    // ========== Discount Application ==========

    /**
     * Apply discount to subscription
     * This applies a discount charge override to an existing subscription via Zuora Orders API
     */
    @Transactional
    public Subscription applyDiscountToSubscription(Long userId, ApplyDiscountRequest request) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Discount discount = getDiscountById(request.getDiscountId());

        // Check if discount is valid
        if (!discount.isActive()) {
            throw new RuntimeException("Discount is not active or has expired");
        }

        // Check redemption limits
        if (discount.getMaxRedemptions() != null &&
            discount.getCurrentRedemptions() >= discount.getMaxRedemptions()) {
            throw new RuntimeException("Discount has reached maximum redemptions");
        }

        // Get subscription
        Subscription subscription = subscriptionRepository.findByZuoraSubscriptionId(request.getSubscriptionId())
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + request.getSubscriptionId()));

        if (subscription.getUser().getId() != userId) {
            throw new RuntimeException("Subscription does not belong to user");
        }

        String accountNumber = user.getZuoraAccountNumber();
        if (accountNumber == null) {
            throw new RuntimeException("User does not have a Zuora account");
        }

        // Apply discount via Zuora
        try {
            JsonNode zuoraResponse = zuoraApiService.applyDiscountToSubscription(
                    accountNumber,
                    request.getSubscriptionId(),
                    discount.getZuoraDiscountId(),
                    discount.getDiscountPercentage(),
                    discount.getDiscountLevel().name(),
                    discount.getApplyTo().name(),
                    request.getDurationPeriods(),
                    request.getDurationPeriodType()
            );

            // Increment redemption count
            discount.setCurrentRedemptions(discount.getCurrentRedemptions() + 1);
            discountRepository.save(discount);

            return subscription;
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply discount to subscription: " + e.getMessage(), e);
        }
    }

    /**
     * Validate a discount code
     */
    public Discount validateDiscountCode(String code) {
        Optional<Discount> discountOpt = discountRepository.findByDiscountCode(code);

        if (discountOpt.isEmpty()) {
            throw new RuntimeException("Invalid discount code: " + code);
        }

        Discount discount = discountOpt.get();

        if (!discount.isActive()) {
            throw new RuntimeException("Discount code is not active or has expired");
        }

        if (discount.getMaxRedemptions() != null &&
            discount.getCurrentRedemptions() >= discount.getMaxRedemptions()) {
            throw new RuntimeException("Discount has reached maximum redemptions");
        }

        return discount;
    }

    /**
     * Calculate discounted price
     */
    public Double calculateDiscountedPrice(Double originalPrice, Discount discount) {
        if (!discount.isActive()) {
            return originalPrice;
        }

        if (discount.getDiscountType() == Discount.DiscountType.PERCENTAGE) {
            Double percentage = discount.getDiscountPercentage() != null ? discount.getDiscountPercentage() : 0.0;
            return originalPrice * (1 - percentage / 100);
        } else {
            Double amount = discount.getDiscountAmount() != null ? discount.getDiscountAmount() : 0.0;
            return Math.max(0, originalPrice - amount);
        }
    }

    /**
     * Calculate discount amount
     */
    public Double calculateDiscountAmount(Double originalPrice, Discount discount) {
        if (!discount.isActive()) {
            return 0.0;
        }

        if (discount.getDiscountType() == Discount.DiscountType.PERCENTAGE) {
            Double percentage = discount.getDiscountPercentage() != null ? discount.getDiscountPercentage() : 0.0;
            return originalPrice * (percentage / 100);
        } else {
            return discount.getDiscountAmount() != null ? discount.getDiscountAmount() : 0.0;
        }
    }

    /**
     * Get applicable discounts for a price
     */
    public List<Discount> getApplicableDiscounts(Long ratePlanId) {
        List<Discount> discounts = discountRepository.findByRatePlanIdAndStatus(ratePlanId, Discount.Status.ACTIVE);

        // Filter by date validity
        LocalDate now = LocalDate.now();
        discounts.removeIf(d -> !d.isActive());

        return discounts;
    }
}