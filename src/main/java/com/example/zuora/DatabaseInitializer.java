package com.example.zuora;

import com.example.zuora.model.*;
import com.example.zuora.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final RatePlanRepository ratePlanRepository;
    private final RatePlanChargeRepository ratePlanChargeRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseInitializer(UserRepository userRepository,
                                ProductRepository productRepository,
                                RatePlanRepository ratePlanRepository,
                                RatePlanChargeRepository ratePlanChargeRepository,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.ratePlanRepository = ratePlanRepository;
        this.ratePlanChargeRepository = ratePlanChargeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initializeAdminUser();
        initializeDefaultProducts();
    }

    private void initializeAdminUser() {
        if (!userRepository.existsByEmail("admin@chezanfitness.com")) {
            User admin = new User();
            admin.setEmail("admin@chezanfitness.com");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setRole(User.Role.ADMIN);
            admin.setIsActive(true);
            userRepository.save(admin);
            System.out.println("Admin user created: admin@chezanfitness.com / admin123");
        }
    }

    private void initializeDefaultProducts() {
        if (productRepository.count() == 0) {
            // Basic Membership
            Product basic = createProduct("Basic Membership",
                "Access to gym floor, cardio equipment, and locker rooms. Perfect for those starting their fitness journey.",
                Product.Category.MEMBERSHIP);
            createRatePlan(basic, "Monthly Basic", "Billed monthly", 29.99, RatePlan.BillingPeriod.Month);
            createRatePlan(basic, "Annual Basic", "Billed annually (Save 20%)", 287.90, RatePlan.BillingPeriod.Annual);

            // Premium Membership
            Product premium = createProduct("Premium Membership",
                "Full gym access plus unlimited group fitness classes, pool, and spa access. Includes 2 guest passes per month.",
                Product.Category.MEMBERSHIP);
            createRatePlan(premium, "Monthly Premium", "Billed monthly", 49.99, RatePlan.BillingPeriod.Month);
            createRatePlan(premium, "Annual Premium", "Billed annually (Save 25%)", 449.90, RatePlan.BillingPeriod.Annual);

            // Elite Membership
            Product elite = createProduct("Elite Membership",
                "The ultimate fitness experience with personal training sessions, nutrition coaching, priority class booking, and unlimited guest privileges.",
                Product.Category.MEMBERSHIP);
            createRatePlan(elite, "Monthly Elite", "Billed monthly", 79.99, RatePlan.BillingPeriod.Month);
            createRatePlan(elite, "Annual Elite", "Billed annually (Save 30%)", 671.90, RatePlan.BillingPeriod.Annual);

            // Personal Training Add-on
            Product training = createProduct("Personal Training Package",
                "One-on-one training sessions with certified fitness professionals. Customized workout plans.",
                Product.Category.PERSONAL_TRAINING);
            createRatePlan(training, "4 Sessions/Month", "Monthly commitment", 199.99, RatePlan.BillingPeriod.Month);
            createRatePlan(training, "8 Sessions/Month", "Monthly commitment", 349.99, RatePlan.BillingPeriod.Month);

            // Class Pass Add-on
            Product classes = createProduct("Group Classes Add-on",
                "Unlimited access to yoga, spin, HIIT, pilates, and specialty classes. New classes weekly.",
                Product.Category.ADD_ON);
            createRatePlan(classes, "Unlimited Classes", "Monthly add-on", 29.99, RatePlan.BillingPeriod.Month);

            System.out.println("Default fitness products initialized");
        }
    }

    private Product createProduct(String name, String description, Product.Category category) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setCategory(category);
        product.setStatus(Product.Status.Active);
        product.setEffectiveStartDate(LocalDate.now());
        return productRepository.save(product);
    }

    private void createRatePlan(Product product, String name, String description, double price, RatePlan.BillingPeriod period) {
        RatePlan plan = new RatePlan();
        plan.setProduct(product);
        plan.setName(name);
        plan.setDescription(description);
        plan.setBillingPeriod(period);
        plan.setStatus(RatePlan.Status.Active);
        plan.setEffectiveStartDate(LocalDate.now());
        // Note: zuoraRatePlanId will be set when synced to Zuora via admin panel
        plan = ratePlanRepository.save(plan);

        RatePlanCharge charge = new RatePlanCharge();
        charge.setRatePlan(plan);
        charge.setName(name + " Recurring Charge");
        charge.setChargeType(RatePlanCharge.ChargeType.Recurring);
        charge.setChargeModel(RatePlanCharge.ChargeModel.FlatFee);
        charge.setAmount(price);
        charge.setCurrency("USD");
        charge.setBillingTiming(RatePlanCharge.BillingTiming.InAdvance);
        // Note: zuoraChargeId will be set when synced to Zuora via admin panel
        ratePlanChargeRepository.save(charge);
    }
}
