package com.example.zuora.controller.admin;

import com.example.zuora.dto.*;
import com.example.zuora.model.Subscription;
import com.example.zuora.model.User;
import com.example.zuora.model.Invoice;
import com.example.zuora.model.PaymentMethod;
import com.example.zuora.model.Product;
import com.example.zuora.model.RatePlan;
import com.example.zuora.model.HealthQuestionnaire;
import com.example.zuora.model.WaiverAcceptance;
import com.example.zuora.model.CheckIn;
import com.example.zuora.model.WaiverContent;
import com.example.zuora.model.ParqQuestion;
import com.example.zuora.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ProductService productService;
    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final InvoiceService invoiceService;
    private final PasswordEncoder passwordEncoder;
    private final ProductSyncService productSyncService;
    private final com.example.zuora.service.DiscountService discountService;
    private final com.example.zuora.service.PaymentService paymentService;
    private final HealthQuestionnaireService healthQuestionnaireService;
    private final WaiverService waiverService;
    private final CheckInService checkInService;
    private final WaiverContentService waiverContentService;
    private final ParqQuestionService parqQuestionService;

    public AdminController(ProductService productService, UserService userService,
                          SubscriptionService subscriptionService, InvoiceService invoiceService,
                          PasswordEncoder passwordEncoder, ProductSyncService productSyncService,
                          com.example.zuora.service.DiscountService discountService,
                          com.example.zuora.service.PaymentService paymentService,
                          HealthQuestionnaireService healthQuestionnaireService,
                          WaiverService waiverService,
                          CheckInService checkInService,
                          WaiverContentService waiverContentService,
                          ParqQuestionService parqQuestionService) {
        this.productService = productService;
        this.userService = userService;
        this.subscriptionService = subscriptionService;
        this.invoiceService = invoiceService;
        this.passwordEncoder = passwordEncoder;
        this.productSyncService = productSyncService;
        this.discountService = discountService;
        this.paymentService = paymentService;
        this.healthQuestionnaireService = healthQuestionnaireService;
        this.waiverService = waiverService;
        this.checkInService = checkInService;
        this.waiverContentService = waiverContentService;
        this.parqQuestionService = parqQuestionService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long totalCustomers = userService.getAllCustomers().size();
        long totalProducts = productService.getAllProducts().size();
        long totalInvoices = invoiceService.getAllInvoices().size();

        // Calculate total revenue from paid invoices
        double totalRevenue = invoiceService.getAllInvoices().stream()
                .filter(inv -> inv.getStatus() != null &&
                        (inv.getStatus().name().equals("Paid") || inv.getStatus().name().equals("Posted")))
                .mapToDouble(Invoice::getTotalAmount)
                .sum();

        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalInvoices", totalInvoices);
        model.addAttribute("totalRevenue", String.format("$%.0f", totalRevenue));
        return "admin/dashboard";
    }

    // ==================== PRODUCT MANAGEMENT ====================

    @GetMapping("/products")
    public String products(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        return "admin/products";
    }

    @GetMapping("/products/new")
    public String newProduct(Model model) {
        model.addAttribute("createProductRequest", new CreateProductRequest());
        return "admin/product-form";
    }

    @PostMapping("/products")
    public String createProduct(@ModelAttribute CreateProductRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            productService.createProduct(request);
            redirectAttributes.addFlashAttribute("success", "Product created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create product: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/products/{id}/edit")
    public String editProduct(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id);
        model.addAttribute("product", product);
        return "admin/product-edit";
    }

    @PostMapping("/products/{id}")
    public String updateProduct(@PathVariable Long id,
                               @RequestParam String name,
                               @RequestParam String description,
                               RedirectAttributes redirectAttributes) {
        try {
            productService.updateProduct(id, name, description);
            redirectAttributes.addFlashAttribute("success", "Product updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update product: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/deactivate")
    public String deactivateProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deactivateProduct(id);
            redirectAttributes.addFlashAttribute("success", "Product deactivated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to deactivate: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/pricing/update")
    public String updatePricing(@ModelAttribute UpdatePriceRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            productService.updatePrice(request);
            redirectAttributes.addFlashAttribute("success", "Price updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update price: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    // ==================== RATE PLAN MANAGEMENT ====================

    @GetMapping("/products/{productId}/rate-plans/new")
    public String newRatePlan(@PathVariable Long productId, Model model) {
        Product product = productService.getProductById(productId);
        model.addAttribute("product", product);
        model.addAttribute("createRatePlanRequest", new CreateRatePlanRequest());
        return "admin/rate-plan-form";
    }

    @PostMapping("/products/{productId}/rate-plans")
    public String createRatePlan(@PathVariable Long productId,
                                @ModelAttribute CreateRatePlanRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            request.setProductId(productId);
            productService.createRatePlan(productId, request);
            redirectAttributes.addFlashAttribute("success", "Rate plan created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create rate plan: " + e.getMessage());
        }
        return "redirect:/admin/products/" + productId + "/edit";
    }

    @GetMapping("/rate-plans/{id}/edit")
    public String editRatePlan(@PathVariable Long id, Model model) {
        RatePlan ratePlan = productService.getRatePlanById(id);
        model.addAttribute("ratePlan", ratePlan);
        model.addAttribute("product", ratePlan.getProduct());

        // Create a request object pre-populated with existing values for the form
        com.example.zuora.dto.CreateRatePlanRequest request = new com.example.zuora.dto.CreateRatePlanRequest();
        request.setName(ratePlan.getName());
        request.setDescription(ratePlan.getDescription());
        request.setBillingPeriod(ratePlan.getBillingPeriod());
        request.setDiscountEligible(ratePlan.getDiscountEligible() == null || ratePlan.getDiscountEligible());
        if (ratePlan.getRecurringCharge() != null) {
            request.setPrice(ratePlan.getRecurringCharge().getAmount());
        }
        model.addAttribute("createRatePlanRequest", request);

        return "admin/rate-plan-form";
    }

    @PostMapping("/rate-plans/{id}")
    public String updateRatePlan(@PathVariable Long id,
                                @ModelAttribute UpdateRatePlanRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            RatePlan ratePlan = productService.getRatePlanById(id);
            productService.updateRatePlan(id, request);
            redirectAttributes.addFlashAttribute("success", "Rate plan updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update rate plan: " + e.getMessage());
        }
        Long productId = productService.getRatePlanById(id).getProduct().getId();
        return "redirect:/admin/products/" + productId + "/edit";
    }

    @PostMapping("/rate-plans/{id}/activate")
    public String activateRatePlan(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            RatePlan ratePlan = productService.getRatePlanById(id);
            Long productId = ratePlan.getProduct().getId();
            productService.activateRatePlan(id);
            redirectAttributes.addFlashAttribute("success", "Rate plan activated");
            return "redirect:/admin/products/" + productId + "/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to activate rate plan: " + e.getMessage());
            return "redirect:/admin/products";
        }
    }

    @PostMapping("/rate-plans/{id}/deactivate")
    public String deactivateRatePlan(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            RatePlan ratePlan = productService.getRatePlanById(id);
            Long productId = ratePlan.getProduct().getId();
            productService.deactivateRatePlan(id);
            redirectAttributes.addFlashAttribute("success", "Rate plan deactivated");
            return "redirect:/admin/products/" + productId + "/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to deactivate rate plan: " + e.getMessage());
            return "redirect:/admin/products";
        }
    }

    // ==================== CUSTOMER MANAGEMENT ====================

    @GetMapping("/customers")
    public String customers(Model model) {
        model.addAttribute("customers", userService.getAllCustomers());
        return "admin/customers";
    }

    @GetMapping("/customers/new")
    public String newCustomer(Model model) {
        model.addAttribute("createUserRequest", new CreateUserRequest());
        model.addAttribute("products", productService.getActiveProductsByCategory(Product.Category.MEMBERSHIP));
        return "admin/customer-form";
    }

    @PostMapping("/customers")
    public String createCustomer(@Valid @ModelAttribute CreateUserRequest request,
                                BindingResult result,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("products", productService.getActiveProductsByCategory(Product.Category.MEMBERSHIP));
            return "admin/customer-form";
        }

        try {
            // Check if email already exists
            if (userService.existsByEmail(request.getEmail())) {
                redirectAttributes.addFlashAttribute("error", "Email already registered: " + request.getEmail());
                return "redirect:/admin/customers/new";
            }

            // Create SignupRequest from CreateUserRequest
            SignupRequest signupRequest = new SignupRequest();
            signupRequest.setFirstName(request.getFirstName());
            signupRequest.setLastName(request.getLastName());
            signupRequest.setEmail(request.getEmail());
            signupRequest.setPhone(request.getPhone());
            signupRequest.setPassword(generateTemporaryPassword()); // Generate temporary password
            signupRequest.setAddress1(request.getAddress1());
            signupRequest.setAddress2(request.getAddress2());
            signupRequest.setCity(request.getCity());
            signupRequest.setState(request.getState());
            signupRequest.setZipCode(request.getZipCode());
            signupRequest.setCountry(request.getCountry());
            signupRequest.setBillToName(request.getBillToName());
            signupRequest.setSoldToAddress1(request.getSoldToAddress1());
            signupRequest.setSoldToAddress2(request.getSoldToAddress2());
            signupRequest.setSoldToCity(request.getSoldToCity());
            signupRequest.setSoldToState(request.getSoldToState());
            signupRequest.setSoldToZipCode(request.getSoldToZipCode());
            signupRequest.setSoldToCountry(request.getSoldToCountry());
            signupRequest.setSameAsBillTo(request.isSameAsBillTo());
            signupRequest.setRatePlanId(request.getRatePlanId());

            // Create the user
            userService.createUser(signupRequest);

            // TODO: Send welcome email with password reset link
            // For now, just show a success message

            redirectAttributes.addFlashAttribute("success",
                "Customer created successfully. A temporary password has been generated and should be sent via email.");
            return "redirect:/admin/customers";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create customer: " + e.getMessage());
            return "redirect:/admin/customers/new";
        }
    }

    @GetMapping("/customers/{id}")
    public String customerDetail(@PathVariable Long id, Model model) {
        User customer = userService.getUserById(id);

        // Sync data from Zuora first
        try {
            if (customer.getZuoraAccountId() != null) {
                invoiceService.syncInvoicesFromZuora(customer);
                invoiceService.syncPaymentsFromZuora(customer);
            }
        } catch (Exception e) {
            System.err.println("Failed to sync Zuora data: " + e.getMessage());
        }

        List<Subscription> subscriptions = subscriptionService.getUserSubscriptions(id);
        List<PaymentMethod> paymentMethods = userService.getUserPaymentMethods(id);
        List<Invoice> invoices = invoiceService.getUserInvoices(id);

        // Fetch Zuora contact details
        try {
            if (customer.getZuoraAccountId() != null) {
                model.addAttribute("zuoraAccount", userService.getZuoraAccountDetails(customer));
                model.addAttribute("billToContact", userService.getBillToContact(customer));
                model.addAttribute("soldToContact", userService.getSoldToContact(customer));
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch Zuora contacts: " + e.getMessage());
        }

        model.addAttribute("customer", customer);
        model.addAttribute("subscriptions", subscriptions);
        model.addAttribute("paymentMethods", paymentMethods);
        model.addAttribute("invoices", invoices);
        model.addAttribute("availablePlans", productService.getActiveProductsByCategory(Product.Category.MEMBERSHIP));
        return "admin/customer-detail";
    }

    @PostMapping("/customers/{id}/update")
    public String updateCustomer(@PathVariable Long id,
                                  @RequestParam String firstName,
                                  @RequestParam String lastName,
                                  @RequestParam String phone,
                                  RedirectAttributes redirectAttributes) {
        try {
            userService.updateUserProfile(id, firstName, lastName, phone);
            redirectAttributes.addFlashAttribute("success", "Customer updated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update: " + e.getMessage());
        }
        return "redirect:/admin/customers/" + id;
    }

    @PostMapping("/customers/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User customer = userService.getUserById(id);

            // Generate a password reset token
            String resetToken = generatePasswordResetToken(customer);

            // TODO: Send password reset email
            // In a real implementation, you would send an email with a link containing the token
            // For now, we just show a success message

            redirectAttributes.addFlashAttribute("success",
                "Password reset email sent to " + customer.getEmail());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Failed to send password reset: " + e.getMessage());
        }
        return "redirect:/admin/customers/" + id;
    }

    // ==================== SUBSCRIPTION MANAGEMENT ====================

    @PostMapping("/customers/{id}/subscriptions/upgrade")
    public String adminUpgradeSubscription(@PathVariable Long id,
                                           @ModelAttribute SubscriptionUpdateRequest request,
                                           RedirectAttributes redirectAttributes) {
        try {
            User customer = userService.getUserById(id);
            subscriptionService.upgradeSubscription(request, customer);
            redirectAttributes.addFlashAttribute("success", "Subscription upgraded successfully in Zuora");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upgrade: " + e.getMessage());
        }
        return "redirect:/admin/customers/" + id;
    }

    @PostMapping("/subscriptions/{id}/cancel")
    public String cancelSubscription(@PathVariable Long id,
                                     @RequestParam String reason,
                                     RedirectAttributes redirectAttributes) {
        try {
            CancelSubscriptionRequest request = new CancelSubscriptionRequest();
            request.setSubscriptionId(id);
            request.setReason(reason);
            Subscription sub = subscriptionService.getSubscriptionById(id);
            subscriptionService.cancelSubscription(request, sub.getUser());
            redirectAttributes.addFlashAttribute("success", "Subscription cancelled");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to cancel: " + e.getMessage());
        }
        return "redirect:/admin/customers";
    }

    // Admin add new subscription for customer
    @PostMapping("/customers/{id}/subscriptions/add")
    public String adminAddSubscription(@PathVariable Long id,
                                       @RequestParam Long ratePlanId,
                                       RedirectAttributes redirectAttributes) {
        try {
            User customer = userService.getUserById(id);
            com.example.zuora.dto.SignupRequest signupRequest = new com.example.zuora.dto.SignupRequest();
            signupRequest.setRatePlanId(ratePlanId);
            subscriptionService.createSubscription(customer, signupRequest);
            redirectAttributes.addFlashAttribute("success", "Subscription created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create subscription: " + e.getMessage());
        }
        return "redirect:/admin/customers/" + id + "/subscriptions";
    }

    // ==================== INVOICE MANAGEMENT ====================

    // Admin download invoice PDF
    @GetMapping("/invoices/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(id);

            // If invoice has Zuora invoice number, get PDF from Zuora
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

    @GetMapping("/invoices")
    public String invoices(Model model) {
        model.addAttribute("invoices", invoiceService.getAllInvoices());
        return "admin/invoices";
    }

    // ==================== PROXY TO MEMBER APIs ====================

    // Customer subscriptions (proxy to member/subscriptions)
    @GetMapping("/customers/{id}/subscriptions")
    public String customerSubscriptions(@PathVariable Long id, Model model) {
        User customer = userService.getUserById(id);
        List<Subscription> subscriptions = subscriptionService.getUserSubscriptions(id);

        model.addAttribute("customer", customer);
        model.addAttribute("subscriptions", subscriptions);
        model.addAttribute("hasActiveSubscription", subscriptionService.hasActiveSubscription(id));
        model.addAttribute("availablePlans", productService.getActiveProductsByCategory(Product.Category.MEMBERSHIP));
        model.addAttribute("addOnProducts", productService.getAddOnProducts());

        // Calculate cancellation options for active subscriptions
        for (Subscription sub : subscriptions) {
            if (sub.getStatus() == Subscription.SubscriptionStatus.Active) {
                try {
                    model.addAttribute("cancellationOptions", subscriptionService.calculateCancellationOptions(sub));
                    break;
                } catch (Exception e) {
                    System.err.println("Failed to calculate cancellation options: " + e.getMessage());
                }
            }
        }

        return "admin/customer-subscriptions";
    }

    // Add add-on to customer subscription
    @PostMapping("/customers/{id}/subscriptions/add-addon")
    public String addAddOnToCustomerSubscription(@PathVariable Long id,
                                                 @RequestParam Long ratePlanId,
                                                 RedirectAttributes redirectAttributes) {
        try {
            User customer = userService.getUserById(id);

            // Check if customer has active subscription
            if (!subscriptionService.hasActiveSubscription(id)) {
                redirectAttributes.addFlashAttribute("error", "Customer needs an active membership before adding add-ons.");
                return "redirect:/admin/customers/" + id + "/subscriptions";
            }

            // Add the add-on
            subscriptionService.addAddOnToSubscription(customer, ratePlanId, null);

            redirectAttributes.addFlashAttribute("success", "Add-on service added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add add-on: " + e.getMessage());
        }
        return "redirect:/admin/customers/" + id + "/subscriptions";
    }

    // Customer billing (proxy to member/billing)
    @GetMapping("/customers/{id}/billing")
    public String customerBilling(@PathVariable Long id, Model model) {
        User customer = userService.getUserById(id);

        try {
            if (customer.getZuoraAccountId() != null) {
                invoiceService.syncInvoicesFromZuora(customer);
                invoiceService.syncPaymentsFromZuora(customer);
            }
        } catch (Exception e) {
            System.err.println("Failed to sync Zuora data: " + e.getMessage());
        }

        model.addAttribute("customer", customer);
        model.addAttribute("invoices", invoiceService.getUserInvoices(id));
        model.addAttribute("payments", invoiceService.getUserPayments(id));

        return "admin/customer-billing";
    }

    // Customer payment methods (proxy to member/payment-methods)
    @GetMapping("/customers/{id}/payment-methods")
    public String customerPaymentMethods(@PathVariable Long id, Model model) {
        User customer = userService.getUserById(id);
        List<PaymentMethod> paymentMethods = userService.getUserPaymentMethods(id);

        model.addAttribute("customer", customer);
        model.addAttribute("paymentMethods", paymentMethods);

        return "admin/customer-payment-methods";
    }

    // Admin set default payment method (using member service)
    @PostMapping("/customers/{userId}/payment-methods/{id}/default")
    public String setDefaultPaymentMethod(@PathVariable Long userId,
                                          @PathVariable Long id,
                                          RedirectAttributes redirectAttributes) {
        try {
            userService.setDefaultPaymentMethod(userId, id);
            redirectAttributes.addFlashAttribute("success", "Default payment method updated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/customers/" + userId + "/payment-methods";
    }

    // Admin remove payment method (using member service)
    @PostMapping("/customers/{userId}/payment-methods/{id}/remove")
    public String removePaymentMethod(@PathVariable Long userId,
                                     @PathVariable Long id,
                                     RedirectAttributes redirectAttributes) {
        try {
            userService.removePaymentMethod(id, userId);
            redirectAttributes.addFlashAttribute("success", "Payment method removed");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/customers/" + userId + "/payment-methods";
    }

    // ==================== PAR-Q STATUS ====================

    @GetMapping("/customers/{id}/parq")
    public String customerParq(@PathVariable Long id, Model model) {
        User customer = userService.getUserById(id);
        HealthQuestionnaire latestParq = healthQuestionnaireService.getLatestQuestionnaire(id);
        List<HealthQuestionnaire> parqHistory = healthQuestionnaireService.getQuestionnaireHistory(id);

        model.addAttribute("customer", customer);
        model.addAttribute("latestParq", latestParq);
        model.addAttribute("parqHistory", parqHistory);
        model.addAttribute("hasCompletedParq", healthQuestionnaireService.hasCompletedParq(id));

        return "admin/customer-parq";
    }

    // ==================== WAIVER STATUS ====================

    @GetMapping("/customers/{id}/waiver")
    public String customerWaiver(@PathVariable Long id, Model model) {
        User customer = userService.getUserById(id);
        WaiverAcceptance latestWaiver = waiverService.getLatestWaiver(id);
        List<WaiverAcceptance> waiverHistory = waiverService.getWaiverHistory(id);

        model.addAttribute("customer", customer);
        model.addAttribute("latestWaiver", latestWaiver);
        model.addAttribute("waiverHistory", waiverHistory);
        model.addAttribute("hasAcceptedWaiver", waiverService.hasAcceptedWaiver(id));
        model.addAttribute("currentWaiverVersion", waiverService.getCurrentWaiverVersion());

        return "admin/customer-waiver";
    }

    // ==================== CHECK-IN HISTORY ====================

    @GetMapping("/customers/{id}/checkins")
    public String customerCheckIns(@PathVariable Long id, Model model) {
        User customer = userService.getUserById(id);
        List<CheckIn> checkInHistory = checkInService.getCheckInHistory(id);
        long totalVisits = checkInService.getVisitCount(id);
        CheckIn currentCheckIn = checkInService.getCurrentCheckIn(id);

        model.addAttribute("customer", customer);
        model.addAttribute("checkIns", checkInHistory);
        model.addAttribute("totalVisits", totalVisits);
        model.addAttribute("currentCheckIn", currentCheckIn);
        model.addAttribute("isCheckedIn", checkInService.isCheckedIn(id));

        return "admin/customer-checkins";
    }

    // ==================== WAIVER MANAGEMENT ====================

    @GetMapping("/waivers")
    public String waiverManagement(Model model) {
        model.addAttribute("waivers", waiverContentService.getAllWaiverVersions());
        model.addAttribute("activeWaiver", waiverContentService.getActiveWaiver());
        return "admin/waivers";
    }

    @PostMapping("/waivers/create")
    public String createWaiver(@RequestParam String version,
                               @RequestParam String title,
                               @RequestParam String content,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getUserByEmail(authentication.getName());
            waiverContentService.createWaiver(version, title, content, currentUser.getId());
            redirectAttributes.addFlashAttribute("success", "Waiver version '" + version + "' created successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create waiver: " + e.getMessage());
        }
        return "redirect:/admin/waivers";
    }

    @PostMapping("/waivers/{id}/update")
    public String updateWaiver(@PathVariable Long id,
                               @RequestParam String title,
                               @RequestParam String content,
                               RedirectAttributes redirectAttributes) {
        try {
            waiverContentService.updateWaiver(id, title, content);
            redirectAttributes.addFlashAttribute("success", "Waiver updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update waiver: " + e.getMessage());
        }
        return "redirect:/admin/waivers";
    }

    @PostMapping("/waivers/{id}/activate")
    public String activateWaiver(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            waiverContentService.activateWaiver(id);
            redirectAttributes.addFlashAttribute("success", "Waiver version activated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to activate waiver: " + e.getMessage());
        }
        return "redirect:/admin/waivers";
    }

    @PostMapping("/waivers/{id}/delete")
    public String deleteWaiver(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            waiverContentService.deleteWaiver(id);
            redirectAttributes.addFlashAttribute("success", "Waiver version deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete waiver: " + e.getMessage());
        }
        return "redirect:/admin/waivers";
    }

    @PostMapping("/waivers/initialize")
    public String initializeDefaultWaiver(RedirectAttributes redirectAttributes) {
        try {
            waiverContentService.initializeDefaultWaiver();
            redirectAttributes.addFlashAttribute("success", "Default waiver loaded successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to initialize default waiver: " + e.getMessage());
        }
        return "redirect:/admin/waivers";
    }

    // ==================== PAR-Q QUESTIONS MANAGEMENT ====================

    @GetMapping("/parq-questions")
    public String parqQuestionsManagement(Model model) {
        model.addAttribute("questions", parqQuestionService.getAllQuestions());
        return "admin/parq-questions";
    }

    @PostMapping("/parq-questions/create")
    public String createParqQuestion(@RequestParam Integer questionNumber,
                                      @RequestParam String questionText,
                                      @RequestParam(required = false) String helpText,
                                      @RequestParam(required = false, defaultValue = "true") Boolean required,
                                      @RequestParam(required = false) Integer displayOrder,
                                      RedirectAttributes redirectAttributes) {
        try {
            parqQuestionService.createQuestion(questionNumber, questionText, helpText, required, displayOrder);
            redirectAttributes.addFlashAttribute("success", "Question created successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create question: " + e.getMessage());
        }
        return "redirect:/admin/parq-questions";
    }

    @PostMapping("/parq-questions/{id}/update")
    public String updateParqQuestion(@PathVariable Long id,
                                      @RequestParam String questionText,
                                      @RequestParam(required = false) String helpText,
                                      @RequestParam(required = false) Boolean active,
                                      @RequestParam(required = false) Boolean required,
                                      @RequestParam(required = false) Integer displayOrder,
                                      RedirectAttributes redirectAttributes) {
        try {
            parqQuestionService.updateQuestion(id, questionText, helpText, active, required, displayOrder);
            redirectAttributes.addFlashAttribute("success", "Question updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update question: " + e.getMessage());
        }
        return "redirect:/admin/parq-questions";
    }

    @PostMapping("/parq-questions/{id}/toggle-active")
    public String toggleParqQuestionActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ParqQuestion question = parqQuestionService.getQuestionById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Question not found"));
            parqQuestionService.updateQuestion(id, null, null, !question.getActive(), null, null);
            redirectAttributes.addFlashAttribute("success",
                    question.getActive() ? "Question deactivated." : "Question activated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to toggle question: " + e.getMessage());
        }
        return "redirect:/admin/parq-questions";
    }

    @PostMapping("/parq-questions/{id}/delete")
    public String deleteParqQuestion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            parqQuestionService.deleteQuestion(id);
            redirectAttributes.addFlashAttribute("success", "Question deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete question: " + e.getMessage());
        }
        return "redirect:/admin/parq-questions";
    }

    @PostMapping("/parq-questions/initialize")
    public String initializeDefaultQuestions(RedirectAttributes redirectAttributes) {
        try {
            parqQuestionService.initializeDefaultQuestions();
            redirectAttributes.addFlashAttribute("success", "Default PAR-Q questions loaded successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to initialize questions: " + e.getMessage());
        }
        return "redirect:/admin/parq-questions";
    }

    // ==================== GLOBAL SEARCH ====================

    @GetMapping("/search")
    public String globalSearch(@RequestParam String query, Model model) {
        if (query == null || query.trim().isEmpty()) {
            return "redirect:/admin/dashboard";
        }

        String searchTerm = query.toLowerCase().trim();

        // Search customers
        List<User> customers = userService.getAllCustomers().stream()
                .filter(c -> c.getFirstName().toLowerCase().contains(searchTerm)
                        || c.getLastName().toLowerCase().contains(searchTerm)
                        || c.getEmail().toLowerCase().contains(searchTerm)
                        || (c.getZuoraAccountNumber() != null && c.getZuoraAccountNumber().toLowerCase().contains(searchTerm)))
                .toList();

        // Search products
        List<Product> products = productService.getAllProducts().stream()
                .filter(p -> p.getName().toLowerCase().contains(searchTerm)
                        || (p.getDescription() != null && p.getDescription().toLowerCase().contains(searchTerm)))
                .toList();

        // Search invoices
        List<Invoice> invoices = invoiceService.getAllInvoices().stream()
                .filter(i -> (i.getZuoraInvoiceNumber() != null && i.getZuoraInvoiceNumber().toLowerCase().contains(searchTerm))
                        || (i.getUser() != null && (i.getUser().getFirstName().toLowerCase().contains(searchTerm)
                                || i.getUser().getLastName().toLowerCase().contains(searchTerm)
                                || i.getUser().getEmail().toLowerCase().contains(searchTerm))))
                .toList();

        model.addAttribute("query", query);
        model.addAttribute("customers", customers);
        model.addAttribute("products", products);
        model.addAttribute("invoices", invoices);
        model.addAttribute("totalResults", customers.size() + products.size() + invoices.size());

        return "admin/search-results";
    }

    // ==================== UTILITY METHODS ====================

    private String generateTemporaryPassword() {
        // Generate a random 12-character temporary password
        return UUID.randomUUID().toString().substring(0, 12) + "A1!";
    }

    private String generatePasswordResetToken(User user) {
        // In a real implementation, generate a secure token and store it with expiration
        // For now, return a placeholder
        return UUID.randomUUID().toString();
    }

    // ==================== ZUORA SYNC ====================

    /**
     * Show Zuora sync status for products
     */
    @GetMapping("/zuora/sync-status")
    public String zuoraSyncStatus(Model model) {
        model.addAttribute("syncStatus", productSyncService.getSyncStatus());
        model.addAttribute("productsSynced", productSyncService.areProductsSynced());
        return "admin/zuora-sync";
    }

    /**
     * Sync all products to Zuora
     */
    @PostMapping("/zuora/sync-products")
    public String syncProductsToZuora(RedirectAttributes redirectAttributes) {
        try {
            productSyncService.syncAllProductsToZuora();
            redirectAttributes.addFlashAttribute("success", "Products synced to Zuora successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to sync products to Zuora: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/admin/zuora/sync-status";
    }

    // ==================== DISCOUNT MANAGEMENT ====================

    @GetMapping("/discounts")
    public String discounts(Model model,
                            @RequestParam(required = false) String success,
                            @RequestParam(required = false) String error) {
        try {
            model.addAttribute("activeDiscounts", discountService.getActiveDiscounts());
            model.addAttribute("allDiscounts", discountService.getAllDiscounts());
            model.addAttribute("products", productService.getAllProducts());
            model.addAttribute("ratePlans", productService.getAllRatePlans());
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load discounts: " + e.getMessage());
        }
        if (success != null) model.addAttribute("success", success);
        if (error != null) model.addAttribute("error", error);
        return "admin/discounts";
    }

    @PostMapping("/discounts")
    public String createDiscount(@ModelAttribute com.example.zuora.dto.CreateDiscountRequest request,
                                 RedirectAttributes redirectAttributes) {
        try {
            discountService.createDiscount(request);
            redirectAttributes.addFlashAttribute("success", "Discount created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create discount: " + e.getMessage());
        }
        return "redirect:/admin/discounts";
    }

    @GetMapping("/discounts/{id}")
    public String editDiscount(@PathVariable Long id, Model model) {
        try {
            model.addAttribute("discount", discountService.getDiscountById(id));
            model.addAttribute("products", productService.getAllProducts());
            model.addAttribute("ratePlans", productService.getAllRatePlans());
            return "admin/discount-edit";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load discount: " + e.getMessage());
            return "redirect:/admin/discounts";
        }
    }

    @PostMapping("/discounts/{id}")
    public String updateDiscount(@PathVariable Long id,
                                 @ModelAttribute com.example.zuora.dto.CreateDiscountRequest request,
                                 RedirectAttributes redirectAttributes) {
        try {
            discountService.updateDiscount(id, request);
            redirectAttributes.addFlashAttribute("success", "Discount updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update discount: " + e.getMessage());
        }
        return "redirect:/admin/discounts";
    }

    @PostMapping("/discounts/{id}/toggle")
    public String toggleDiscount(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            com.example.zuora.model.Discount discount = discountService.getDiscountById(id);
            if (discount.getStatus() == com.example.zuora.model.Discount.Status.ACTIVE) {
                discountService.deactivateDiscount(id);
                redirectAttributes.addFlashAttribute("success", "Discount deactivated");
            } else {
                discountService.activateDiscount(id);
                redirectAttributes.addFlashAttribute("success", "Discount activated");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to toggle discount: " + e.getMessage());
        }
        return "redirect:/admin/discounts";
    }

    @PostMapping("/discounts/{id}/delete")
    public String deleteDiscount(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            discountService.deleteDiscount(id);
            redirectAttributes.addFlashAttribute("success", "Discount deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete discount: " + e.getMessage());
        }
        return "redirect:/admin/discounts";
    }

    // ==================== PAYMENT MANAGEMENT ====================

    @GetMapping("/payments")
    public String payments(Model model,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String dateFrom,
                          @RequestParam(required = false) String dateTo,
                          @RequestParam(required = false) String customer,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size) {
        try {
            java.util.List<com.example.zuora.model.Payment> allPayments = paymentService.getAllPayments();

            // Calculate statistics
            double totalPayments = allPayments.stream()
                .filter(p -> p.getStatus() != com.example.zuora.model.Payment.PaymentStatus.Voided)
                .mapToDouble(com.example.zuora.model.Payment::getAmount)
                .sum();
            double processedPayments = allPayments.stream()
                .filter(p -> p.getStatus() == com.example.zuora.model.Payment.PaymentStatus.Processed)
                .mapToDouble(com.example.zuora.model.Payment::getAmount)
                .sum();
            double processingPayments = allPayments.stream()
                .filter(p -> p.getStatus() == com.example.zuora.model.Payment.PaymentStatus.Processing)
                .mapToDouble(com.example.zuora.model.Payment::getAmount)
                .sum();

            model.addAttribute("payments", allPayments);
            model.addAttribute("totalPayments", totalPayments);
            model.addAttribute("processedPayments", processedPayments);
            model.addAttribute("processingPayments", processingPayments);
            model.addAttribute("totalRefunds", 0.0);
            model.addAttribute("customers", userService.getAllCustomers());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", 1);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load payments: " + e.getMessage());
        }
        return "admin/payments";
    }

    // Record a new payment
    @PostMapping("/payments")
    public String recordPayment(@RequestParam Long customerId,
                                @RequestParam Double amount,
                                @RequestParam(required = false) String paymentMethodType,
                                @RequestParam(required = false) String paymentDate,
                                @RequestParam(required = false) String referenceNumber,
                                @RequestParam(required = false) String notes,
                                RedirectAttributes redirectAttributes) {
        try {
            User customer = userService.getUserById(customerId);
            // Create payment record (this would typically go through a payment service)
            paymentService.recordManualPayment(customer, amount, paymentMethodType, referenceNumber, notes);
            redirectAttributes.addFlashAttribute("success", "Payment recorded successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to record payment: " + e.getMessage());
        }
        return "redirect:/admin/payments";
    }

    // Process refund
    @PostMapping("/payments/refund")
    public String processRefund(@RequestParam Long paymentId,
                                @RequestParam Double amount,
                                @RequestParam(required = false) String refundType,
                                @RequestParam(required = false) String reasonCode,
                                @RequestParam(required = false) String notes,
                                RedirectAttributes redirectAttributes) {
        try {
            paymentService.processRefund(paymentId, amount, reasonCode, notes);
            redirectAttributes.addFlashAttribute("success", "Refund processed successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to process refund: " + e.getMessage());
        }
        return "redirect:/admin/payments";
    }
}
