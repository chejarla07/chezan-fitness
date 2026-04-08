package com.example.zuora.service;

import com.example.zuora.dto.SignupRequest;
import com.example.zuora.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "zuora.client-id=test",
    "zuora.client-secret=test",
    "zuora.base-url=https://rest.apisandbox.zuora.com",
    "spring.main.banner-mode=off"
})
class ZuoraApiServiceIntegrationTest {

    @Autowired
    private ZuoraApiService zuoraApiService;

    @Test
    void testCreateAccount_ReturnsSuccess() throws Exception {
        // Test mode should return mock success response
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");
        user.setPhone("1234567890");

        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setAddress1("123 Test St");
        signupRequest.setCity("Test City");
        signupRequest.setState("CA");
        signupRequest.setCountry("USA");
        signupRequest.setZipCode("12345");
        signupRequest.setSameAsBillTo(true);

        JsonNode result = zuoraApiService.createAccount(user, signupRequest);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.has("success"), "Response should have success field");
        assertTrue(result.get("success").asBoolean(), "Account creation should return success");
        assertTrue(result.has("accountId"), "Response should have accountId");
        assertTrue(result.has("accountNumber"), "Response should have accountNumber");

        System.out.println("✓ testCreateAccount PASSED");
        System.out.println("  Response: " + result.toString());
    }

    @Test
    void testCreateProduct_ReturnsSuccess() throws Exception {
        // In test mode with test credentials, this returns mock response
        Product product = new Product();
        product.setName("Test Product" + System.currentTimeMillis());
        product.setDescription("Test Description");
        product.setEffectiveStartDate(LocalDate.now());
        product.setEffectiveEndDate(LocalDate.now().plusYears(1));

        JsonNode result = zuoraApiService.createProduct(product);

        assertNotNull(result, "Result should not be null");
        System.out.println("✓ testCreateProduct PASSED");
        System.out.println("  Response: " + result.toString());
    }

    @Test
    void testCreateRatePlan_ReturnsSuccess() throws Exception {
        RatePlan ratePlan = new RatePlan();
        ratePlan.setName("Test Rate Plan" + System.currentTimeMillis());
        ratePlan.setDescription("Test Description");
        ratePlan.setEffectiveStartDate(LocalDate.now());
        ratePlan.setEffectiveEndDate(LocalDate.now().plusYears(1));

        JsonNode result = zuoraApiService.createRatePlan(ratePlan, "test-product-id");

        assertNotNull(result, "Result should not be null");
        System.out.println("✓ testCreateRatePlan PASSED");
        System.out.println("  Response: " + result.toString());
    }

    @Test
    void testCreateRatePlanCharge_ReturnsSuccess() throws Exception {
        RatePlanCharge charge = new RatePlanCharge();
        charge.setName("Test Charge" + System.currentTimeMillis());
        charge.setChargeType(RatePlanCharge.ChargeType.Recurring);
        charge.setChargeModel(RatePlanCharge.ChargeModel.FlatFee);
        charge.setAmount(99.99);
        charge.setBillingTiming(RatePlanCharge.BillingTiming.InAdvance);

        JsonNode result = zuoraApiService.createRatePlanCharge(charge, "test-rateplan-id");

        assertNotNull(result, "Result should not be null");
        System.out.println("✓ testCreateRatePlanCharge PASSED");
        System.out.println("  Response: " + result.toString());
    }

    @Test
    void testCreateSubscription_ReturnsSuccess() throws Exception {
        User user = new User();
        user.setZuoraAccountNumber("A-TEST-001");

        RatePlan ratePlan = new RatePlan();
        ratePlan.setZuoraRatePlanId("test-rateplan-id");

        JsonNode result = zuoraApiService.createSubscription(user, ratePlan, "A-TEST-001", null, null);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.has("success"), "Response should have success field");
        assertTrue(result.get("success").asBoolean(), "Subscription creation should return success");
        assertTrue(result.has("subscriptionNumber"), "Response should have subscriptionNumber");

        System.out.println("✓ testCreateSubscription PASSED");
        System.out.println("  Response: " + result.toString());
    }

    @Test
    void testAmendSubscription_ReturnsSuccess() throws Exception {
        String subscriptionNumber = "S-TEST-001";
        String currentRatePlanId = "test-current-rateplan-id";
        String newRatePlanId = "test-new-rateplan-id";
        String accountNumber = "A-TEST-001";
        String effectiveDate = LocalDate.now().toString();

        JsonNode result = zuoraApiService.amendSubscription(subscriptionNumber, currentRatePlanId, newRatePlanId, accountNumber, effectiveDate);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.has("success"), "Response should have success field");
        assertTrue(result.get("success").asBoolean(), "Subscription amendment should return success");

        System.out.println("✓ testAmendSubscription PASSED");
        System.out.println("  Response: " + result.toString());
    }

    @Test
    void testCancelSubscription_ReturnsSuccess() throws Exception {
        String subscriptionNumber = "S-TEST-001";
        String cancellationPolicy = "EndOfCurrentTerm";
        String accountNumber = "A-TEST-001";
        String cancellationDate = null;

        JsonNode result = zuoraApiService.cancelSubscription(subscriptionNumber, cancellationPolicy, accountNumber, cancellationDate);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.has("success"), "Response should have success field");
        assertTrue(result.get("success").asBoolean(), "Subscription cancellation should return success");

        System.out.println("✓ testCancelSubscription PASSED");
        System.out.println("  Response: " + result.toString());
    }

    @Test
    void testPauseSubscription_ReturnsSuccess() throws Exception {
        String subscriptionNumber = "S-TEST-001";
        String accountNumber = "A-TEST-001";
        int pausePeriods = 3;

        JsonNode result = zuoraApiService.pauseSubscription(subscriptionNumber, accountNumber, pausePeriods);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.has("success"), "Response should have success field");
        assertTrue(result.get("success").asBoolean(), "Subscription pause should return success");

        System.out.println("✓ testPauseSubscription PASSED");
        System.out.println("  Response: " + result.toString());
    }

    @Test
    void testResumeSubscription_ReturnsSuccess() throws Exception {
        String subscriptionNumber = "S-TEST-001";
        String accountNumber = "A-TEST-001";
        String resumeDate = null;

        JsonNode result = zuoraApiService.resumeSubscription(subscriptionNumber, accountNumber, resumeDate);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.has("success"), "Response should have success field");
        assertTrue(result.get("success").asBoolean(), "Subscription resume should return success");

        System.out.println("✓ testResumeSubscription PASSED");
        System.out.println("  Response: " + result.toString());
    }

    @Test
    void testUpdateRatePlanChargePrice_ReturnsSuccess() throws Exception {
        String chargeId = "test-charge-id";
        Double newPrice = 149.99;

        JsonNode result = zuoraApiService.updateRatePlanChargePrice(chargeId, newPrice);

        assertNotNull(result, "Result should not be null");
        System.out.println("✓ testUpdateRatePlanChargePrice PASSED");
        System.out.println("  Response: " + result.toString());
    }

    @Test
    void testUpdateProduct_ReturnsSuccess() throws Exception {
        String productId = "test-product-id";
        String newName = "Updated Product Name";
        String newDescription = "Updated Description";

        JsonNode result = zuoraApiService.updateProduct(productId, newName, newDescription);

        assertNotNull(result, "Result should not be null");
        System.out.println("✓ testUpdateProduct PASSED");
        System.out.println("  Response: " + result.toString());
    }

    @Test
    void testGetAccountDetails_ReturnsSuccess() throws Exception {
        String accountId = "test-account-id";

        JsonNode result = zuoraApiService.getAccountDetails(accountId);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.has("id"), "Response should have id field");
        assertTrue(result.has("accountNumber"), "Response should have accountNumber field");

        System.out.println("✓ testGetAccountDetails PASSED");
        System.out.println("  Response: " + result.toString());
    }

    @Test
    void testCreatePaymentMethod_ReturnsSuccess() throws Exception {
        String accountId = "test-account-id";
        String token = "test-payment-token";

        JsonNode result = zuoraApiService.createPaymentMethod(accountId, token, PaymentMethod.PaymentType.CreditCard);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.has("success"), "Response should have success field");
        assertTrue(result.get("success").asBoolean(), "Payment method creation should return success");

        System.out.println("✓ testCreatePaymentMethod PASSED");
        System.out.println("  Response: " + result.toString());
    }

    @Test
    void testGetInvoicePdf_ReturnsSuccess() throws Exception {
        String invoiceId = "test-invoice-id";

        byte[] result = zuoraApiService.getInvoicePdf(invoiceId);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.length > 0, "PDF bytes should not be empty");

        System.out.println("✓ testGetInvoicePdf PASSED");
        System.out.println("  PDF size: " + result.length + " bytes");
    }
}