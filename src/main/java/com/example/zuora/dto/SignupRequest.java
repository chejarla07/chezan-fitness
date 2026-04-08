package com.example.zuora.dto;

import com.example.zuora.model.User;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class SignupRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Please enter a valid phone number")
    private String phone;

    // Personal Information (New)
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private User.Gender gender;

    @NotBlank(message = "Address is required")
    private String address1;

    private String address2;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "ZIP code is required")
    private String zipCode;

    @NotBlank(message = "Country is required")
    private String country;

    // Bill To Name (optional - defaults to firstName + lastName if not provided)
    private String billToName;

    // Sold To Address (separate from Bill To)
    private String soldToName;  // Name for Sold To address
    private String soldToAddress1;
    private String soldToAddress2;
    private String soldToCity;
    private String soldToState;
    private String soldToZipCode;
    private String soldToCountry;

    // Flag to indicate if Sold To is same as Bill To
    private boolean sameAsBillTo = true;

    // Emergency Contact (Optional)
    private String emergencyContactName;

    private String emergencyContactPhone;

    private String emergencyContactRelationship;

    // Health Information (Optional)
    private String healthConditions;
    private String allergies;
    private String medications;

    @NotNull(message = "Please select a membership plan")
    private Long ratePlanId;

    private String cardToken;

    // Card details (optional - for display purposes)
    private Integer expirationMonth;
    private Integer expirationYear;
    private String lastFour;
    private String brand;

    // Terms Acceptance (New)
    @AssertTrue(message = "You must accept the terms and conditions")
    private boolean termsAccepted;

    private String termsVersion = "2024-01";

    // Getters and Setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress1() { return address1; }
    public void setAddress1(String address1) { this.address1 = address1; }

    public String getAddress2() { return address2; }
    public void setAddress2(String address2) { this.address2 = address2; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getBillToName() { return billToName; }
    public void setBillToName(String billToName) { this.billToName = billToName; }

    public String getSoldToName() { return soldToName; }
    public void setSoldToName(String soldToName) { this.soldToName = soldToName; }

    public String getSoldToAddress1() { return soldToAddress1; }
    public void setSoldToAddress1(String soldToAddress1) { this.soldToAddress1 = soldToAddress1; }

    public String getSoldToAddress2() { return soldToAddress2; }
    public void setSoldToAddress2(String soldToAddress2) { this.soldToAddress2 = soldToAddress2; }

    public String getSoldToCity() { return soldToCity; }
    public void setSoldToCity(String soldToCity) { this.soldToCity = soldToCity; }

    public String getSoldToState() { return soldToState; }
    public void setSoldToState(String soldToState) { this.soldToState = soldToState; }

    public String getSoldToZipCode() { return soldToZipCode; }
    public void setSoldToZipCode(String soldToZipCode) { this.soldToZipCode = soldToZipCode; }

    public String getSoldToCountry() { return soldToCountry; }
    public void setSoldToCountry(String soldToCountry) { this.soldToCountry = soldToCountry; }

    public boolean isSameAsBillTo() { return sameAsBillTo; }
    public void setSameAsBillTo(boolean sameAsBillTo) { this.sameAsBillTo = sameAsBillTo; }

    public Long getRatePlanId() { return ratePlanId; }
    public void setRatePlanId(Long ratePlanId) { this.ratePlanId = ratePlanId; }

    public String getCardToken() { return cardToken; }
    public void setCardToken(String cardToken) { this.cardToken = cardToken; }

    public Integer getExpirationMonth() { return expirationMonth; }
    public void setExpirationMonth(Integer expirationMonth) { this.expirationMonth = expirationMonth; }

    public Integer getExpirationYear() { return expirationYear; }
    public void setExpirationYear(Integer expirationYear) { this.expirationYear = expirationYear; }

    public String getLastFour() { return lastFour; }
    public void setLastFour(String lastFour) { this.lastFour = lastFour; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    // New fields - Getters and Setters
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public User.Gender getGender() { return gender; }
    public void setGender(User.Gender gender) { this.gender = gender; }

    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }

    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }

    public String getEmergencyContactRelationship() { return emergencyContactRelationship; }
    public void setEmergencyContactRelationship(String emergencyContactRelationship) { this.emergencyContactRelationship = emergencyContactRelationship; }

    public String getHealthConditions() { return healthConditions; }
    public void setHealthConditions(String healthConditions) { this.healthConditions = healthConditions; }

    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }

    public String getMedications() { return medications; }
    public void setMedications(String medications) { this.medications = medications; }

    public boolean isTermsAccepted() { return termsAccepted; }
    public void setTermsAccepted(boolean termsAccepted) { this.termsAccepted = termsAccepted; }

    public String getTermsVersion() { return termsVersion; }
    public void setTermsVersion(String termsVersion) { this.termsVersion = termsVersion; }
}
