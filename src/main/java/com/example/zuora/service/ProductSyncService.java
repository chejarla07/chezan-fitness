package com.example.zuora.service;

import com.example.zuora.model.*;
import com.example.zuora.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service to sync local products/rate plans with Zuora
 * Creates products in Zuora and stores the returned IDs locally
 */
@Service
public class ProductSyncService {

    private final ProductRepository productRepository;
    private final RatePlanRepository ratePlanRepository;
    private final RatePlanChargeRepository ratePlanChargeRepository;
    private final ZuoraApiService zuoraApiService;

    public ProductSyncService(ProductRepository productRepository,
                              RatePlanRepository ratePlanRepository,
                              RatePlanChargeRepository ratePlanChargeRepository,
                              ZuoraApiService zuoraApiService) {
        this.productRepository = productRepository;
        this.ratePlanRepository = ratePlanRepository;
        this.ratePlanChargeRepository = ratePlanChargeRepository;
        this.zuoraApiService = zuoraApiService;
    }

    /**
     * Sync all local products to Zuora
     * Creates products, rate plans, and charges in Zuora and updates local IDs
     */
    @Transactional
    public void syncAllProductsToZuora() throws Exception {
        System.out.println("Starting product sync to Zuora...");

        List<Product> products = productRepository.findAll();

        for (Product product : products) {
            syncProductToZuora(product);
        }

        System.out.println("Product sync completed. Synced " + products.size() + " products.");
    }

    /**
     * Sync a single product to Zuora
     */
    @Transactional
    public void syncProductToZuora(Product product) throws Exception {
        // Check if product already has Zuora ID
        if (product.getZuoraProductId() != null && !product.getZuoraProductId().isEmpty()) {
            System.out.println("Product '" + product.getName() + "' already synced to Zuora (ID: " + product.getZuoraProductId() + ")");
            return;
        }

        System.out.println("Creating product in Zuora: " + product.getName());

        // Create product in Zuora
        JsonNode productResponse = zuoraApiService.createProduct(product);

        // Zuora returns "Id" (uppercase) in some responses
        String zuoraProductId = null;
        if (productResponse.has("Id")) {
            zuoraProductId = productResponse.get("Id").asText();
        } else if (productResponse.has("id")) {
            zuoraProductId = productResponse.get("id").asText();
        } else if (productResponse.has("productId")) {
            zuoraProductId = productResponse.get("productId").asText();
        }

        if (zuoraProductId == null || zuoraProductId.isEmpty()) {
            throw new RuntimeException("Failed to create product in Zuora: " + productResponse.toString());
        }

        product.setZuoraProductId(zuoraProductId);
        productRepository.save(product);
        System.out.println("Product created in Zuora. ID: " + zuoraProductId);

        // Sync all rate plans for this product
        List<RatePlan> ratePlans = ratePlanRepository.findByProductId(product.getId());
        for (RatePlan ratePlan : ratePlans) {
            syncRatePlanToZuora(ratePlan, zuoraProductId);
        }
    }

    /**
     * Sync a rate plan to Zuora
     */
    @Transactional
    public void syncRatePlanToZuora(RatePlan ratePlan, String zuoraProductId) throws Exception {
        // Check if rate plan already has Zuora ID
        if (ratePlan.getZuoraRatePlanId() != null && !ratePlan.getZuoraRatePlanId().isEmpty()) {
            // Check if it's a test/mock ID that needs to be replaced
            if (ratePlan.getZuoraRatePlanId().startsWith("test-")) {
                // This is a mock ID, proceed to create real one
            } else {
                System.out.println("Rate plan '" + ratePlan.getName() + "' already synced to Zuora (ID: " + ratePlan.getZuoraRatePlanId() + ")");
                return;
            }
        }

        System.out.println("Creating rate plan in Zuora: " + ratePlan.getName() + " for product: " + zuoraProductId);

        // Create rate plan in Zuora
        JsonNode ratePlanResponse = zuoraApiService.createRatePlan(ratePlan, zuoraProductId);

        // Zuora returns "Id" (uppercase) in some responses
        String zuoraRatePlanId = null;
        if (ratePlanResponse.has("Id")) {
            zuoraRatePlanId = ratePlanResponse.get("Id").asText();
        } else if (ratePlanResponse.has("id")) {
            zuoraRatePlanId = ratePlanResponse.get("id").asText();
        } else if (ratePlanResponse.has("ratePlanId")) {
            zuoraRatePlanId = ratePlanResponse.get("ratePlanId").asText();
        }

        if (zuoraRatePlanId == null || zuoraRatePlanId.isEmpty()) {
            throw new RuntimeException("Failed to create rate plan in Zuora: " + ratePlanResponse.toString());
        }

        ratePlan.setZuoraRatePlanId(zuoraRatePlanId);
        ratePlanRepository.save(ratePlan);
        System.out.println("Rate plan created in Zuora. ID: " + zuoraRatePlanId);

        // Sync all charges for this rate plan
        List<RatePlanCharge> charges = ratePlanChargeRepository.findByRatePlanId(ratePlan.getId());
        for (RatePlanCharge charge : charges) {
            syncChargeToZuora(charge, zuoraRatePlanId);
        }
    }

