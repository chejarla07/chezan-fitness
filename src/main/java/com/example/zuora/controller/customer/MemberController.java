package com.example.zuora.controller.customer;

import com.example.zuora.dto.*;
import com.example.zuora.model.*;
import com.example.zuora.repository.PaymentMethodRepository;
import com.example.zuora.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/member")
public class MemberController {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final ProductService productService;
    private final InvoiceService invoiceService;
    private final PaymentMethodRepository paymentMethodRepository;
    private final HealthQuestionnaireService healthQuestionnaireService;
    private final WaiverService waiverService;
    private final CheckInService checkInService;

    public MemberController(UserService userService,
                           SubscriptionService subscriptionService,
                           ProductService productService,
                           InvoiceService invoiceService,
                           PaymentMethodRepository paymentMethodRepository,
                           HealthQuestionnaireService healthQuestionnaireService,
                           WaiverService waiverService,
                           CheckInService checkInService) {
        this.userService = userService;
        this.subscriptionService = subscriptionService;
        this.productService = productService;
        this.invoiceService = invoiceService;
        this.paymentMethodRepository = paymentMethodRepository;
        this.healthQuestionnaireService = healthQuestionnaireService;
        this.waiverService = waiverService;
        this.checkInService = checkInService;
    }

    private User getCurrentUser(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName());
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);
        List<Subscription> subscriptions = subscriptionService.getUserSubscriptions(user.getId());
        List<PaymentMethod> paymentMethods = userService.getUserPaymentMethods(user.getId());
        List<Invoice> invoices = invoiceService.getUserInvoices(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("subscriptions", subscriptions);
        model.addAttribute("paymentMethods", paymentMethods);
        model.addAttribute("invoices", invoices);
        model.addAttribute("hasActiveSubscription", subscriptionService.hasActiveSubscription(user.getId()));

        // Add visit statistics
        try {
            model.addAttribute("visitCount", checkInService.getVisitCount(user.getId()));
            model.addAttribute("isCheckedIn", checkInService.isCheckedIn(user.getId()));
        } catch (Exception e) {
            model.addAttribute("visitCount", 0);
            model.addAttribute("isCheckedIn", false);
        }

        // Fetch Zuora contact details if available
        if (user.getZuoraAccountId() != null) {
            try {
                model.addAttribute("billToContact", userService.getBillToContact(user));
                model.addAttribute("soldToContact", userService.getSoldToContact(user));
            } catch (Exception e) {
                // Log but don't fail
                System.err.println("Failed to fetch Zuora contact details: " + e.getMessage());
            }
        }

        return "member/dashboard";
    }

    // Profile Management
    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);
        model.addAttribute("user", user);

        // Fetch Zuora contact details if available
        if (user.getZuoraAccountId() != null) {
            try {
                model.addAttribute("zuoraAccount", userService.getZuoraAccountDetails(user));
                model.addAttribute("billToContact", userService.getBillToContact(user));
                model.addAttribute("soldToContact", userService.getSoldToContact(user));
            } catch (Exception e) {
                // Log error but don't fail - display user data only
                System.err.println("Failed to fetch Zuora contact details: " + e.getMessage());
            }
        }

        return "member/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(Authentication authentication,
                               @RequestParam String firstName,
                               @RequestParam String lastName,
                               @RequestParam String phone,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            userService.updateUserProfile(user.getId(), firstName, lastName, phone);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update profile: " + e.getMessage());
        }
        return "redirect:/member/profile";
    }

    // Subscription Management
    @GetMapping("/subscriptions")
    public String subscriptions(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);
        List<Subscription> subscriptions = subscriptionService.getUserSubscriptions(user.getId());

        // For suspended subscriptions without suspendEndDate, try to fetch from Zuora
        for (Subscription sub : subscriptions) {
            if (sub.getStatus() == Subscription.SubscriptionStatus.Suspended && sub.getSuspendEndDate() == null) {
                try {
                    subscriptionService.fetchAndPopulateSuspendDate(sub);
                } catch (Exception e) {
                    // Log but continue - will show empty in UI
                    System.err.println("Failed to fetch suspend date for subscription " + sub.getZuoraSubscriptionNumber() + ": " + e.getMessage());
                }
            }
        }

        model.addAttribute("subscriptions", subscriptions);
        model.addAttribute("availablePlans", productService.getActiveProductsByCategory(Product.Category.MEMBERSHIP));
        model.addAttribute("hasActiveSubscription", subscriptionService.hasActiveSubscription(user.getId()));

        // Calculate cancellation options for active subscriptions
        for (Subscription sub : subscriptions) {
            if (sub.getStatus() == Subscription.SubscriptionStatus.Active) {
                try {
                    model.addAttribute("cancellationOptions", subscriptionService.calculateCancellationOptions(sub));
                    break; // Only calculate for first active subscription
                } catch (Exception e) {
                    System.err.println("Failed to calculate cancellation options: " + e.getMessage());
                }
            }
        }

        return "member/subscriptions";
    }

    @PostMapping("/subscriptions/upgrade")
    public String upgradeSubscription(Authentication authentication,
                                     @ModelAttribute SubscriptionUpdateRequest request,
                                     RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            subscriptionService.upgradeSubscription(request, user);
            redirectAttributes.addFlashAttribute("success", "Subscription upgraded successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upgrade: " + e.getMessage());
        }
        return "redirect:/member/subscriptions";
    }

    @PostMapping("/subscriptions/cancel")
    public String cancelSubscription(Authentication authentication,
                                    @ModelAttribute CancelSubscriptionRequest request,
                                    RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            subscriptionService.cancelSubscription(request, user);
            redirectAttributes.addFlashAttribute("success", "Subscription cancelled in Zuora");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to cancel: " + e.getMessage());
        }
        return "redirect:/member/subscriptions";
    }

    @PostMapping("/subscriptions/pause")
    public String pauseSubscription(Authentication authentication,
                                   @RequestParam(defaultValue = "1") int pauseMonths,
                                   RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            subscriptionService.pauseSubscription(user.getId(), pauseMonths);
            redirectAttributes.addFlashAttribute("success", "Subscription paused for " + pauseMonths + " month(s) in Zuora");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to pause: " + e.getMessage());
        }
        return "redirect:/member/subscriptions";
    }

    @PostMapping("/subscriptions/resume")
    public String resumeSubscription(Authentication authentication,
                                    @RequestParam(required = false) String resumeDate,
                                    RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            subscriptionService.resumeSubscription(user.getId(), resumeDate);
            redirectAttributes.addFlashAttribute("success", "Subscription resumed successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to resume: " + e.getMessage());
        }
        return "redirect:/member/subscriptions";
    }

    // Browse Products - For users who cancelled and want to subscribe to new products
    @GetMapping("/products")
    public String browseProducts(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);
        model.addAttribute("user", user);
        model.addAttribute("products", productService.getActiveProductsByCategory(Product.Category.MEMBERSHIP));
        model.addAttribute("paymentMethods", userService.getUserPaymentMethods(user.getId()));
        model.addAttribute("hasActiveSubscription", subscriptionService.hasActiveSubscription(user.getId()));
        model.addAttribute("subscribeRequest", new SubscribeRequest());
        return "member/products";
    }

    // Subscribe to a new product after cancellation
    @PostMapping("/subscriptions/new")
    public String subscribeToProduct(Authentication authentication,
                                   @ModelAttribute SubscribeRequest request,
                                   RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);

            // Create a SignupRequest from the SubscribeRequest
            SignupRequest signupRequest = new SignupRequest();
            signupRequest.setRatePlanId(request.getRatePlanId());
            signupRequest.setCardToken(request.getCardToken());

            // Convert startDate from String to LocalDate and set in signup request
            // The service will handle the start date
            subscriptionService.createSubscriptionWithStartDate(user, signupRequest, request.getStartDate());

            redirectAttributes.addFlashAttribute("success", "Subscription created successfully!");
            return "redirect:/member/subscriptions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create subscription: " + e.getMessage());
            return "redirect:/member/products";
        }
    }

    // Payment Methods
    @GetMapping("/payment-methods")
    public String paymentMethods(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);
        model.addAttribute("paymentMethods", userService.getUserPaymentMethods(user.getId()));
        return "member/payment-methods";
    }

    @PostMapping("/payment-methods")
    public String addPaymentMethod(Authentication authentication,
                                  @ModelAttribute PaymentMethodRequest request,
                                  RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            userService.addPaymentMethod(user, request.getCardToken(),
                    PaymentMethod.PaymentType.valueOf(request.getType()),
                    request.getExpirationMonth(),
                    request.getExpirationYear(),
                    request.getLastFour(),
                    request.getBrand(),
                    request.isMakeDefault());
            redirectAttributes.addFlashAttribute("success", "Payment method added");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add payment method: " + e.getMessage());
        }
        return "redirect:/member/payment-methods";
    }

    @PostMapping("/payment-methods/{id}/default")
    public String setDefaultPaymentMethod(Authentication authentication,
                                        @PathVariable Long id,
                                        RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            userService.setDefaultPaymentMethod(user.getId(), id);
            redirectAttributes.addFlashAttribute("success", "Default payment method updated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/member/payment-methods";
    }

    @PostMapping("/payment-methods/{id}/remove")
    public String removePaymentMethod(Authentication authentication,
                                    @PathVariable Long id,
                                    RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser(authentication);
            userService.removePaymentMethod(id, user.getId());
            redirectAttributes.addFlashAttribute("success", "Payment method removed");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/member/payment-methods";
    }

    // Billing History
    @GetMapping("/billing")
    public String billing(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);

        try {
            // Sync from Zuora
            invoiceService.syncInvoicesFromZuora(user);
            invoiceService.syncPaymentsFromZuora(user);
        } catch (Exception e) {
            // Log but don't fail - show cached data
        }

        model.addAttribute("invoices", invoiceService.getUserInvoices(user.getId()));
        model.addAttribute("payments", invoiceService.getUserPayments(user.getId()));
        return "member/billing";
    }

    // Invoice PDF Download
    @GetMapping("/invoices/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(Authentication authentication,
                                                      @PathVariable Long id) {
        User user = getCurrentUser(authentication);

        try {
            // Get invoice from service
            Invoice invoice = invoiceService.getInvoiceById(id);

            // Verify invoice belongs to this user
            if (!invoice.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // If invoice has Zuora invoice number, get PDF from Zuora
            // The Zuora API requires invoice number (e.g., INV00000073) not the Zuora ID
            if (invoice.getZuoraInvoiceNumber() != null && !invoice.getZuoraInvoiceNumber().isEmpty()) {
                byte[] pdfBytes = invoiceService.getInvoicePdf(invoice.getZuoraInvoiceNumber());
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice_" + invoice.getInvoiceNumber() + ".pdf\"")
                        .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                        .body(pdfBytes);
            } else {
                // Generate simple PDF for local invoices without Zuora ID
                String content = "Invoice #: " + invoice.getInvoiceNumber() + "\n" +
                        "Amount: $" + invoice.getAmount() + "\n" +
                        "Date: " + invoice.getInvoiceDate() + "\n" +
                        "Status: " + invoice.getStatus();
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice_" + invoice.getInvoiceNumber() + ".txt\"")
                        .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                        .body(content.getBytes());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== PAR-Q QUESTIONNAIRE ====================

    @GetMapping("/parq")
    public String parqForm(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);

        // Check if already completed
        if (healthQuestionnaireService.hasCompletedParq(user.getId())) {
            model.addAttribute("questionnaire", healthQuestionnaireService.getLatestQuestionnaire(user.getId()));
            return "member/parq-view";
        }

        model.addAttribute("parqRequest", new ParqRequest());
        return "member/parq-form";
    }

    @PostMapping("/parq")
    public String submitParq(Authentication authentication,
                            @Valid @ModelAttribute ParqRequest parqRequest,
                            BindingResult bindingResult,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(authentication);

        if (bindingResult.hasErrors()) {
            return "member/parq-form";
        }

        try {
            healthQuestionnaireService.submitParq(user, parqRequest, request);

            if (parqRequest.hasAnyYesAnswer()) {
                redirectAttributes.addFlashAttribute("warning",
                    "Based on your responses, we recommend consulting with a healthcare provider before starting an exercise program. " +
                    "You may still use the facility, but please exercise caution.");
            } else {
                redirectAttributes.addFlashAttribute("success", "Health questionnaire submitted successfully. You can now check in to the gym.");
            }

            return "redirect:/member/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to submit questionnaire: " + e.getMessage());
            return "member/parq-form";
        }
    }

    // ==================== LIABILITY WAIVER ====================

    @GetMapping("/waiver")
    public String waiverForm(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);

        // Check if already accepted
        if (waiverService.hasAcceptedWaiver(user.getId())) {
            model.addAttribute("waiver", waiverService.getLatestWaiver(user.getId()));
            return "member/waiver-view";
        }

        model.addAttribute("waiverRequest", new WaiverRequest());
        model.addAttribute("waiverVersion", waiverService.getCurrentWaiverVersion());
        return "member/waiver-form";
    }

    @PostMapping("/waiver")
    public String acceptWaiver(Authentication authentication,
                              @Valid @ModelAttribute WaiverRequest waiverRequest,
                              BindingResult bindingResult,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(authentication);

        if (bindingResult.hasErrors()) {
            return "member/waiver-form";
        }

        try {
            waiverService.acceptWaiver(user, waiverRequest, request);
            redirectAttributes.addFlashAttribute("success", "Liability waiver accepted successfully. You can now check in to the gym.");
            return "redirect:/member/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to accept waiver: " + e.getMessage());
            return "member/waiver-form";
        }
    }

    // ==================== CHECK-IN SYSTEM ====================

    @GetMapping("/check-in")
    public String checkInPage(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);
        model.addAttribute("user", user);
        model.addAttribute("qrCode", checkInService.generateQrCode(user));
        model.addAttribute("isCheckedIn", checkInService.isCheckedIn(user.getId()));
        model.addAttribute("lastCheckIn", user.getLastCheckInAt());
        model.addAttribute("visitCount", checkInService.getVisitCount(user.getId()));
        model.addAttribute("hasCompletedParq", healthQuestionnaireService.hasCompletedParq(user.getId()));
        model.addAttribute("hasAcceptedWaiver", waiverService.hasAcceptedWaiver(user.getId()));
        return "member/check-in";
    }

    @PostMapping("/check-in")
    public String performCheckIn(Authentication authentication,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(authentication);
        try {
            String ipAddress = request.getRemoteAddr();
            checkInService.checkIn(user, null, ipAddress);
            redirectAttributes.addFlashAttribute("success", "Checked in successfully! Enjoy your workout.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/member/check-in";
    }

    @PostMapping("/check-out")
    public String performCheckOut(Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(authentication);
        try {
            CheckIn checkIn = checkInService.checkOut(user);
            redirectAttributes.addFlashAttribute("success",
                "Checked out successfully! Session duration: " + checkIn.getDurationFormatted());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/member/check-in";
    }

    @GetMapping("/visit-history")
    public String visitHistory(Authentication authentication, Model model) {
        User user = getCurrentUser(authentication);
        model.addAttribute("checkIns", checkInService.getCheckInHistory(user.getId()));
        model.addAttribute("visitCount", checkInService.getVisitCount(user.getId()));
        return "member/visit-history";
    }
}
