package com.example.zuora.service;

import com.example.zuora.dto.ProfileUpdateRequest;
import com.example.zuora.dto.SignupRequest;
import com.example.zuora.model.*;
import com.example.zuora.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final ZuoraApiService zuoraApiService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                      PaymentMethodRepository paymentMethodRepository,
                      ZuoraApiService zuoraApiService,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.zuoraApiService = zuoraApiService;
        this.passwordEncoder = passwordEncoder;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    public User getUserByZuoraAccountId(String zuoraAccountId) {
        return userRepository.findByZuoraAccountId(zuoraAccountId)
                .orElseThrow(() -> new RuntimeException("User not found for Zuora account: " + zuoraAccountId));
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public SignupResult createUser(SignupRequest request) throws Exception {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        // STEP 1: Create Zuora account FIRST (before local user)
        JsonNode zuoraResponse;
        try {
            // Build temporary user data for Zuora call (not saved yet)
            User tempUser = new User();
            tempUser.setEmail(request.getEmail());
            tempUser.setFirstName(request.getFirstName());
            tempUser.setLastName(request.getLastName());
            tempUser.setPhone(request.getPhone());

            zuoraResponse = zuoraApiService.createAccount(tempUser, request);
            System.out.println("Zuora account creation response: " + zuoraResponse.toString());
        } catch (Exception e) {
            String errorMsg = "Unable to create billing account. Please try again or contact support.";
            System.err.println("Zuora account creation failed: " + e.getMessage());
            throw new RuntimeException(errorMsg, e);
        }

        // STEP 2: Validate Zuora response - MUST have success=true AND accountId
        boolean zuoraSuccess = zuoraResponse.has("success") && zuoraResponse.get("success").asBoolean();
        String zuoraAccountId = zuoraResponse.has("accountId") ? zuoraResponse.get("accountId").asText() : null;

        if (!zuoraSuccess || zuoraAccountId == null || zuoraAccountId.isEmpty()) {
            String errorMsg = "We were unable to create your account. Please try again or contact support.";
            System.err.println("Zuora account creation failed. Response: " + zuoraResponse.toString());
            throw new RuntimeException(errorMsg);
        }

        // Extract additional fields from Zuora response
        String zuoraAccountNumber = zuoraResponse.has("accountNumber")
                ? zuoraResponse.get("accountNumber").asText()
                : zuoraAccountId;
        String billToContactId = zuoraResponse.has("billToContactId")
                ? zuoraResponse.get("billToContactId").asText()
                : null;
        String soldToContactId = zuoraResponse.has("soldToContactId")
                ? zuoraResponse.get("soldToContactId").asText()
                : null;

        // STEP 3: Only create local user AFTER Zuora success
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setRole(User.Role.CUSTOMER);
        user.setIsActive(true);

        // Store new personal information fields
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());

        // Store emergency contact information
        user.setEmergencyContactName(request.getEmergencyContactName());
        user.setEmergencyContactPhone(request.getEmergencyContactPhone());
        user.setEmergencyContactRelationship(request.getEmergencyContactRelationship());

        // Store health information (optional)
        user.setHealthConditions(request.getHealthConditions());
        user.setAllergies(request.getAllergies());
        user.setMedications(request.getMedications());

        // Store terms acceptance timestamp if accepted
        if (request.isTermsAccepted()) {
            user.setTermsAcceptedAt(java.time.LocalDateTime.now());
            user.setTermsVersion(request.getTermsVersion() != null ? request.getTermsVersion() : "2024-01");
        }

        // Initialize member status
        user.setStatus(User.MemberStatus.ACTIVE);

        // Store Zuora account information
        user.setZuoraAccountId(zuoraAccountId);
        user.setZuoraAccountNumber(zuoraAccountNumber);
        user.setZuoraBillToContactId(billToContactId);
        user.setZuoraSoldToContactId(soldToContactId);

        user = userRepository.save(user);
        System.out.println("User created successfully with Zuora account: " + zuoraAccountNumber);

        // STEP 4: Create payment method if token provided
        String paymentMethodId = null;
        if (request.getCardToken() != null && !request.getCardToken().isEmpty()) {
            try {
                PaymentMethod paymentMethod = addPaymentMethod(user, request.getCardToken(),
                        PaymentMethod.PaymentType.CreditCard,
                        request.getExpirationMonth(),
                        request.getExpirationYear(),
                        request.getLastFour(),
                        request.getBrand(),
                        true);
                paymentMethodId = paymentMethod.getZuoraPaymentMethodId();
                System.out.println("Payment method created: " + paymentMethodId);
            } catch (Exception e) {
                System.err.println("Payment method creation failed: " + e.getMessage());
                // Log but continue - user can add payment method later
            }
        }

        // Build success result
        return SignupResult.builder()
                .user(user)
                .zuoraSuccess(true)
                .zuoraAccountId(zuoraAccountId)
                .zuoraAccountNumber(zuoraAccountNumber)
                .billToContactId(billToContactId)
                .soldToContactId(soldToContactId)
                .build();
    }

    @Transactional
    public PaymentMethod addPaymentMethod(User user, String token, PaymentMethod.PaymentType type,
                                         Integer expMonth, Integer expYear, String lastFour,
                                         String brand, boolean makeDefault) throws Exception {

        // Create in Zuora
        JsonNode zuoraResponse = zuoraApiService.createPaymentMethod(user.getZuoraAccountId(), token, type);
        String zuoraPaymentMethodId = zuoraResponse.get("id").asText();

        // If making default, unset existing default
        if (makeDefault) {
            paymentMethodRepository.findByUserIdAndIsDefault(user.getId(), true)
                    .ifPresent(pm -> {
                        pm.setIsDefault(false);
                        paymentMethodRepository.save(pm);
                    });
        }

        // Create locally
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setUser(user);
        paymentMethod.setZuoraPaymentMethodId(zuoraPaymentMethodId);
        paymentMethod.setType(type);
        paymentMethod.setIsDefault(makeDefault);
        paymentMethod.setIsActive(true);

        if (type == PaymentMethod.PaymentType.CreditCard) {
            paymentMethod.setCardExpirationMonth(expMonth);
            paymentMethod.setCardExpirationYear(expYear);
            paymentMethod.setCardLastFour(lastFour);
            paymentMethod.setCardBrand(brand);
        }

        return paymentMethodRepository.save(paymentMethod);
    }

    public List<PaymentMethod> getUserPaymentMethods(Long userId) {
        return paymentMethodRepository.findByUserId(userId);
    }

    @Transactional
    public void setDefaultPaymentMethod(Long userId, Long paymentMethodId) {
        // Unset existing default
        paymentMethodRepository.findByUserIdAndIsDefault(userId, true)
                .ifPresent(pm -> {
                    pm.setIsDefault(false);
                    paymentMethodRepository.save(pm);
                });

        // Set new default
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new RuntimeException("Payment method not found"));

        paymentMethod.setIsDefault(true);
        paymentMethodRepository.save(paymentMethod);
    }

    @Transactional
    public void removePaymentMethod(Long paymentMethodId, Long userId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new RuntimeException("Payment method not found"));

        if (!paymentMethod.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        paymentMethod.setIsActive(false);
        paymentMethodRepository.save(paymentMethod);
    }

    public List<User> getAllCustomers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.CUSTOMER)
                .toList();
    }

    @Transactional
    public void updateUserProfile(Long userId, String firstName, String lastName, String phone) throws Exception {
        User user = getUserById(userId);

        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        if (phone != null) user.setPhone(phone);

        userRepository.save(user);

        // Update in Zuora
        if (user.getZuoraAccountId() != null) {
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            if (firstName != null) updates.put("billToContact.firstName", firstName);
            if (lastName != null) updates.put("billToContact.lastName", lastName);
            if (phone != null) updates.put("billToContact.workPhone", phone);
            zuoraApiService.updateAccount(user.getZuoraAccountId(), updates);
        }
    }

    /**
     * Enhanced profile update with new fields
     */
    @Transactional
    public void updateEnhancedProfile(Long userId, ProfileUpdateRequest request) throws Exception {
        User user = getUserById(userId);

        // Basic info
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());

        // Personal information
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) user.setGender(request.getGender());

        // Emergency contact
        if (request.getEmergencyContactName() != null) user.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactPhone() != null) user.setEmergencyContactPhone(request.getEmergencyContactPhone());
        if (request.getEmergencyContactRelationship() != null) user.setEmergencyContactRelationship(request.getEmergencyContactRelationship());

        // Address
        if (request.getAddressLine1() != null) user.setAddressLine1(request.getAddressLine1());
        if (request.getAddressLine2() != null) user.setAddressLine2(request.getAddressLine2());
        if (request.getCity() != null) user.setCity(request.getCity());
        if (request.getState() != null) user.setState(request.getState());
        if (request.getZipCode() != null) user.setZipCode(request.getZipCode());
        if (request.getCountry() != null) user.setCountry(request.getCountry());

        // Health information
        if (request.getHealthConditions() != null) user.setHealthConditions(request.getHealthConditions());
        if (request.getAllergies() != null) user.setAllergies(request.getAllergies());
        if (request.getMedications() != null) user.setMedications(request.getMedications());

        userRepository.save(user);

        // Update in Zuora
        if (user.getZuoraAccountId() != null) {
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            if (request.getFirstName() != null) updates.put("billToContact.firstName", request.getFirstName());
            if (request.getLastName() != null) updates.put("billToContact.lastName", request.getLastName());
            if (request.getPhone() != null) updates.put("billToContact.workPhone", request.getPhone());
            if (!updates.isEmpty()) {
                zuoraApiService.updateAccount(user.getZuoraAccountId(), updates);
            }
        }
    }

    @Transactional
    public User createAdminUser(String email, String password, String firstName, String lastName) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        User admin = new User();
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setFirstName(firstName);
        admin.setLastName(lastName);
        admin.setRole(User.Role.ADMIN);
        admin.setIsActive(true);

        return userRepository.save(admin);
    }

    /**
     * Get Bill To contact details from Zuora
     */
    public com.fasterxml.jackson.databind.JsonNode getBillToContact(User user) throws Exception {
        if (user.getZuoraBillToContactId() == null) {
            return null;
        }
        return zuoraApiService.getContact(user.getZuoraBillToContactId());
    }

    /**
     * Get Sold To contact details from Zuora
     */
    public com.fasterxml.jackson.databind.JsonNode getSoldToContact(User user) throws Exception {
        if (user.getZuoraSoldToContactId() == null) {
            return null;
        }
        return zuoraApiService.getContact(user.getZuoraSoldToContactId());
    }

    /**
     * Get full account details from Zuora
     */
    public com.fasterxml.jackson.databind.JsonNode getZuoraAccountDetails(User user) throws Exception {
        if (user.getZuoraAccountId() == null) {
            return null;
        }
        return zuoraApiService.getAccountDetails(user.getZuoraAccountId());
    }
}
