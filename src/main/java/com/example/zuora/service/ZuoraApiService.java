package com.example.zuora.service;

import com.example.zuora.dto.SignupRequest;
import com.example.zuora.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class ZuoraApiService {

    @Value("${zuora.base-url:https://rest.apisandbox.zuora.com}")
    private String zuoraBaseUrl;

    @Value("${zuora.client-id}")
    private String clientId;

    @Value("${zuora.client-secret}")
    private String clientSecret;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String accessToken;
    private long tokenExpiry;

    public ZuoraApiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    /**
     * Authenticate with Zuora OAuth
     */
    private synchronized void authenticate() throws Exception {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiry) {
            return;
        }

        String authUrl = zuoraBaseUrl + "/oauth/token";
        String formData = "grant_type=client_credentials&client_id=" + clientId
                + "&client_secret=" + clientSecret;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(authUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Zuora authentication failed: " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        accessToken = json.get("access_token").asText();
        long expiresIn = json.get("expires_in").asLong();
        tokenExpiry = System.currentTimeMillis() + (expiresIn * 1000) - 60000; // Buffer 1 minute
    }

    /**
     * Create a product in Zuora
     */
    public JsonNode createProduct(Product product) throws Exception {
        // Test mode: return mock success response
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock product creation");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("success", true);
            mockResponse.put("id", "test-product-" + System.currentTimeMillis());
            return mockResponse;
        }

        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/object/product";

        ObjectNode body = objectMapper.createObjectNode();
        body.put("Name", product.getName());
        body.put("Description", product.getDescription());
        body.put("EffectiveStartDate", product.getEffectiveStartDate().format(DateTimeFormatter.ISO_DATE));
        body.put("EffectiveEndDate", product.getEffectiveEndDate() != null
                ? product.getEffectiveEndDate().format(DateTimeFormatter.ISO_DATE)
                : "2099-12-31");

        return executePost(apiUrl, body.toString());
    }

    /**
     * Update product in Zuora
     */
    public JsonNode updateProduct(String zuoraProductId, String name, String description) throws Exception {
        // Test mode: return mock success response
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock product update");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("success", true);
            mockResponse.put("id", zuoraProductId);
            return mockResponse;
        }

        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/object/product/" + zuoraProductId;

        ObjectNode body = objectMapper.createObjectNode();
        if (name != null) body.put("Name", name);
        if (description != null) body.put("Description", description);

        return executePut(apiUrl, body.toString());
    }

    /**
     * Create a rate plan in Zuora
     */
    public JsonNode createRatePlan(RatePlan ratePlan, String zuoraProductId) throws Exception {
        // Test mode: return mock success response
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock rate plan creation");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("success", true);
            mockResponse.put("id", "test-rateplan-" + System.currentTimeMillis());
            return mockResponse;
        }

        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/object/product-rate-plan";

        ObjectNode body = objectMapper.createObjectNode();
        body.put("Name", ratePlan.getName());
        body.put("Description", ratePlan.getDescription());
        body.put("ProductId", zuoraProductId);
        body.put("EffectiveStartDate", ratePlan.getEffectiveStartDate() != null
                ? ratePlan.getEffectiveStartDate().format(DateTimeFormatter.ISO_DATE)
                : LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        body.put("EffectiveEndDate", ratePlan.getEffectiveEndDate() != null
                ? ratePlan.getEffectiveEndDate().format(DateTimeFormatter.ISO_DATE)
                : "2099-12-31");

        return executePost(apiUrl, body.toString());
    }

    /**
     * Create a rate plan charge in Zuora
     * Required fields per Zuora API: Name, ProductRatePlanId, ChargeType, ChargeModel,
     * BillCycleType, BillingPeriod (except OneTime), TriggerEvent, UseDiscountSpecificAccountingCode,
     * ProductRatePlanChargeTierData (for pricing)
     */
    public JsonNode createRatePlanCharge(RatePlanCharge charge, String zuoraRatePlanId) throws Exception {
        // Test mode: return mock success response
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock rate plan charge creation");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("success", true);
            mockResponse.put("id", "test-charge-" + System.currentTimeMillis());
            return mockResponse;
        }

        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/object/product-rate-plan-charge";

        ObjectNode body = objectMapper.createObjectNode();
        body.put("Name", charge.getName());
        body.put("ChargeType", charge.getChargeType().toString());
        body.put("ChargeModel", charge.getChargeModel() != null ? charge.getChargeModel().toString() : "Flat Fee Pricing");
        body.put("ProductRatePlanId", zuoraRatePlanId);

        // Required: BillCycleType
        body.put("BillCycleType", "DefaultFromCustomer");

        // Required: BillingPeriod (except for OneTime charges)
        if (charge.getChargeType() == null || !"OneTime".equals(charge.getChargeType().toString())) {
            body.put("BillingPeriod", "Month");
        }

        // Required: TriggerEvent
        body.put("TriggerEvent", "ContractEffective");

        // Required: UseDiscountSpecificAccountingCode
        body.put("UseDiscountSpecificAccountingCode", false);

        // Pricing via ProductRatePlanChargeTierData (required container)
        ObjectNode tierData = objectMapper.createObjectNode();
        ObjectNode tier = objectMapper.createObjectNode();

        // Tier pricing fields
        if (charge.getAmount() != null) {
            tier.put("Price", charge.getAmount());
        }
        tier.put("Currency", "USD");

        // Add tier to tier data
        tierData.putArray("ProductRatePlanChargeTier").add(tier);
        body.set("ProductRatePlanChargeTierData", tierData);

        // Optional: BillingTiming (for recurring charges)
        if (charge.getBillingTiming() != null) {
            // Convert enum to Zuora format (e.g., InAdvance -> In Advance)
            String billingTiming = charge.getBillingTiming().toString().replaceAll("([A-Z])", " $1").trim();
            body.put("BillingTiming", billingTiming);
        }

        return executePost(apiUrl, body.toString());
    }

    /**
     * Update rate plan charge pricing
     */
    public JsonNode updateRatePlanChargePrice(String zuoraChargeId, Double newPrice) throws Exception {
        // Test mode: return mock success response
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock price update");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("success", true);
            mockResponse.put("id", zuoraChargeId);
            return mockResponse;
        }

        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/object/product-rate-plan-charge/" + zuoraChargeId;

        ObjectNode body = objectMapper.createObjectNode();
        body.put("ListPrice", newPrice);

        return executePut(apiUrl, body.toString());
    }

    /**
     * Create a customer account in Zuora
     */
    public JsonNode createAccount(User user, SignupRequest signupRequest) throws Exception {
        // Test mode: return mock success response when using test credentials
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock successful account creation");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("success", true);
            mockResponse.put("accountId", "test-acc-" + System.currentTimeMillis());
            mockResponse.put("accountNumber", "T" + System.currentTimeMillis());
            mockResponse.put("billToContactId", "test-bill-contact-" + System.currentTimeMillis());
            mockResponse.put("soldToContactId", "test-sold-contact-" + System.currentTimeMillis());
            return mockResponse;
        }

        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/accounts";

        // Determine Bill To name (use provided or default to user's name)
        String billToFirstName = user.getFirstName();
        String billToLastName = user.getLastName();
        if (signupRequest.getBillToName() != null && !signupRequest.getBillToName().isEmpty()) {
            String[] nameParts = signupRequest.getBillToName().trim().split("\\s+");
            billToFirstName = nameParts[0];
            billToLastName = nameParts.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(nameParts, 1, nameParts.length)) : "";
        }

        // Build Bill To Contact
        ObjectNode billToContact = objectMapper.createObjectNode();
        billToContact.put("firstName", billToFirstName);
        billToContact.put("lastName", billToLastName);
        billToContact.put("workEmail", user.getEmail());
        billToContact.put("workPhone", user.getPhone() != null ? user.getPhone() : "");
        billToContact.put("address1", signupRequest.getAddress1());
        if (signupRequest.getAddress2() != null && !signupRequest.getAddress2().isEmpty()) {
            billToContact.put("address2", signupRequest.getAddress2());
        }
        billToContact.put("city", signupRequest.getCity());
        billToContact.put("state", signupRequest.getState());
        billToContact.put("country", signupRequest.getCountry());
        billToContact.put("zipCode", signupRequest.getZipCode());

        // Build Sold To Contact (use Bill To if sameAsBillTo is true, otherwise use separate fields)
        ObjectNode soldToContact = objectMapper.createObjectNode();
        if (signupRequest.isSameAsBillTo()) {
            soldToContact = billToContact;
        } else {
            soldToContact.put("firstName", user.getFirstName());
            soldToContact.put("lastName", user.getLastName());
            soldToContact.put("workEmail", user.getEmail());
            soldToContact.put("workPhone", user.getPhone() != null ? user.getPhone() : "");
            soldToContact.put("address1", signupRequest.getSoldToAddress1() != null ? signupRequest.getSoldToAddress1() : signupRequest.getAddress1());
            if (signupRequest.getSoldToAddress2() != null && !signupRequest.getSoldToAddress2().isEmpty()) {
                soldToContact.put("address2", signupRequest.getSoldToAddress2());
            }
            soldToContact.put("city", signupRequest.getSoldToCity() != null ? signupRequest.getSoldToCity() : signupRequest.getCity());
            soldToContact.put("state", signupRequest.getSoldToState() != null ? signupRequest.getSoldToState() : signupRequest.getState());
            soldToContact.put("country", signupRequest.getSoldToCountry() != null ? signupRequest.getSoldToCountry() : signupRequest.getCountry());
            soldToContact.put("zipCode", signupRequest.getSoldToZipCode() != null ? signupRequest.getSoldToZipCode() : signupRequest.getZipCode());
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("name", user.getFullName());
        body.put("currency", "USD");
        body.put("billCycleDay", 1);
        body.put("autoPay", false);
        body.put("paymentTerm", "Due Upon Receipt");
        body.set("billToContact", billToContact);
        body.set("soldToContact", soldToContact);
        body.put("ConsumerType__c", "B2C");

        return executePost(apiUrl, body.toString());
    }

    /**
     * Update customer account in Zuora
     */
    public JsonNode updateAccount(String zuoraAccountId, Map<String, Object> updates) throws Exception {
        // Test mode: return mock success response
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock account update");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("id", zuoraAccountId);
            mockResponse.put("success", true);
            return mockResponse;
        }

        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/accounts/" + zuoraAccountId;

        return executePut(apiUrl, objectMapper.writeValueAsString(updates));
    }

    /**
     * Get contact details from Zuora by contact ID
     */
    public JsonNode getContact(String contactId) throws Exception {
        // Test mode: return mock contact data
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock contact data");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("id", contactId);
            mockResponse.put("firstName", "Test");
            mockResponse.put("lastName", "User");
            mockResponse.put("workEmail", "test@example.com");
            mockResponse.put("workPhone", "(555) 123-4567");
            mockResponse.put("address1", "123 Test St");
            mockResponse.put("city", "Test City");
            mockResponse.put("state", "CA");
            mockResponse.put("country", "United States");
            mockResponse.put("zipCode", "12345");
            return mockResponse;
        }

        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/contacts/" + contactId;
        return executeGet(apiUrl);
    }

    /**
     * Get full account details including contacts from Zuora
     */
    public JsonNode getAccountDetails(String zuoraAccountId) throws Exception {
        // Test mode: return mock account data
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock account details");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("id", zuoraAccountId);
            mockResponse.put("accountNumber", "A00000000");
            mockResponse.put("name", "Test User");
            mockResponse.put("currency", "USD");
            mockResponse.put("status", "Active");
            return mockResponse;
        }

        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/accounts/" + zuoraAccountId;
        return executeGet(apiUrl);
    }

    /**
     * Get account by ID
     */
    public JsonNode getAccount(String zuoraAccountId) throws Exception {
        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/accounts/" + zuoraAccountId;
        return executeGet(apiUrl);
    }

    /**
     * Create a subscription in Zuora
     * Note: When Orders is enabled, this method returns a mock response
     * to allow local subscription creation. Full Orders API integration
     * requires additional implementation.
     */
    public JsonNode createSubscription(User user, RatePlan ratePlan, String zuoraAccountId,
                                        String paymentMethodId) throws Exception {
        return createSubscription(user, ratePlan, zuoraAccountId, paymentMethodId, null);
    }

    /**
     * Create a subscription in Zuora using the Orders API with custom start date
     */
    public JsonNode createSubscriptionWithStartDate(User user, RatePlan ratePlan, String zuoraAccountNumber,
                                                   String paymentMethodId, String zuoraChargeId,
                                                   String contractEffectiveDate, String termStartDate) throws Exception {
        // Test mode: return mock success response
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock subscription creation with start date: " + termStartDate);
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("subscriptionId", "test-sub-" + System.currentTimeMillis());
            mockResponse.put("subscriptionNumber", "S-TEST-" + System.currentTimeMillis());
            mockResponse.put("success", true);
            return mockResponse;
        }

        authenticate();

        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String contractDate = contractEffectiveDate != null ? contractEffectiveDate : today;
        String startDate = termStartDate != null ? termStartDate : today;

        String apiUrl = zuoraBaseUrl + "/v1/orders";

        System.out.println("Creating subscription in Zuora for account: " + zuoraAccountNumber);
        System.out.println("  Rate Plan ID: " + ratePlan.getZuoraRatePlanId());
        System.out.println("  Contract Effective Date: " + contractDate);
        System.out.println("  Term Start Date: " + startDate);

        // Build rate plan subscription
        ObjectNode subscribeToRatePlan = objectMapper.createObjectNode();
        subscribeToRatePlan.put("productRatePlanId", ratePlan.getZuoraRatePlanId());

        // Build terms - TERMED subscription with auto-renewal
        ObjectNode initialTerm = objectMapper.createObjectNode();
        initialTerm.put("startDate", startDate);
        initialTerm.put("period", 12);
        initialTerm.put("periodType", "Month");
        initialTerm.put("termType", "TERMED");

        ObjectNode renewalTerm = objectMapper.createObjectNode();
        renewalTerm.put("period", 12);
        renewalTerm.put("periodType", "Month");

        ObjectNode terms = objectMapper.createObjectNode();
        terms.set("initialTerm", initialTerm);
        terms.put("renewalSetting", "RENEW_WITH_SPECIFIC_TERM");
        terms.putArray("renewalTerms").add(renewalTerm);

        // Build createSubscription action
        ObjectNode createSubscription = objectMapper.createObjectNode();
        createSubscription.set("terms", terms);
        createSubscription.putArray("subscribeToRatePlans").add(subscribeToRatePlan);

        // Build order action with custom dates
        ObjectNode orderAction = objectMapper.createObjectNode();
        orderAction.put("type", "CreateSubscription");
        orderAction.putArray("triggerDates")
            .add(objectMapper.createObjectNode().put("name", "ContractEffective").put("triggerDate", contractDate))
            .add(objectMapper.createObjectNode().put("name", "ServiceActivation").put("triggerDate", startDate))
            .add(objectMapper.createObjectNode().put("name", "CustomerAcceptance").put("triggerDate", contractDate));
        orderAction.set("createSubscription", createSubscription);

        // Build subscription
        ObjectNode subscriptionData = objectMapper.createObjectNode();
        subscriptionData.putArray("orderActions").add(orderAction);

        // Build order request
        ObjectNode body = objectMapper.createObjectNode();
        body.put("existingAccountNumber", zuoraAccountNumber);
        body.put("orderDate", today);
        // Note: Zuora Orders API auto-generates order number, no "name" field accepted
        body.put("description", "Subscription created via Chezan Fitness");
        body.putArray("subscriptions").add(subscriptionData);

        System.out.println("Zuora Orders API Request: " + body.toString());

        JsonNode response = executePost(apiUrl, body.toString());
        System.out.println("Zuora Orders API Response: " + response.toString());

        // Check for success
        if (response.has("success") && !response.get("success").asBoolean()) {
            String errorMsg = response.has("reasons") ? response.get("reasons").toString() : "Unknown error";
            throw new RuntimeException("Zuora subscription creation failed: " + errorMsg);
        }

        // Extract subscription number from response
        String subscriptionNumber = null;

        if (response.has("subscriptionNumbers") && response.get("subscriptionNumbers").isArray()
                && response.get("subscriptionNumbers").size() > 0) {
            subscriptionNumber = response.get("subscriptionNumbers").get(0).asText();
        }

        if (subscriptionNumber == null) {
            throw new RuntimeException("Zuora response missing subscription number");
        }

        System.out.println("Zuora subscription created successfully: " + subscriptionNumber);

        // Return response in expected format
        ObjectNode result = objectMapper.createObjectNode();
        result.put("subscriptionId", subscriptionNumber);
        result.put("subscriptionNumber", subscriptionNumber);
        result.put("success", true);
        return result;
    }

    /**
     * Create a subscription in Zuora using the Orders API
     * Creates an actual subscription in Zuora and returns the subscription number
     */
    public JsonNode createSubscription(User user, RatePlan ratePlan, String zuoraAccountNumber,
                                        String paymentMethodId, String zuoraChargeId) throws Exception {
        // Delegate to the new method with today's date
        return createSubscriptionWithStartDate(user, ratePlan, zuoraAccountNumber, paymentMethodId, zuoraChargeId, null, null);
    }

    /**
     * Amend/Update subscription using Orders API
     * Changes plan by removing old rate plan and adding new one (upgrade/downgrade)
     * Per Zuora API: Use changePlan order action to swap rate plans
     */
    public JsonNode amendSubscription(String zuoraSubscriptionNumber, String currentRatePlanId, String newRatePlanId,
                                       String zuoraAccountNumber, String effectiveDate) throws Exception {
        // Test mode: return mock success response
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock amendment");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("success", true);
            mockResponse.put("subscriptionNumber", zuoraSubscriptionNumber);
            mockResponse.put("orderNumber", "O-TEST-" + System.currentTimeMillis());
            return mockResponse;
        }

        authenticate();
        String today = effectiveDate != null ? effectiveDate : LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String apiUrl = zuoraBaseUrl + "/v1/orders";

        System.out.println("Amending subscription in Zuora: " + zuoraSubscriptionNumber);
        System.out.println("  Current Rate Plan (Product): " + currentRatePlanId);
        System.out.println("  New Rate Plan (Product): " + newRatePlanId);
        System.out.println("  Account Number: " + zuoraAccountNumber);
        System.out.println("  Effective Date: " + today);

        // First get the subscription to find the subscription-specific rate plan ID
        // This is different from the product rate plan ID - it's the instance ID on this subscription
        JsonNode subDetails = getSubscription(zuoraSubscriptionNumber);
        String subscriptionRatePlanId = null;

        if (subDetails != null && subDetails.has("ratePlans")) {
            for (JsonNode rp : subDetails.get("ratePlans")) {
                // Find the rate plan that matches our current product rate plan ID
                if (rp.has("productRatePlanId") && currentRatePlanId.equals(rp.get("productRatePlanId").asText())) {
                    subscriptionRatePlanId = rp.get("id").asText(); // This is the subscription-specific ID
                    System.out.println("  Found subscription rate plan ID: " + subscriptionRatePlanId);
                    break;
                }
            }
        }

        if (subscriptionRatePlanId == null) {
            throw new RuntimeException("Could not find current rate plan on subscription. " +
                "Product rate plan ID may not match the subscription's rate plan.");
        }

        // Build changePlan order action
        // Per API: changePlan requires ratePlanId (subscription-specific instance ID) and newProductRatePlan
        ObjectNode changePlan = objectMapper.createObjectNode();
        changePlan.put("ratePlanId", subscriptionRatePlanId); // Subscription-specific rate plan ID to remove

        ObjectNode newProductRatePlan = objectMapper.createObjectNode();
        newProductRatePlan.put("productRatePlanId", newRatePlanId); // New product rate plan to add
        changePlan.set("newProductRatePlan", newProductRatePlan);

        ObjectNode changePlanAction = objectMapper.createObjectNode();
        changePlanAction.put("type", "ChangePlan");
        changePlanAction.putArray("triggerDates").add(
            objectMapper.createObjectNode().put("name", "ContractEffective").put("triggerDate", today)
        );
        changePlanAction.set("changePlan", changePlan);

        // Build order request with subscriptions containing orderActions
        ObjectNode subscriptionData = objectMapper.createObjectNode();
        subscriptionData.put("subscriptionNumber", zuoraSubscriptionNumber);
        subscriptionData.putArray("orderActions").add(changePlanAction);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("existingAccountNumber", zuoraAccountNumber);
        body.put("orderDate", today);
        body.put("description", "Subscription plan change via Chezan Fitness");
        body.putArray("subscriptions").add(subscriptionData);

        System.out.println("Zuora Orders API Amend Request: " + body.toString());

        JsonNode response = executePost(apiUrl, body.toString());
        System.out.println("Zuora Orders API Amend Response: " + response.toString());

        // Check for success
        if (response.has("success") && !response.get("success").asBoolean()) {
            String errorMsg = response.has("reasons") ? response.get("reasons").toString() : "Unknown error";
            throw new RuntimeException("Zuora subscription amendment failed: " + errorMsg);
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("subscriptionNumber", zuoraSubscriptionNumber);
        return result;
    }

    /**
     * Cancel subscription using Orders API
     * Per Zuora API: cancellationPolicy is required. Options: EndOfCurrentTerm, EndOfLastInvoicePeriod, SpecificDate
     */
    public JsonNode cancelSubscription(String zuoraSubscriptionNumber,
                                        String cancellationPolicy,
                                        String zuoraAccountNumber,
                                        String cancellationDate) throws Exception {
        // Test mode: return mock success response
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock cancellation");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("success", true);
            mockResponse.put("subscriptionNumber", zuoraSubscriptionNumber);
            mockResponse.put("orderNumber", "O-TEST-" + System.currentTimeMillis());
            return mockResponse;
        }

        authenticate();
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String apiUrl = zuoraBaseUrl + "/v1/orders";

        System.out.println("Cancelling subscription in Zuora: " + zuoraSubscriptionNumber);
        System.out.println("  Cancellation Policy: " + cancellationPolicy);
        System.out.println("  Cancellation Date: " + cancellationDate);

        // Map cancellation policy to Zuora format
        // Valid values: EndOfCurrentTerm, EndOfLastInvoicePeriod, SpecificDate
        String zuoraCancelPolicy;
        String effectiveDate;

        // Check if the provided policy is valid
        if ("EndOfLastInvoicePeriod".equalsIgnoreCase(cancellationPolicy)) {
            zuoraCancelPolicy = "EndOfLastInvoicePeriod";
            effectiveDate = today;
        } else if ("EndOfCurrentTerm".equalsIgnoreCase(cancellationPolicy)) {
            zuoraCancelPolicy = "EndOfCurrentTerm";
            effectiveDate = today;
        } else if ("SpecificDate".equalsIgnoreCase(cancellationPolicy) || "immediate".equalsIgnoreCase(cancellationPolicy)) {
            // SpecificDate policy requires a cancellation date
            zuoraCancelPolicy = "SpecificDate";
            effectiveDate = (cancellationDate != null && !cancellationDate.isEmpty()) ? cancellationDate : today;
        } else {
            // Default: EndOfCurrentTerm
            zuoraCancelPolicy = "EndOfCurrentTerm";
            effectiveDate = today;
        }

        // Build cancel subscription action
        ObjectNode cancelSubscription = objectMapper.createObjectNode();
        cancelSubscription.put("cancellationPolicy", zuoraCancelPolicy);

        // Add cancellationEffectiveDate for SpecificDate policy
        if ("SpecificDate".equals(zuoraCancelPolicy)) {
            cancelSubscription.put("cancellationEffectiveDate", effectiveDate);
        }

        // Build order action
        ObjectNode orderAction = objectMapper.createObjectNode();
        orderAction.put("type", "CancelSubscription");
        orderAction.putArray("triggerDates").add(
            objectMapper.createObjectNode().put("name", "ContractEffective").put("triggerDate", effectiveDate)
        );
        orderAction.set("cancelSubscription", cancelSubscription);

        // Build subscription
        ObjectNode subscriptionData = objectMapper.createObjectNode();
        subscriptionData.put("subscriptionNumber", zuoraSubscriptionNumber);
        subscriptionData.putArray("orderActions").add(orderAction);

        // Build order request
        ObjectNode body = objectMapper.createObjectNode();
        body.put("existingAccountNumber", zuoraAccountNumber);
        body.put("orderDate", today);
        body.put("description", "Subscription cancellation via Chezan Fitness");
        body.putArray("subscriptions").add(subscriptionData);

        System.out.println("Zuora Orders API Cancel Request: " + body.toString());

        JsonNode response = executePost(apiUrl, body.toString());
        System.out.println("Zuora Orders API Cancel Response: " + response.toString());

        // Check for success
        if (response.has("success") && !response.get("success").asBoolean()) {
            String errorMsg = response.has("reasons") ? response.get("reasons").toString() : "Unknown error";
            throw new RuntimeException("Zuora subscription cancellation failed: " + errorMsg);
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("subscriptionNumber", zuoraSubscriptionNumber);
        return result;
    }

    /**
     * Pause/Suspend subscription using Orders API
     * Per Zuora API: SuspendSubscription requires suspendPolicy and optionally suspendPeriods
     */
    public JsonNode pauseSubscription(String zuoraSubscriptionNumber,
                                       String zuoraAccountNumber,
                                       int pausePeriods) throws Exception {
        // Test mode: return mock success response
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock pause");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("success", true);
            mockResponse.put("subscriptionNumber", zuoraSubscriptionNumber);
            mockResponse.put("orderNumber", "O-TEST-" + System.currentTimeMillis());
            return mockResponse;
        }

        authenticate();
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String apiUrl = zuoraBaseUrl + "/v1/orders";

        System.out.println("Pausing subscription in Zuora: " + zuoraSubscriptionNumber);
        System.out.println("  Pause Periods: " + pausePeriods);

        // Build suspend subscription action per Zuora API
        // Required: suspendPolicy (FixedPeriodsFromToday, Today, EndOfLastInvoicePeriod, or SpecificDate)
        // Optional: suspendPeriods (number of periods)
        ObjectNode suspend = objectMapper.createObjectNode();
        suspend.put("suspendPolicy", "FixedPeriodsFromToday");
        suspend.put("suspendPeriods", pausePeriods);
        suspend.put("suspendPeriodsType", "Month");

        // Build order action
        // Per Zuora API: type is "Suspend" (not "SuspendSubscription")
        ObjectNode orderAction = objectMapper.createObjectNode();
        orderAction.put("type", "Suspend");
        orderAction.putArray("triggerDates").add(
            objectMapper.createObjectNode().put("name", "ContractEffective").put("triggerDate", today)
        );
        orderAction.set("suspend", suspend);

        // Build subscription
        ObjectNode subscriptionData = objectMapper.createObjectNode();
        subscriptionData.put("subscriptionNumber", zuoraSubscriptionNumber);
        subscriptionData.putArray("orderActions").add(orderAction);

        // Build order request
        ObjectNode body = objectMapper.createObjectNode();
        body.put("existingAccountNumber", zuoraAccountNumber);
        body.put("orderDate", today);
        body.put("description", "Subscription pause via Chezan Fitness");
        body.putArray("subscriptions").add(subscriptionData);

        System.out.println("Zuora Orders API Pause Request: " + body.toString());

        JsonNode response = executePost(apiUrl, body.toString());
        System.out.println("Zuora Orders API Pause Response: " + response.toString());

        // Check for success
        if (response.has("success") && !response.get("success").asBoolean()) {
            String errorMsg = response.has("reasons") ? response.get("reasons").toString() : "Unknown error";
            throw new RuntimeException("Zuora subscription pause failed: " + errorMsg);
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("subscriptionNumber", zuoraSubscriptionNumber);
        return result;
    }

    /**
     * Resume subscription using Orders API
     * Per Zuora API: resumePolicy options are "Today" or "SpecificDate"
     */
    public JsonNode resumeSubscription(String zuoraSubscriptionNumber,
                                        String zuoraAccountNumber,
                                        String resumeDate) throws Exception {
        // Test mode: return mock success response
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock resume");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("success", true);
            mockResponse.put("subscriptionNumber", zuoraSubscriptionNumber);
            mockResponse.put("orderNumber", "O-TEST-" + System.currentTimeMillis());
            return mockResponse;
        }

        authenticate();
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String apiUrl = zuoraBaseUrl + "/v1/orders";

        System.out.println("Resuming subscription in Zuora: " + zuoraSubscriptionNumber);
        System.out.println("  Resume Date: " + (resumeDate != null ? resumeDate : today));

        // Build resume action per Zuora API
        ObjectNode resume = objectMapper.createObjectNode();

        // Use SpecificDate policy if resumeDate is provided and different from today
        if (resumeDate != null && !resumeDate.equals(today)) {
            resume.put("resumePolicy", "SpecificDate");
            resume.put("resumeSpecificDate", resumeDate);
        } else {
            resume.put("resumePolicy", "Today");
        }

        // Build order action
        // Per Zuora API: type is "Resume" (not "ResumeSubscription")
        ObjectNode orderAction = objectMapper.createObjectNode();
        orderAction.put("type", "Resume");
        orderAction.putArray("triggerDates").add(
            objectMapper.createObjectNode().put("name", "ContractEffective").put("triggerDate", resumeDate != null ? resumeDate : today)
        );
        orderAction.set("resume", resume);

        // Build subscription
        ObjectNode subscriptionData = objectMapper.createObjectNode();
        subscriptionData.put("subscriptionNumber", zuoraSubscriptionNumber);
        subscriptionData.putArray("orderActions").add(orderAction);

        // Build order request
        ObjectNode body = objectMapper.createObjectNode();
        body.put("existingAccountNumber", zuoraAccountNumber);
        body.put("orderDate", today);
        body.put("description", "Subscription resume via Chezan Fitness");
        body.putArray("subscriptions").add(subscriptionData);

        System.out.println("Zuora Orders API Resume Request: " + body.toString());

        JsonNode response = executePost(apiUrl, body.toString());
        System.out.println("Zuora Orders API Resume Response: " + response.toString());

        // Check for success
        if (response.has("success") && !response.get("success").asBoolean()) {
            String errorMsg = response.has("reasons") ? response.get("reasons").toString() : "Unknown error";
            throw new RuntimeException("Zuora subscription resume failed: " + errorMsg);
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("subscriptionNumber", zuoraSubscriptionNumber);
        return result;
    }

    /**
     * Get subscription details
     */
    public JsonNode getSubscription(String zuoraSubscriptionId) throws Exception {
        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/subscriptions/" + zuoraSubscriptionId;
        return executeGet(apiUrl);
    }

    /**
     * Create a payment method (using token from Hosted Payment Page)
     */
    public JsonNode createPaymentMethod(String zuoraAccountId, String token, PaymentMethod.PaymentType type) throws Exception {
        // Test mode: return mock success response
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock payment method");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("id", "test-pm-" + System.currentTimeMillis());
            mockResponse.put("success", true);
            return mockResponse;
        }

        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/payment-methods";

        ObjectNode body = objectMapper.createObjectNode();
        body.put("accountKey", zuoraAccountId);
        body.put("token", token);
        body.put("type", type.toString());
        body.put("defaultPaymentMethod", true);

        return executePost(apiUrl, body.toString());
    }

    /**
     * Get payment methods for account
     */
    public JsonNode getPaymentMethods(String zuoraAccountId) throws Exception {
        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/payment-methods/accounts/" + zuoraAccountId;
        return executeGet(apiUrl);
    }

    /**
     * Update payment method
     */
    public JsonNode updatePaymentMethod(String paymentMethodId, Map<String, Object> updates) throws Exception {
        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/payment-methods/" + paymentMethodId;
        return executePut(apiUrl, objectMapper.writeValueAsString(updates));
    }

    /**
     * Get invoices for account using accountKey (can be account ID or account number)
     */
    public JsonNode getInvoices(String accountKey) throws Exception {
        // Test mode: return mock invoices
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock invoices");
            return objectMapper.readTree("{\"invoices\":[{\"id\":\"inv-test-1\",\"accountId\":\"" + accountKey + "\",\"status\":\"Posted\"}],\"success\":true}");
        }

        authenticate();
        // Zuora Billing Documents API - returns invoices, credit memos, and debit memos
        // Use accountNumber parameter for account number (e.g. A00000101)
        String apiUrl = zuoraBaseUrl + "/v1/billing-documents?accountNumber=" + java.net.URLEncoder.encode(accountKey, "UTF-8");
        System.out.println("Fetching billing documents from Zuora for account: " + accountKey);
        System.out.println("API URL: " + apiUrl);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Zuora invoices response status: " + response.statusCode());
        System.out.println("Zuora invoices response body: " + response.body());

        return objectMapper.readTree(response.body());
    }

    /**
     * Get specific invoice
     */
    public JsonNode getInvoice(String zuoraInvoiceId) throws Exception {
        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/invoices/" + zuoraInvoiceId;
        return executeGet(apiUrl);
    }

    /**
     * Get invoice PDF from Zuora
     * Uses GET /v1/invoices/{invoiceNumber}/files to get file metadata,
     * then downloads from the file URL
     */
    public byte[] getInvoicePdf(String invoiceNumber) throws Exception {
        // Test mode: return mock PDF bytes
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock PDF data");
            return ("Mock PDF content for invoice " + invoiceNumber).getBytes();
        }

        authenticate();

        // Step 1: Get file list from /v1/invoices/{invoiceNumber}/files
        String filesUrl = zuoraBaseUrl + "/v1/invoices/" + java.net.URLEncoder.encode(invoiceNumber, "UTF-8") + "/files";
        System.out.println("Fetching invoice files from: " + filesUrl);

        HttpRequest filesRequest = HttpRequest.newBuilder()
                .uri(URI.create(filesUrl))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> filesResponse = httpClient.send(filesRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("Files API response status: " + filesResponse.statusCode());

        if (filesResponse.statusCode() != 200) {
            throw new RuntimeException("Failed to get invoice files: HTTP " + filesResponse.statusCode());
        }

        JsonNode filesJson = objectMapper.readTree(filesResponse.body());
        System.out.println("Files API response: " + filesResponse.body());

        if (!filesJson.has("invoiceFiles") || !filesJson.get("invoiceFiles").isArray()
                || filesJson.get("invoiceFiles").size() == 0) {
            throw new RuntimeException("No PDF files found for invoice: " + invoiceNumber);
        }

        // Get the first (most recent) file
        JsonNode fileInfo = filesJson.get("invoiceFiles").get(0);
        String pdfFileUrl = fileInfo.has("pdfFileUrl") ? fileInfo.get("pdfFileUrl").asText() : null;
        String fileId = fileInfo.has("id") ? fileInfo.get("id").asText() : null;

        if (pdfFileUrl == null || pdfFileUrl.isEmpty()) {
            throw new RuntimeException("PDF file URL not found in response");
        }

        System.out.println("Found PDF file URL: " + pdfFileUrl);

        // Step 2: Download the PDF file
        // The pdfFileUrl is relative like "/v1/files/xxxxx", need to construct full URL
        // For file downloads, use the base URL without "rest." prefix
        String downloadUrl = zuoraBaseUrl.replace("rest.", "") + "/apps" + pdfFileUrl;
        System.out.println("Downloading PDF from: " + downloadUrl);

        HttpRequest downloadRequest = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<byte[]> downloadResponse = httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofByteArray());

        if (downloadResponse.statusCode() >= 200 && downloadResponse.statusCode() < 300) {
            byte[] pdfBytes = downloadResponse.body();
            System.out.println("Successfully downloaded PDF, size: " + pdfBytes.length + " bytes");
            if (pdfBytes.length == 0) {
                throw new RuntimeException("Downloaded PDF is empty (0 bytes)");
            }
            return pdfBytes;
        } else {
            throw new RuntimeException("Failed to download PDF: HTTP " + downloadResponse.statusCode());
        }
    }

    /**
     * Get payments for account
     */
    public JsonNode getPayments(String zuoraAccountId) throws Exception {
        // Test mode: return mock payments
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock payments");
            return objectMapper.readTree("{\"payments\":[{\"id\":\"pay-test-1\",\"accountId\":\"" + zuoraAccountId + "\",\"status\":\"Processed\"}],\"success\":true}");
        }

        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/payments/?accountKey=" + zuoraAccountId;
        return executeGet(apiUrl);
    }

    /**
     * Get specific payment
     */
    public JsonNode getPayment(String zuoraPaymentId) throws Exception {
        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/payments/" + zuoraPaymentId;
        return executeGet(apiUrl);
    }

    /**
     * Create a payment in Zuora
     * API: POST /v1/payments
     */
    public JsonNode createPayment(String zuoraAccountKey, String zuoraPaymentMethodId,
                                   Double amount, String currency, String type,
                                   String effectiveDate, String comment,
                                   java.util.List<java.util.Map<String, Object>> invoiceApplications) throws Exception {
        // Test mode: return mock success response
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock payment creation");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("success", true);
            mockResponse.put("id", "pay-test-" + System.currentTimeMillis());
            mockResponse.put("paymentNumber", "P-" + System.currentTimeMillis());
            return mockResponse;
        }

        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/payments";

        ObjectNode body = objectMapper.createObjectNode();
        body.put("accountKey", zuoraAccountKey);
        body.put("amount", amount);
        body.put("currency", currency != null ? currency : "USD");
        body.put("type", type != null ? type : "External");

        if (zuoraPaymentMethodId != null && !zuoraPaymentMethodId.isEmpty()) {
            body.put("paymentMethodId", zuoraPaymentMethodId);
        }

        if (effectiveDate != null && !effectiveDate.isEmpty()) {
            body.put("effectiveDate", effectiveDate);
        }

        if (comment != null && !comment.isEmpty()) {
            body.put("comment", comment);
        }

        // Add invoice applications if provided
        if (invoiceApplications != null && !invoiceApplications.isEmpty()) {
            var invoicesArray = body.putArray("invoices");
            for (java.util.Map<String, Object> invoice : invoiceApplications) {
                ObjectNode invoiceNode = objectMapper.createObjectNode();
                if (invoice.get("invoiceId") != null) {
                    invoiceNode.put("invoiceId", invoice.get("invoiceId").toString());
                }
                if (invoice.get("invoiceNumber") != null) {
                    invoiceNode.put("invoiceNumber", invoice.get("invoiceNumber").toString());
                }
                if (invoice.get("amount") != null) {
                    invoiceNode.put("amount", Double.parseDouble(invoice.get("amount").toString()));
                }
                invoicesArray.add(invoiceNode);
            }
        }

        System.out.println("Creating payment in Zuora for account: " + zuoraAccountKey);
        System.out.println("Payment API Request: " + body.toString());

        return executePost(apiUrl, body.toString());
    }

    /**
     * Create an electronic payment (processed through payment gateway)
     */
    public JsonNode createElectronicPayment(String zuoraAccountKey, String zuoraPaymentMethodId,
                                             Double amount, String currency,
                                             String gatewayId, String comment) throws Exception {
        // Test mode: return mock success response
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock electronic payment");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("success", true);
            mockResponse.put("id", "pay-electronic-" + System.currentTimeMillis());
            mockResponse.put("paymentNumber", "P-" + System.currentTimeMillis());
            mockResponse.put("status", "Processed");
            return mockResponse;
        }

        return createPayment(zuoraAccountKey, zuoraPaymentMethodId, amount, currency,
                "Electronic", null, comment, null);
    }

    /**
     * Refund a payment
     * API: POST /v1/payments/{paymentKey}/refunds
     */
    public JsonNode refundPayment(String paymentKey, Double amount, String type,
                                    String refundDate, String comment, String methodType,
                                    String reasonCode) throws Exception {
        // Test mode: return mock success response
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock refund");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("success", true);
            mockResponse.put("id", "refund-test-" + System.currentTimeMillis());
            mockResponse.put("number", "R-" + System.currentTimeMillis());
            mockResponse.put("status", "Processed");
            return mockResponse;
        }

        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/payments/" + paymentKey + "/refunds";

        ObjectNode body = objectMapper.createObjectNode();
        body.put("totalAmount", amount);
        body.put("type", type != null ? type : "External");

        if (refundDate != null && !refundDate.isEmpty()) {
            body.put("refundDate", refundDate);
        }

        if (comment != null && !comment.isEmpty()) {
            body.put("comment", comment);
        }

        if (methodType != null && !methodType.isEmpty()) {
            body.put("methodType", methodType);
        }

        if (reasonCode != null && !reasonCode.isEmpty()) {
            body.put("reasonCode", reasonCode);
        }

        System.out.println("Creating refund in Zuora for payment: " + paymentKey);
        System.out.println("Refund API Request: " + body.toString());

        return executePost(apiUrl, body.toString());
    }

    /**
     * Apply a discount charge to a subscription using Orders API
     * Uses chargeOverrides to apply percentage or fixed amount discount
     */
    public JsonNode applyDiscountToSubscription(String zuoraAccountNumber, String zuoraSubscriptionNumber,
                                                  String discountChargeId, Double discountPercentage,
                                                  String discountLevel, String applyTo,
                                                  Integer durationPeriods, String durationPeriodType) throws Exception {
        // Test mode: return mock success response
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock discount application");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("success", true);
            mockResponse.put("subscriptionNumber", zuoraSubscriptionNumber);
            mockResponse.put("orderNumber", "O-DISCOUNT-" + System.currentTimeMillis());
            return mockResponse;
        }

        authenticate();
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String apiUrl = zuoraBaseUrl + "/v1/orders";

        System.out.println("Applying discount to subscription: " + zuoraSubscriptionNumber);
        System.out.println("  Discount Charge ID: " + discountChargeId);
        System.out.println("  Discount Percentage: " + discountPercentage);
        System.out.println("  Discount Level: " + discountLevel);

        // Build charge override with discount pricing
        ObjectNode chargeOverride = objectMapper.createObjectNode();
        chargeOverride.put("productRatePlanChargeId", discountChargeId);

        // Start date policy - apply immediately
        ObjectNode startDate = objectMapper.createObjectNode();
        startDate.put("startDatePolicy", "ApplyToChargeStartDate");

        // End date policy - fixed duration if specified
        if (durationPeriods != null && durationPeriodType != null) {
            ObjectNode endDate = objectMapper.createObjectNode();
            endDate.put("endDatePolicy", "FixedPeriod");
            endDate.put("upToPeriodsType", durationPeriodType); // Day, Week, Month, Year
            endDate.put("upToPeriods", durationPeriods);
            chargeOverride.set("endDate", endDate);
        }

        chargeOverride.set("startDate", startDate);

        // Discount pricing
        ObjectNode pricing = objectMapper.createObjectNode();
        ObjectNode discount = objectMapper.createObjectNode();
        discount.put("discountPercentage", discountPercentage);
        discount.put("applyDiscountTo", applyTo != null ? applyTo : "RECURRING");
        discount.put("discountLevel", discountLevel != null ? discountLevel : "subscription");
        discount.put("applyToBillingPeriodPartially", true);
        pricing.set("discount", discount);

        chargeOverride.set("pricing", pricing);

        // Build subscribeToRatePlans with charge override
        // Note: This applies discount as an additional rate plan on the subscription
        ObjectNode subscribeToRatePlan = objectMapper.createObjectNode();
        subscribeToRatePlan.put("productRatePlanId", discountChargeId.split("~")[0]); // Extract product rate plan ID
        subscribeToRatePlan.putArray("chargeOverrides").add(chargeOverride);

        // Build addProduct order action
        ObjectNode addProduct = objectMapper.createObjectNode();
        addProduct.putArray("subscribeToRatePlans").add(subscribeToRatePlan);

        ObjectNode orderAction = objectMapper.createObjectNode();
        orderAction.put("type", "AddProduct");
        orderAction.putArray("triggerDates").add(
            objectMapper.createObjectNode().put("name", "ContractEffective").put("triggerDate", today)
        );
        orderAction.set("addProduct", addProduct);

        // Build subscription data
        ObjectNode subscriptionData = objectMapper.createObjectNode();
        subscriptionData.put("subscriptionNumber", zuoraSubscriptionNumber);
        subscriptionData.putArray("orderActions").add(orderAction);

        // Build order request
        ObjectNode body = objectMapper.createObjectNode();
        body.put("existingAccountNumber", zuoraAccountNumber);
        body.put("orderDate", today);
        body.put("description", "Discount application via Chezan Fitness");
        body.putArray("subscriptions").add(subscriptionData);

        System.out.println("Zuora Orders API Discount Request: " + body.toString());

        JsonNode response = executePost(apiUrl, body.toString());
        System.out.println("Zuora Orders API Discount Response: " + response.toString());

        // Check for success
        if (response.has("success") && !response.get("success").asBoolean()) {
            String errorMsg = response.has("reasons") ? response.get("reasons").toString() : "Unknown error";
            throw new RuntimeException("Zuora discount application failed: " + errorMsg);
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.put("subscriptionNumber", zuoraSubscriptionNumber);
        return result;
    }

    /**
     * Create a discount product rate plan charge
     * API: POST /v1/object/product-rate-plan-charge
     */
    public JsonNode createDiscountCharge(String name, String productRatePlanId,
                                          Double discountPercentage, String discountLevel,
                                          String applyTo) throws Exception {
        // Test mode: return mock success response
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock discount charge creation");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("success", true);
            mockResponse.put("id", "test-discount-charge-" + System.currentTimeMillis());
            return mockResponse;
        }

        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/object/product-rate-plan-charge";

        ObjectNode body = objectMapper.createObjectNode();
        body.put("Name", name);
        body.put("ProductRatePlanId", productRatePlanId);
        body.put("ChargeType", "Recurring");
        body.put("ChargeModel", "Discount Percentage");
        body.put("BillCycleType", "DefaultFromCustomer");
        body.put("TriggerEvent", "ContractEffective");
        body.put("UseDiscountSpecificAccountingCode", false);

        // Discount-specific fields
        ObjectNode tierData = objectMapper.createObjectNode();
        ObjectNode tier = objectMapper.createObjectNode();
        tier.put("Price", discountPercentage);
        tier.put("Currency", "USD");
        tierData.putArray("ProductRatePlanChargeTier").add(tier);
        body.set("ProductRatePlanChargeTierData", tierData);

        System.out.println("Creating discount charge in Zuora: " + body.toString());

        return executePost(apiUrl, body.toString());
    }

    /**
     * Delete payment method
     * API: DELETE /v1/payment-methods/{paymentMethodId}
     */
    public JsonNode deletePaymentMethod(String paymentMethodId) throws Exception {
        // Test mode: return mock success response
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock payment method deletion");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("success", true);
            return mockResponse;
        }

        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/payment-methods/" + paymentMethodId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(response);
    }

    /**
     * Set payment method as default
     * API: PUT /v1/payment-methods/{paymentMethodId}
     */
    public JsonNode setDefaultPaymentMethod(String paymentMethodId) throws Exception {
        // Test mode: return mock success response
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock default payment method update");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("success", true);
            mockResponse.put("id", paymentMethodId);
            return mockResponse;
        }

        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/payment-methods/" + paymentMethodId;

        ObjectNode body = objectMapper.createObjectNode();
        body.put("makeDefault", true);

        return executePut(apiUrl, body.toString());
    }

    /**
     * Verify payment method (verify card/bank account)
     * API: PUT /v1/payment-methods/{paymentMethodId}/verify
     */
    public JsonNode verifyPaymentMethod(String paymentMethodId) throws Exception {
        // Test mode: return mock success response
        if ("test".equals(clientId) || "test".equals(clientSecret)) {
            System.out.println("Zuora TEST MODE: Returning mock payment method verification");
            ObjectNode mockResponse = objectMapper.createObjectNode();
            mockResponse.put("success", true);
            mockResponse.put("verificationStatus", "Valid");
            return mockResponse;
        }

        authenticate();
        String apiUrl = zuoraBaseUrl + "/v1/payment-methods/" + paymentMethodId + "/verify";

        ObjectNode body = objectMapper.createObjectNode();
        body.put("securityCode", "skip"); // Skip CVV verification for stored cards

        return executePut(apiUrl, body.toString());
    }

    // Helper methods
    private JsonNode executeGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(response);
    }

    private JsonNode executePost(String url, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(response);
    }

    private JsonNode executePut(String url, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(response);
    }

    private JsonNode handleResponse(HttpResponse<String> response) throws Exception {
        JsonNode json = objectMapper.readTree(response.body());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return json;
        }
        throw new RuntimeException("Zuora API error: " + json.toString());
    }
}
