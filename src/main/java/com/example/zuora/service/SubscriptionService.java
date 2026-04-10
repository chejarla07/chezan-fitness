package com.example.zuora.service;

import com.example.zuora.dto.*;
import com.example.zuora.model.*;
import com.example.zuora.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final RatePlanRepository ratePlanRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final ZuoraApiService zuoraApiService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                              RatePlanRepository ratePlanRepository,
                              PaymentMethodRepository paymentMethodRepository,
                              ZuoraApiService zuoraApiService) {
        this.subscriptionRepository = subscriptionRepository;
        this.ratePlanRepository = ratePlanRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.zuoraApiService = zuoraApiService;
    }

    public List<Subscription> getUserSubscriptions(Long userId) {
        return subscriptionRepository.findByUserId(userId);
    }

    public List<Subscription> getActiveUserSubscriptions(Long userId) {
        return subscriptionRepository.findByUserIdAndStatus(userId, Subscription.SubscriptionStatus.Active);
    }

    public Subscription getSubscriptionById(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + id));
    }

    @Transactional
    public Subscription createSubscription(User user, SignupRequest signupRequest) throws Exception {
        // Validate required fields
        if (user.getZuoraAccountId() == null) {
            throw new RuntimeException("Cannot create subscription: User does not have a Zuora account");
        }

        if (signupRequest.getRatePlanId() == null) {
            throw new RuntimeException("Cannot create subscription: No rate plan selected");
        }

        // Get the selected rate plan
        RatePlan ratePlan = ratePlanRepository.findById(signupRequest.getRatePlanId())
                .orElseThrow(() -> new RuntimeException("Rate plan not found"));

        // Check if rate plan has Zuora ID - if not, create local subscription only
        boolean needsZuoraSync = ratePlan.getZuoraRatePlanId() == null || ratePlan.getZuoraRatePlanId().isEmpty();

        if (needsZuoraSync) {
            System.out.println("WARNING: Rate plan '" + ratePlan.getName() + "' is not synced to Zuora. Creating local subscription only.");
            System.out.println("Please sync products via Admin > Zuora Sync to enable Zuora integration.");

            // Create local subscription only
            Subscription subscription = new Subscription();
            subscription.setUser(user);
            subscription.setRatePlan(ratePlan);
            subscription.setStatus(Subscription.SubscriptionStatus.Active);
            subscription.setContractEffectiveDate(LocalDate.now());
            subscription.setTermStartDate(LocalDate.now());
            subscription.setAutoRenew(true);

            return subscriptionRepository.save(subscription);
        }

        // Get the recurring charge from the rate plan
        RatePlanCharge recurringCharge = ratePlan.getRecurringCharge();
        String zuoraChargeId = recurringCharge != null ? recurringCharge.getZuoraChargeId() : null;

        // Create subscription in Zuora WITHOUT payment method (payment is optional)
        // Pass the account NUMBER (not ID) for Zuora Orders API
        // Pass the charge ID for proper Zuora catalog integration
        JsonNode zuoraResponse = zuoraApiService.createSubscription(
                user,
                ratePlan,
                user.getZuoraAccountNumber(), // Use account number, not account ID
                null, // No payment method required at signup
                zuoraChargeId // Include rate plan charge ID
        );

        // Validate Zuora response
        if (!zuoraResponse.has("subscriptionId")) {
            throw new RuntimeException("Invalid response from Zuora: missing subscriptionId");
        }

        // Create local subscription
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setZuoraSubscriptionId(zuoraResponse.get("subscriptionId").asText());
        subscription.setZuoraSubscriptionNumber(
                zuoraResponse.has("subscriptionNumber")
                        ? zuoraResponse.get("subscriptionNumber").asText()
                        : zuoraResponse.get("subscriptionId").asText()
        );
        subscription.setRatePlan(ratePlan);
        subscription.setStatus(Subscription.SubscriptionStatus.Active);
        subscription.setContractEffectiveDate(LocalDate.now());
        subscription.setTermStartDate(LocalDate.now());
        subscription.setAutoRenew(true);

        return subscriptionRepository.save(subscription);
    }

    /**
     * Create a subscription with a custom start date (for users subscribing after cancellation)
     */
    @Transactional
    public Subscription createSubscriptionWithStartDate(User user, SignupRequest signupRequest, String startDateStr) throws Exception {
        // Validate required fields
        if (user.getZuoraAccountId() == null) {
            throw new RuntimeException("Cannot create subscription: User does not have a Zuora account");
        }

        if (signupRequest.getRatePlanId() == null) {
            throw new RuntimeException("Cannot create subscription: No rate plan selected");
        }

        // Get the selected rate plan
        RatePlan ratePlan = ratePlanRepository.findById(signupRequest.getRatePlanId())
                .orElseThrow(() -> new RuntimeException("Rate plan not found"));

        // Validate that rate plan has Zuora ID
        if (ratePlan.getZuoraRatePlanId() == null) {
            throw new RuntimeException("Rate plan is not synced with Zuora");
        }

        // Parse start date (if provided)
        LocalDate contractDate = LocalDate.now();
        LocalDate termStartDate = LocalDate.now();
        if (startDateStr != null && !startDateStr.isEmpty()) {
            try {
                termStartDate = LocalDate.parse(startDateStr);
                contractDate = termStartDate; // Contract date should match term start
            } catch (Exception e) {
                System.err.println("Failed to parse start date: " + startDateStr + ", using today");
            }
        }

        // Get the recurring charge from the rate plan
        RatePlanCharge recurringCharge = ratePlan.getRecurringCharge();
        String zuoraChargeId = recurringCharge != null ? recurringCharge.getZuoraChargeId() : null;

        // Create subscription in Zuora with start date
        JsonNode zuoraResponse = zuoraApiService.createSubscriptionWithStartDate(
                user,
                ratePlan,
                user.getZuoraAccountNumber(),
                null, // No payment method required at signup
                zuoraChargeId,
                contractDate.toString(),
                termStartDate.toString()
        );

        // Validate Zuora response
        if (!zuoraResponse.has("subscriptionId")) {
            throw new RuntimeException("Invalid response from Zuora: missing subscriptionId");
        }

        // Create local subscription
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setZuoraSubscriptionId(zuoraResponse.get("subscriptionId").asText());
        subscription.setZuoraSubscriptionNumber(
                zuoraResponse.has("subscriptionNumber")
                        ? zuoraResponse.get("subscriptionNumber").asText()
                        : zuoraResponse.get("subscriptionId").asText()
        );
        subscription.setRatePlan(ratePlan);
        subscription.setStatus(Subscription.SubscriptionStatus.Active);
        subscription.setContractEffectiveDate(contractDate);
        subscription.setTermStartDate(termStartDate);
        subscription.setAutoRenew(true);

        subscription = subscriptionRepository.save(subscription);

        // Add add-ons if selected
        if (signupRequest.getAddOnRatePlanIds() != null && !signupRequest.getAddOnRatePlanIds().isEmpty()) {
            for (Long addOnRatePlanId : signupRequest.getAddOnRatePlanIds()) {
                try {
                    addAddOnToSubscription(user, addOnRatePlanId, termStartDate.toString());
                } catch (Exception e) {
                    System.err.println("Failed to add add-on " + addOnRatePlanId + ": " + e.getMessage());
                    // Continue with other add-ons
                }
            }
        }

        return subscription;
    }

    @Transactional
    public Subscription upgradeSubscription(SubscriptionUpdateRequest request, User user) throws Exception {
        RatePlan newRatePlan = ratePlanRepository.findById(request.getNewRatePlanId())
                .orElseThrow(() -> new RuntimeException("New rate plan not found"));

        // Check if user has an existing subscription
        Subscription existingSubscription = getActiveSubscription(user.getId());

        if (existingSubscription == null) {
            // No existing subscription - create new one locally (Zuora integration optional)
            Subscription subscription = new Subscription();
            subscription.setUser(user);
            subscription.setRatePlan(newRatePlan);
            subscription.setStatus(Subscription.SubscriptionStatus.Active);
            subscription.setContractEffectiveDate(LocalDate.now());
            subscription.setTermStartDate(LocalDate.now());
            subscription.setAutoRenew(true);

            // Try to create in Zuora (optional)
            try {
                PaymentMethod defaultPayment = paymentMethodRepository
                        .findByUserIdAndIsDefault(user.getId(), true)
                        .orElse(null);

                JsonNode zuoraResponse = zuoraApiService.createSubscription(
                        user,
                        newRatePlan,
                        user.getZuoraAccountNumber(), // Use account number
                        defaultPayment != null ? defaultPayment.getZuoraPaymentMethodId() : null
                );

                if (zuoraResponse != null && zuoraResponse.has("subscriptionId")) {
                    subscription.setZuoraSubscriptionId(zuoraResponse.get("subscriptionId").asText());
                    subscription.setZuoraSubscriptionNumber(zuoraResponse.has("subscriptionNumber")
                            ? zuoraResponse.get("subscriptionNumber").asText()
                            : subscription.getZuoraSubscriptionId());
                }
            } catch (Exception e) {
                // Log but continue - subscription is created locally
                System.err.println("Zuora subscription creation skipped: " + e.getMessage());
            }

            return subscriptionRepository.save(subscription);
        }

        // Existing subscription - upgrade/downgrade it
        Subscription subscription = existingSubscription;

        if (!subscription.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Unauthorized to modify this subscription");
        }

        // Validate rate plan has Zuora ID
        if (newRatePlan.getZuoraRatePlanId() == null) {
            throw new RuntimeException("New rate plan is not synced with Zuora");
        }

        // Get current rate plan ID (from subscription's current rate plan)
        String currentRatePlanId = subscription.getRatePlan() != null ?
                subscription.getRatePlan().getZuoraRatePlanId() : null;
        if (currentRatePlanId == null) {
            throw new RuntimeException("Current subscription rate plan is not synced with Zuora");
        }

        // Amend in Zuora using Orders API - ChangePlan to swap rate plans
        String effectiveDate = request.getEffectiveDate();
        JsonNode zuoraResponse = zuoraApiService.amendSubscription(
                subscription.getZuoraSubscriptionNumber(),
                currentRatePlanId,
                newRatePlan.getZuoraRatePlanId(),
                user.getZuoraAccountNumber(),
                effectiveDate
        );

        // Update locally
        subscription.setRatePlan(newRatePlan);
        subscription.setUpdatedAt(java.time.LocalDateTime.now());

        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public void cancelSubscription(CancelSubscriptionRequest request, User user) throws Exception {
        // Check if user has an active subscription
        Subscription subscription = getActiveSubscription(user.getId());

        if (subscription == null) {
            throw new RuntimeException("No active subscription to cancel");
        }

        if (!subscription.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Unauthorized to cancel this subscription");
        }

        // Cancel in Zuora using Orders API
        // Use the cancellation policy from the request, or default to EndOfCurrentTerm
        String cancellationPolicy = request.getCancellationPolicy();
        if (cancellationPolicy == null || cancellationPolicy.isEmpty()) {
            cancellationPolicy = request.isCancelImmediately() ? "SpecificDate" : "EndOfCurrentTerm";
        }
        JsonNode zuoraResponse = zuoraApiService.cancelSubscription(
                subscription.getZuoraSubscriptionNumber(),
                cancellationPolicy,
                user.getZuoraAccountNumber(),
                request.getCancellationDate()
        );

        // Update locally
        subscription.setStatus(Subscription.SubscriptionStatus.Cancelled);
        subscription.setCancelReason(request.getReason());
        subscription.setCancellationDate(LocalDate.now());
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription pauseSubscription(Long userId, int pausePeriods) throws Exception {
        // Check if user has an active subscription
        Subscription subscription = getActiveSubscription(userId);

        if (subscription == null) {
            throw new RuntimeException("No active subscription to pause");
        }

        User user = subscription.getUser();

        // Pause in Zuora using Orders API
        JsonNode zuoraResponse = zuoraApiService.pauseSubscription(
                subscription.getZuoraSubscriptionNumber(),
                user.getZuoraAccountNumber(),
                pausePeriods
        );

        // Update locally - mark as suspended/paused
        subscription.setStatus(Subscription.SubscriptionStatus.Suspended);
        subscription.setSuspendEndDate(LocalDate.now().plusMonths(pausePeriods));
        subscription.setUpdatedAt(java.time.LocalDateTime.now());

        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription resumeSubscription(Long userId, String resumeDate) throws Exception {
        // Check if user has a suspended subscription
        List<Subscription> suspended = subscriptionRepository.findByUserIdAndStatus(userId, Subscription.SubscriptionStatus.Suspended);
        if (suspended.isEmpty()) {
            throw new RuntimeException("No suspended subscription to resume");
        }

        Subscription subscription = suspended.get(0);
        User user = subscription.getUser();

        // Resume in Zuora using Orders API with selected resume date
        JsonNode zuoraResponse = zuoraApiService.resumeSubscription(
                subscription.getZuoraSubscriptionNumber(),
                user.getZuoraAccountNumber(),
                resumeDate
        );

        // Update locally - mark as active and clear suspend end date
        subscription.setStatus(Subscription.SubscriptionStatus.Active);
        subscription.setSuspendEndDate(null);
        subscription.setUpdatedAt(java.time.LocalDateTime.now());

        return subscriptionRepository.save(subscription);
    }

    public boolean hasActiveSubscription(Long userId) {
        return subscriptionRepository.countByUserIdAndStatus(userId, Subscription.SubscriptionStatus.Active) > 0;
    }

    public Subscription getActiveSubscription(Long userId) {
        List<Subscription> active = subscriptionRepository.findByUserIdAndStatus(userId, Subscription.SubscriptionStatus.Active);
        return active.isEmpty() ? null : active.get(0);
    }

    /**
     * Fetch subscription details from Zuora to get suspend end date
     */
    @Transactional
    public void fetchAndPopulateSuspendDate(Subscription subscription) throws Exception {
        if (subscription.getZuoraSubscriptionId() == null) {
            return;
        }

        // Fetch subscription details from Zuora
        JsonNode zuoraSub = zuoraApiService.getSubscription(subscription.getZuoraSubscriptionId());

        // Try to find the suspend end date from Zuora response
        // Zuora returns subscription details including status and suspend dates
        if (zuoraSub.has("status") && "Suspended".equals(zuoraSub.get("status").asText())) {
            // Calculate approximate suspend end date based on term start/end dates
            // or use a reasonable default (e.g., 1 month from now if not specified)
            if (zuoraSub.has("termEndDate")) {
                String termEndDate = zuoraSub.get("termEndDate").asText();
                subscription.setSuspendEndDate(LocalDate.parse(termEndDate));
                subscriptionRepository.save(subscription);
            } else {
                // Default: assume 1 month suspension from today
                subscription.setSuspendEndDate(LocalDate.now().plusMonths(1));
                subscriptionRepository.save(subscription);
            }
        }
    }

    /**
     * Add an add-on rate plan to an existing subscription
     */
    @Transactional
    public Subscription addAddOnToSubscription(User user, Long addOnRatePlanId, String startDateStr) throws Exception {
        // Get the user's active subscription
        Subscription subscription = getActiveSubscription(user.getId());
        if (subscription == null) {
            throw new RuntimeException("No active subscription found");
        }

        // Get the add-on rate plan
        RatePlan addOnRatePlan = ratePlanRepository.findById(addOnRatePlanId)
                .orElseThrow(() -> new RuntimeException("Add-on rate plan not found"));

        // Validate that rate plan has Zuora ID
        if (addOnRatePlan.getZuoraRatePlanId() == null) {
            throw new RuntimeException("Add-on rate plan is not synced with Zuora");
        }

        // Parse start date
        LocalDate effectiveDate = LocalDate.now();
        if (startDateStr != null && !startDateStr.isEmpty()) {
            try {
                effectiveDate = LocalDate.parse(startDateStr);
            } catch (Exception e) {
                System.err.println("Failed to parse start date: " + startDateStr + ", using today");
            }
        }

        // Get the charge for the add-on
        RatePlanCharge addOnCharge = addOnRatePlan.getRecurringCharge();
        String zuoraChargeId = addOnCharge != null ? addOnCharge.getZuoraChargeId() : null;

        // Add add-on to subscription in Zuora using Orders API
        JsonNode zuoraResponse = zuoraApiService.addRatePlanToSubscription(
                subscription.getZuoraSubscriptionNumber(),
                addOnRatePlan.getZuoraRatePlanId(),
                user.getZuoraAccountNumber(),
                effectiveDate.toString(),
                zuoraChargeId
        );

        // Note: We don't create a new Subscription entity for add-ons
        // Add-ons are tracked as part of the subscription in Zuora
        // In the future, we could add a separate entity to track add-ons locally

        return subscription;
    }

    /**
     * Calculate cancellation options with estimated credits for a subscription
     * Valid Zuora cancellation policies: EndOfCurrentTerm, EndOfLastInvoicePeriod, SpecificDate
     */
    public List<CancellationOptions> calculateCancellationOptions(Subscription subscription) throws Exception {
        List<CancellationOptions> options = new ArrayList<>();

        // Get monthly price for calculations
        Double monthlyPrice = subscription.getMonthlyPrice() != null ? subscription.getMonthlyPrice() : 0.0;

        if (subscription.getZuoraSubscriptionId() == null || subscription.getZuoraSubscriptionNumber() == null) {
            // Return basic options without Zuora data
            LocalDate today = LocalDate.now();
            LocalDate termEnd = today.plusYears(1);

            // Option 1: End of Current Term - no refund, service continues
            CancellationOptions endOfTerm = new CancellationOptions(
                "EndOfCurrentTerm",
                "End of Current Term",
                termEnd,
                0.0,  // No credit
                0.0,   // No debit
                0.0,   // No invoice balance
                "Your subscription will remain active until the end of the current term. No charges or credits applied."
            );
            options.add(endOfTerm);

            // Option 2: End of Last Invoice Period
            LocalDate lastInvoiceEnd = today.withDayOfMonth(today.lengthOfMonth());
            long daysRemaining = ChronoUnit.DAYS.between(today, lastInvoiceEnd);
            double proratedCredit = monthlyPrice * (daysRemaining / 30.0);
            CancellationOptions endOfInvoice = new CancellationOptions(
                "EndOfLastInvoicePeriod",
                "End of Last Invoice Period",
                lastInvoiceEnd,
                Math.max(0, proratedCredit),
                0.0,
                0.0,
                "Your subscription ends at the end of the current billing period. You may receive a prorated credit for unused days."
            );
            options.add(endOfInvoice);

            // Option 3: Immediate cancellation
            CancellationOptions immediate = new CancellationOptions(
                "SpecificDate",
                "Cancel Immediately",
                today,
                monthlyPrice, // Full month credit
                0.0,
                0.0,
                "Your subscription will be cancelled immediately. You will receive credit for the remaining billing period."
            );
            options.add(immediate);

            return options;
        }

        // Fetch subscription details from Zuora
        JsonNode zuoraSub = zuoraApiService.getSubscription(subscription.getZuoraSubscriptionId());

        // Try to fetch invoice balance from Zuora
        double invoiceBalance = 0.0;
        try {
            if (subscription.getUser() != null && subscription.getUser().getZuoraAccountNumber() != null) {
                JsonNode invoices = zuoraApiService.getInvoices(subscription.getUser().getZuoraAccountNumber());
                if (invoices != null && invoices.has("invoices")) {
                    for (JsonNode invoice : invoices.get("invoices")) {
                        if (invoice.has("status") && "Posted".equals(invoice.get("status").asText())) {
                            if (invoice.has("balance")) {
                                invoiceBalance += invoice.get("balance").asDouble();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not fetch invoice balance: " + e.getMessage());
        }

        // Get key dates from Zuora response
        LocalDate today = LocalDate.now();
        LocalDate termStartDate = zuoraSub.has("termStartDate") ? LocalDate.parse(zuoraSub.get("termStartDate").asText()) : today;
        LocalDate termEndDate = zuoraSub.has("termEndDate") ? LocalDate.parse(zuoraSub.get("termEndDate").asText()) : today.plusYears(1);

        // Calculate last invoice period end (approximate - typically end of current billing period)
        LocalDate endOfLastInvoicePeriod = today.withDayOfMonth(today.lengthOfMonth());

        // Calculate days remaining for prorated credits
        long daysToTermEnd = Math.max(0, ChronoUnit.DAYS.between(today, termEndDate));
        long daysToInvoiceEnd = Math.max(0, ChronoUnit.DAYS.between(today, endOfLastInvoicePeriod));

        // Option 1: End of Current Term - No credit, service continues to end of term
        double creditEndOfTerm = 0.0; // No credit - you've already paid for the full term
        double debitEndOfTerm = invoiceBalance; // Any outstanding invoice balance still owed
        CancellationOptions endOfTerm = new CancellationOptions(
            "EndOfCurrentTerm",
            "End of Current Term",
            termEndDate,
            creditEndOfTerm,
            debitEndOfTerm,
            invoiceBalance,
            daysToTermEnd > 0
                ? String.format("Your subscription remains active until %s. You've already paid for this period, so no additional charges or credits apply.", termEndDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")))
                : "Your subscription will end at the current term end date."
        );
        options.add(endOfTerm);

        // Option 2: End of Last Invoice Period - Prorated credit for unused days
        double creditInvoicePeriod = monthlyPrice * (daysToInvoiceEnd / 30.0); // Prorated credit
        double debitInvoicePeriod = invoiceBalance; // Outstanding balance
        CancellationOptions endOfInvoice = new CancellationOptions(
            "EndOfLastInvoicePeriod",
            "End of Current Billing Period",
            endOfLastInvoicePeriod,
            Math.max(0, creditInvoicePeriod),
            debitInvoicePeriod,
            invoiceBalance,
            String.format("Your subscription ends on %s. You may receive a prorated credit of $%.2f for unused days in the current billing period.",
                endOfLastInvoicePeriod.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")),
                Math.max(0, creditInvoicePeriod))
        );
        options.add(endOfInvoice);

        // Option 3: Immediate cancellation - Full credit for current period
        // Customer gets credit for remaining days in billing period
        long daysFromTermStart = Math.max(0, ChronoUnit.DAYS.between(termStartDate, today));
        long totalTermDays = Math.max(1, ChronoUnit.DAYS.between(termStartDate, termEndDate));
        double usedAmount = monthlyPrice * (daysFromTermStart / (double) totalTermDays * 12); // Approximate used amount
        double creditImmediate = Math.max(0, monthlyPrice - usedAmount);

        CancellationOptions immediate = new CancellationOptions(
            "SpecificDate",
            "Cancel Immediately",
            today,
            Math.max(0, creditImmediate),
            invoiceBalance,
            invoiceBalance,
            String.format("Your subscription will be cancelled immediately. You may receive a credit of $%.2f for the unused portion of your subscription.",
                Math.max(0, creditImmediate))
        );
        options.add(immediate);

        return options;
    }
}