    /**
     * Sync a rate plan charge to Zuora
     */
    @Transactional
    public void syncChargeToZuora(RatePlanCharge charge, String zuoraRatePlanId) throws Exception {
        // Check if charge already has Zuora ID
        if (charge.getZuoraChargeId() != null && !charge.getZuoraChargeId().isEmpty()) {
            if (charge.getZuoraChargeId().startsWith("test-")) {
                // This is a mock ID, proceed to create real one
            } else {
                System.out.println("Charge '" + charge.getName() + "' already synced to Zuora (ID: " + charge.getZuoraChargeId() + ")");
                return;
            }
        }

        System.out.println("Creating charge in Zuora: " + charge.getName() + " for rate plan: " + zuoraRatePlanId);

        // Create charge in Zuora
        JsonNode chargeResponse = zuoraApiService.createRatePlanCharge(charge, zuoraRatePlanId);

        // Zuora returns "Id" (uppercase) in some responses
        String zuoraChargeId = null;
        if (chargeResponse.has("Id")) {
            zuoraChargeId = chargeResponse.get("Id").asText();
        } else if (chargeResponse.has("id")) {
            zuoraChargeId = chargeResponse.get("id").asText();
        } else if (chargeResponse.has("chargeId")) {
            zuoraChargeId = chargeResponse.get("chargeId").asText();
        }

        if (zuoraChargeId == null || zuoraChargeId.isEmpty()) {
            throw new RuntimeException("Failed to create charge in Zuora: " + chargeResponse.toString());
        }

        charge.setZuoraChargeId(zuoraChargeId);
        ratePlanChargeRepository.save(charge);
        System.out.println("Charge created in Zuora. ID: " + zuoraChargeId);
    }

    /**
     * Check if products are synced to Zuora
     */
    public boolean areProductsSynced() {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            return false;
        }

        for (Product product : products) {
            if (product.getZuoraProductId() == null || product.getZuoraProductId().isEmpty()) {
                return false;
            }

            List<RatePlan> ratePlans = ratePlanRepository.findByProductId(product.getId());
            for (RatePlan ratePlan : ratePlans) {
                if (ratePlan.getZuoraRatePlanId() == null || ratePlan.getZuoraRatePlanId().isEmpty() ||
                    ratePlan.getZuoraRatePlanId().startsWith("test-")) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Get sync status for all products
     */
    public String getSyncStatus() {
        StringBuilder status = new StringBuilder();
        List<Product> products = productRepository.findAll();

        status.append("Product Sync Status:\n");
        status.append("==================\n\n");

        for (Product product : products) {
            status.append("Product: ").append(product.getName())
                  .append(" (ID: ").append(product.getId()).append(")\n");
            status.append("  Zuora Product ID: ");

            if (product.getZuoraProductId() == null || product.getZuoraProductId().isEmpty()) {
                status.append("NOT SYNCED\n");
            } else {
                status.append(product.getZuoraProductId()).append("\n");
            }

            List<RatePlan> ratePlans = ratePlanRepository.findByProductId(product.getId());
            for (RatePlan ratePlan : ratePlans) {
                status.append("  └─ Rate Plan: ").append(ratePlan.getName()).append("\n");
                status.append("     Zuora Rate Plan ID: ");

                if (ratePlan.getZuoraRatePlanId() == null || ratePlan.getZuoraRatePlanId().isEmpty()) {
                    status.append("NOT SYNCED\n");
                } else if (ratePlan.getZuoraRatePlanId().startsWith("test-")) {
                    status.append(ratePlan.getZuoraRatePlanId()).append(" (MOCK - needs sync)\n");
                } else {
                    status.append(ratePlan.getZuoraRatePlanId()).append("\n");
                }
            }
            status.append("\n");
        }

        return status.toString();
    }
}