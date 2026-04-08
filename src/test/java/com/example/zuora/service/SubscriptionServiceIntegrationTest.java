package com.example.zuora.service;

import com.example.zuora.dto.CancelSubscriptionRequest;
import com.example.zuora.dto.SignupRequest;
import com.example.zuora.dto.SubscriptionUpdateRequest;
import com.example.zuora.model.*;
import com.example.zuora.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "zuora.client-id=test",
    "zuora.client-secret=test"
})
@Transactional
class SubscriptionServiceIntegrationTest {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RatePlanRepository ratePlanRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private ProductRepository productRepository;

    private User testUser;
    private RatePlan testRatePlan;

    @BeforeEach
    void setUp() throws Exception {
        // Create a test user
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setEmail("subscription.test@example.com");
        signupRequest.setPassword("Password123!");
        signupRequest.setFirstName("Subscription");
        signupRequest.setLastName("Test");
        signupRequest.setPhone("1234567890");
        signupRequest.setAddress1("123 Main St");
        signupRequest.setCity("San Francisco");
        signupRequest.setState("CA");
        signupRequest.setZipCode("94105");
        signupRequest.setCountry("USA");

        testUser = userService.createUser(signupRequest).getUser();

        // Create a test product and rate plan
        Product product = new Product();
        product.setName("Test Product");
        product.setDescription("Test product for subscriptions");
        product.setCategory(Product.Category.MEMBERSHIP);
        product.setStatus(Product.Status.Active);
        product = productRepository.save(product);

        testRatePlan = new RatePlan();
        testRatePlan.setName("Test Plan");
        testRatePlan.setDescription("Test rate plan");
        testRatePlan.setProduct(product);
        testRatePlan.setStatus(RatePlan.Status.Active);
        testRatePlan = ratePlanRepository.save(testRatePlan);
    }

    @Test
    void hasActiveSubscription_FalseForNewUser() {
        assertFalse(subscriptionService.hasActiveSubscription(testUser.getId()));
    }

    @Test
    void getActiveSubscription_NoSubscription() {
        Subscription result = subscriptionService.getActiveSubscription(testUser.getId());
        assertNull(result);
    }

    @Test
    void upgradeSubscription_CreateNewSubscription() throws Exception {
        // Setup
        SubscriptionUpdateRequest request = new SubscriptionUpdateRequest();
        request.setNewRatePlanId(testRatePlan.getId());

        // Execute
        Subscription result = subscriptionService.upgradeSubscription(request, testUser);

        // Verify
        assertNotNull(result);
        assertEquals(testRatePlan.getId(), result.getRatePlan().getId());
        assertEquals(Subscription.SubscriptionStatus.Active, result.getStatus());
        assertTrue(subscriptionService.hasActiveSubscription(testUser.getId()));
    }

    @Test
    void upgradeSubscription_RatePlanNotFound() {
        SubscriptionUpdateRequest request = new SubscriptionUpdateRequest();
        request.setNewRatePlanId(99999L);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            subscriptionService.upgradeSubscription(request, testUser);
        });

        assertEquals("New rate plan not found", exception.getMessage());
    }

    @Test
    void cancelSubscription_NoActiveSubscription() {
        CancelSubscriptionRequest request = new CancelSubscriptionRequest();
        request.setReason("Test cancellation");
        request.setCancelImmediately(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            subscriptionService.cancelSubscription(request, testUser);
        });

        assertEquals("No active subscription to cancel", exception.getMessage());
    }

    @Test
    void getUserSubscriptions_Success() throws Exception {
        // First create a subscription
        SubscriptionUpdateRequest request = new SubscriptionUpdateRequest();
        request.setNewRatePlanId(testRatePlan.getId());
        subscriptionService.upgradeSubscription(request, testUser);

        // Execute
        var subscriptions = subscriptionService.getUserSubscriptions(testUser.getId());

        // Verify
        assertNotNull(subscriptions);
        assertEquals(1, subscriptions.size());
    }

    @Test
    void getActiveUserSubscriptions_Success() throws Exception {
        // Create subscription
        SubscriptionUpdateRequest request = new SubscriptionUpdateRequest();
        request.setNewRatePlanId(testRatePlan.getId());
        subscriptionService.upgradeSubscription(request, testUser);

        // Execute
        var subscriptions = subscriptionService.getActiveUserSubscriptions(testUser.getId());

        // Verify
        assertEquals(1, subscriptions.size());
        assertEquals(Subscription.SubscriptionStatus.Active, subscriptions.get(0).getStatus());
    }

    @Test
    void getSubscriptionById_Success() throws Exception {
        // Create subscription
        SubscriptionUpdateRequest request = new SubscriptionUpdateRequest();
        request.setNewRatePlanId(testRatePlan.getId());
        Subscription created = subscriptionService.upgradeSubscription(request, testUser);

        // Execute
        Subscription result = subscriptionService.getSubscriptionById(created.getId());

        // Verify
        assertNotNull(result);
        assertEquals(created.getId(), result.getId());
    }

    @Test
    void getSubscriptionById_NotFound() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            subscriptionService.getSubscriptionById(99999L);
        });

        assertEquals("Subscription not found: 99999", exception.getMessage());
    }
}
