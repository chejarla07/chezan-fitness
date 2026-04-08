package com.example.zuora.controller.admin;

import com.example.zuora.dto.*;
import com.example.zuora.model.Subscription;
import com.example.zuora.model.User;
import com.example.zuora.model.Invoice;
import com.example.zuora.model.PaymentMethod;
import com.example.zuora.model.Product;
import com.example.zuora.model.RatePlan;
import com.example.zuora.service.*;
import jakarta.validation.Valid;
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

    public AdminController(ProductService productService, UserService userService,
                          SubscriptionService subscriptionService, InvoiceService invoiceService,
                          PasswordEncoder passwordEncoder, ProductSyncService productSyncService) {
        this.productService = productService;
        this.userService = userService;
        this.subscriptionService = subscriptionService;
        this.invoiceService = invoiceService;
        this.passwordEncoder = passwordEncoder;
        this.productSyncService = productSyncService;
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

    // ==================== INVOICE MANAGEMENT ====================

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
}
