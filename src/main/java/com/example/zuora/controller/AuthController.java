package com.example.zuora.controller;

import com.example.zuora.dto.LoginRequest;
import com.example.zuora.dto.PasswordResetConfirmRequest;
import com.example.zuora.dto.PasswordResetRequest;
import com.example.zuora.dto.SignupRequest;
import com.example.zuora.model.User;
import com.example.zuora.security.CustomUserDetailsService;
import com.example.zuora.security.JwtUtil;
import com.example.zuora.service.EmailVerificationService;
import com.example.zuora.service.PasswordResetService;
import com.example.zuora.service.ProductService;
import com.example.zuora.service.SignupResult;
import com.example.zuora.service.SubscriptionService;
import com.example.zuora.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private final ProductService productService;
    private final SubscriptionService subscriptionService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService, ProductService productService,
                         SubscriptionService subscriptionService, JwtUtil jwtUtil,
                         AuthenticationManager authenticationManager,
                         EmailVerificationService emailVerificationService,
                         PasswordResetService passwordResetService) {
        this.userService = userService;
        this.productService = productService;
        this.subscriptionService = subscriptionService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.emailVerificationService = emailVerificationService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("memberships", productService.getActiveProductsByCategory(com.example.zuora.model.Product.Category.MEMBERSHIP));
        return "index";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid email or password");
        }
        return "login";
    }

    @PostMapping("/api/auth/login")
    public String login(@Valid @ModelAttribute LoginRequest loginRequest,
                       BindingResult bindingResult,
                       HttpServletResponse response,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please enter valid credentials");
            return "redirect:/login";
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            User user = userService.getUserByEmail(loginRequest.getEmail());
            String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

            // Set JWT cookie
            Cookie cookie = new Cookie("jwt_token", token);
            cookie.setHttpOnly(true);
            cookie.setMaxAge(86400); // 24 hours
            cookie.setPath("/");
            response.addCookie(cookie);

            // Redirect based on role
            if (user.getRole() == User.Role.ADMIN) {
                return "redirect:/admin/dashboard";
            }
            return "redirect:/member/dashboard";

        } catch (BadCredentialsException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid email or password");
            return "redirect:/login";
        }
    }

    @GetMapping("/signup")
    public String signupPage(Model model) {
        model.addAttribute("memberships", productService.getActiveProductsByCategory(com.example.zuora.model.Product.Category.MEMBERSHIP));
        model.addAttribute("signupRequest", new SignupRequest());
        return "signup";
    }

    @PostMapping("/api/auth/signup")
    public String signup(@Valid @ModelAttribute SignupRequest signupRequest,
                        BindingResult bindingResult,
                        HttpServletResponse response,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("memberships", productService.getActiveProductsByCategory(com.example.zuora.model.Product.Category.MEMBERSHIP));
            model.addAttribute("errors", bindingResult.getAllErrors());
            return "signup";
        }

        try {
            // STEP 1: Create user with Zuora account (atomic operation)
            SignupResult result = userService.createUser(signupRequest);
            User user = result.getUser();

            // STEP 2: Create subscription IMMEDIATELY after account creation (payment is optional)
            boolean subscriptionCreated = false;
            if (signupRequest.getRatePlanId() != null) {
                try {
                    subscriptionService.createSubscription(user, signupRequest);
                    subscriptionCreated = true;
                    System.out.println("Subscription created successfully for user: " + user.getEmail());
                } catch (Exception e) {
                    // Log but don't fail - user can subscribe later from profile
                    System.err.println("Subscription creation failed for user " + user.getEmail() + ": " + e.getMessage());
                }
            }

            // STEP 3: Generate JWT token and set cookie
            String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
            Cookie cookie = new Cookie("jwt_token", token);
            cookie.setHttpOnly(true);
            cookie.setMaxAge(86400);
            cookie.setPath("/");
            response.addCookie(cookie);

            // STEP 4: Show appropriate success message
            StringBuilder successMessage = new StringBuilder("Your account has been successfully created.");
            successMessage.append(" Your account number is: ").append(result.getZuoraAccountNumber());

            if (signupRequest.getRatePlanId() != null) {
                if (subscriptionCreated) {
                    successMessage.append(" Your subscription is now active.");
                } else {
                    successMessage.append(" You can subscribe to a plan from your dashboard.");
                }
            }

            redirectAttributes.addFlashAttribute("success", successMessage.toString());

            return "redirect:/member/dashboard";

        } catch (Exception e) {
            // Log full error for debugging
            System.err.println("Signup failed: " + e.getMessage());
            e.printStackTrace();

            // Show user-friendly error (don't expose internal details)
            String userMessage = e.getMessage();
            if (userMessage == null || userMessage.contains("Unable to create billing account")
                    || userMessage.contains("We were unable to create your account")) {
                userMessage = "We were unable to create your account. Please try again or contact support.";
            }

            model.addAttribute("memberships", productService.getActiveProductsByCategory(com.example.zuora.model.Product.Category.MEMBERSHIP));
            model.addAttribute("error", userMessage);
            return "signup";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt_token", null);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:/";
    }

    // ==================== EMAIL VERIFICATION ====================

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token, RedirectAttributes redirectAttributes) {
        boolean verified = emailVerificationService.verifyEmail(token);
        if (verified) {
            redirectAttributes.addFlashAttribute("success", "Email verified successfully! You can now log in.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired verification link. Please request a new one.");
        }
        return "redirect:/login";
    }

    @PostMapping("/api/auth/resend-verification")
    public String resendVerification(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserByEmail(email);
            if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                String token = emailVerificationService.resendVerification(user);
                // TODO: Send email with verification link containing token
                // For now, just log it
                System.out.println("Verification token for " + email + ": " + token);
            }
            redirectAttributes.addFlashAttribute("success", "If an account exists with this email, a verification link has been sent.");
        } catch (Exception e) {
            // Don't reveal if email exists
            redirectAttributes.addFlashAttribute("success", "If an account exists with this email, a verification link has been sent.");
        }
        return "redirect:/login";
    }

    // ==================== PASSWORD RESET ====================

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/api/auth/forgot-password")
    public String forgotPassword(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            String token = passwordResetService.initiatePasswordReset(email);
            if (token != null) {
                // TODO: Send email with reset link
                // For now, just log it for development
                System.out.println("Password reset token for " + email + ": " + token);
            }
            redirectAttributes.addFlashAttribute("success", "If an account exists with this email, a password reset link has been sent.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("success", "If an account exists with this email, a password reset link has been sent.");
        }
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        if (!passwordResetService.validateToken(token)) {
            model.addAttribute("error", "Invalid or expired reset link. Please request a new one.");
            return "reset-password";
        }
        model.addAttribute("token", token);
        model.addAttribute("resetRequest", new PasswordResetConfirmRequest());
        return "reset-password";
    }

    @PostMapping("/api/auth/reset-password")
    public String resetPassword(@Valid @ModelAttribute PasswordResetConfirmRequest request,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("token", request.getToken());
            model.addAttribute("errors", bindingResult.getAllErrors());
            return "reset-password";
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            model.addAttribute("token", request.getToken());
            model.addAttribute("error", "Passwords do not match");
            return "reset-password";
        }

        boolean success = passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        if (success) {
            redirectAttributes.addFlashAttribute("success", "Password reset successfully! Please log in with your new password.");
            return "redirect:/login";
        } else {
            model.addAttribute("error", "Invalid or expired reset link. Please request a new one.");
            return "reset-password";
        }
    }
}
