package com.example.zuora.service;

import com.example.zuora.dto.CreateProductRequest;
import com.example.zuora.dto.CreateRatePlanRequest;
import com.example.zuora.dto.UpdatePriceRequest;
import com.example.zuora.dto.UpdateRatePlanRequest;
import com.example.zuora.model.*;
import com.example.zuora.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final RatePlanRepository ratePlanRepository;
    private final RatePlanChargeRepository ratePlanChargeRepository;
    private final ZuoraApiService zuoraApiService;

    public ProductService(ProductRepository productRepository,
                         RatePlanRepository ratePlanRepository,
                         RatePlanChargeRepository ratePlanChargeRepository,
                         ZuoraApiService zuoraApiService) {
        this.productRepository = productRepository;
        this.ratePlanRepository = ratePlanRepository;
        this.ratePlanChargeRepository = ratePlanChargeRepository;
        this.zuoraApiService = zuoraApiService;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> getActiveProducts() {
        return productRepository.findByStatus(Product.Status.Active);
    }

    public List<Product> getActiveProductsByCategory(Product.Category category) {
        return productRepository.findByCategoryAndStatus(category, Product.Status.Active);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
    }

    @Transactional
    public Product createProduct(CreateProductRequest request) throws Exception {
        // Create product locally
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(Product.Category.valueOf(request.getCategory()));
        product.setEffectiveStartDate(LocalDate.now());
        product = productRepository.save(product);

        // Create in Zuora
        JsonNode zuoraResponse = zuoraApiService.createProduct(product);
        String zuoraProductId = zuoraResponse.get("Id").asText();
        product.setZuoraProductId(zuoraProductId);
        product = productRepository.save(product);

        // Create rate plan
        RatePlan ratePlan = new RatePlan();
        ratePlan.setProduct(product);
        ratePlan.setName(request.getRatePlanName());
        ratePlan.setDescription(request.getRatePlanDescription());
        ratePlan.setBillingPeriod(RatePlan.BillingPeriod.valueOf(request.getBillingPeriod()));
        ratePlan.setEffectiveStartDate(LocalDate.now());
        ratePlan = ratePlanRepository.save(ratePlan);

        // Create rate plan in Zuora
        JsonNode zuoraRatePlanResponse = zuoraApiService.createRatePlan(ratePlan, zuoraProductId);
        String zuoraRatePlanId = zuoraRatePlanResponse.get("Id").asText();
        ratePlan.setZuoraRatePlanId(zuoraRatePlanId);
        ratePlan = ratePlanRepository.save(ratePlan);

        // Create recurring charge
        RatePlanCharge charge = new RatePlanCharge();
        charge.setRatePlan(ratePlan);
        charge.setName(request.getName() + " - " + request.getRatePlanName());
        charge.setChargeType(RatePlanCharge.ChargeType.Recurring);
        charge.setChargeModel(RatePlanCharge.ChargeModel.FlatFee);
        charge.setAmount(request.getPrice().doubleValue());
        charge.setCurrency(request.getCurrency());
        charge.setBillingTiming(RatePlanCharge.BillingTiming.InAdvance);
        charge = ratePlanChargeRepository.save(charge);

        // Create charge in Zuora
        JsonNode zuoraChargeResponse = zuoraApiService.createRatePlanCharge(charge, zuoraRatePlanId);
        charge.setZuoraChargeId(zuoraChargeResponse.get("Id").asText());
        ratePlanChargeRepository.save(charge);

        return product;
    }

    @Transactional
    public Product updateProduct(Long id, String name, String description) throws Exception {
        Product product = getProductById(id);

        // Update in Zuora
        if (product.getZuoraProductId() != null) {
            zuoraApiService.updateProduct(product.getZuoraProductId(), name, description);
        }

        // Update locally
        if (name != null) product.setName(name);
        if (description != null) product.setDescription(description);

        return productRepository.save(product);
    }

    @Transactional
    public void updatePrice(UpdatePriceRequest request) throws Exception {
        RatePlan ratePlan = ratePlanRepository.findById(request.getRatePlanId())
                .orElseThrow(() -> new RuntimeException("Rate plan not found"));

        RatePlanCharge charge = ratePlan.getRecurringCharge();
        if (charge == null) {
            throw new RuntimeException("No recurring charge found for this rate plan");
        }

        // Update in Zuora
        if (charge.getZuoraChargeId() != null) {
            zuoraApiService.updateRatePlanChargePrice(charge.getZuoraChargeId(), request.getNewPrice().doubleValue());
        }

        // Update locally
        charge.setAmount(request.getNewPrice().doubleValue());
        ratePlanChargeRepository.save(charge);
    }

    public List<RatePlan> getRatePlansByProduct(Long productId) {
        return ratePlanRepository.findByProductIdAndStatus(productId, RatePlan.Status.Active);
    }

    public RatePlan getRatePlanById(Long id) {
        return ratePlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rate plan not found: " + id));
    }

    @Transactional
    public void deactivateProduct(Long id) {
        Product product = getProductById(id);
        product.setStatus(Product.Status.Inactive);
        product.setEffectiveEndDate(LocalDate.now());
        productRepository.save(product);
    }

    // ==================== RATE PLAN MANAGEMENT ====================

    /**
     * Create a new rate plan for a product
     */
    @Transactional
    public RatePlan createRatePlan(Long productId, CreateRatePlanRequest request) throws Exception {
        Product product = getProductById(productId);

        // Create rate plan locally
        RatePlan ratePlan = new RatePlan();
        ratePlan.setProduct(product);
        ratePlan.setName(request.getName());
        ratePlan.setDescription(request.getDescription());
        ratePlan.setBillingPeriod(request.getBillingPeriod());
        ratePlan.setEffectiveStartDate(LocalDate.now());
        if (!request.isActive()) {
            ratePlan.setStatus(RatePlan.Status.Inactive);
        }
        ratePlan = ratePlanRepository.save(ratePlan);

        // Create in Zuora if product is synced
        if (product.getZuoraProductId() != null) {
            try {
                JsonNode zuoraRatePlanResponse = zuoraApiService.createRatePlan(ratePlan, product.getZuoraProductId());
                String zuoraRatePlanId = zuoraRatePlanResponse.get("Id").asText();
                ratePlan.setZuoraRatePlanId(zuoraRatePlanId);
                ratePlan = ratePlanRepository.save(ratePlan);

                // Create recurring charge
                RatePlanCharge charge = new RatePlanCharge();
                charge.setRatePlan(ratePlan);
                charge.setName(product.getName() + " - " + request.getName());
                charge.setChargeType(RatePlanCharge.ChargeType.Recurring);
                charge.setChargeModel(RatePlanCharge.ChargeModel.FlatFee);
                charge.setAmount(request.getPrice().doubleValue());
                charge.setCurrency("USD");
                charge.setBillingTiming(RatePlanCharge.BillingTiming.InAdvance);
                charge = ratePlanChargeRepository.save(charge);

                // Create charge in Zuora
                JsonNode zuoraChargeResponse = zuoraApiService.createRatePlanCharge(charge, zuoraRatePlanId);
                charge.setZuoraChargeId(zuoraChargeResponse.get("Id").asText());
                ratePlanChargeRepository.save(charge);
            } catch (Exception e) {
                // Log error but continue - rate plan can still be used locally
                System.err.println("Failed to create rate plan in Zuora: " + e.getMessage());
            }
        }

        return ratePlan;
    }

    /**
     * Update an existing rate plan
     */
    @Transactional
    public RatePlan updateRatePlan(Long ratePlanId, UpdateRatePlanRequest request) throws Exception {
        RatePlan ratePlan = getRatePlanById(ratePlanId);

        // Update locally
        if (request.getName() != null) {
            ratePlan.setName(request.getName());
        }
        if (request.getDescription() != null) {
            ratePlan.setDescription(request.getDescription());
        }
        if (request.getBillingPeriod() != null) {
            ratePlan.setBillingPeriod(request.getBillingPeriod());
        }

        ratePlan = ratePlanRepository.save(ratePlan);

        // Update price if provided
        if (request.getPrice() != null) {
            RatePlanCharge charge = ratePlan.getRecurringCharge();
            if (charge != null) {
                // Update in Zuora
                if (charge.getZuoraChargeId() != null) {
                    try {
                        zuoraApiService.updateRatePlanChargePrice(charge.getZuoraChargeId(), request.getPrice().doubleValue());
                    } catch (Exception e) {
                        System.err.println("Failed to update charge price in Zuora: " + e.getMessage());
                    }
                }
                // Update locally
                charge.setAmount(request.getPrice().doubleValue());
                ratePlanChargeRepository.save(charge);
            }
        }

        return ratePlan;
    }

    /**
     * Activate a rate plan
     */
    @Transactional
    public void activateRatePlan(Long ratePlanId) {
        RatePlan ratePlan = getRatePlanById(ratePlanId);
        ratePlan.setStatus(RatePlan.Status.Active);
        ratePlan.setEffectiveEndDate(null);
        ratePlanRepository.save(ratePlan);
    }

    /**
     * Deactivate a rate plan
     */
    @Transactional
    public void deactivateRatePlan(Long ratePlanId) {
        RatePlan ratePlan = getRatePlanById(ratePlanId);
        ratePlan.setStatus(RatePlan.Status.Inactive);
        ratePlan.setEffectiveEndDate(LocalDate.now());
        ratePlanRepository.save(ratePlan);
    }

    /**
     * Get all rate plans for a product (including inactive)
     */
    public List<RatePlan> getAllRatePlansByProduct(Long productId) {
        return ratePlanRepository.findByProductId(productId);
    }

    /**
     * Get all rate plans (all products)
     */
    public List<RatePlan> getAllRatePlans() {
        return ratePlanRepository.findAll();
    }
}
