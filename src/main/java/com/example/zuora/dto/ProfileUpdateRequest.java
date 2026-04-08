package com.example.zuora.dto;

import com.example.zuora.model.User;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Enhanced profile update request DTO
 * Includes all editable user profile fields
 */
public class ProfileUpdateRequest {

    @Size(min = 1, max = 50, message = "First name must be between 1-50 characters")
    private String firstName;

    @Size(min = 1, max = 50, message = "Last name must be between 1-50 characters")
    private String lastName;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Please enter a valid phone number")
    private String phone;

    // Personal Information
    private LocalDate dateOfBirth;
    private User.Gender gender;

    // Emergency Contact
    private String emergencyContactName;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Please enter a valid phone number")
    private String emergencyContactPhone;

    private String emergencyContactRelationship;

    // Address (local storage, separate from Zuora billing)
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    // Health Information (optional)
    private String healthConditions;
    private String allergies;
    private String medications;

    // ==================== GETTERS AND SETTERS ====================

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

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

    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getHealthConditions() { return healthConditions; }
    public void setHealthConditions(String healthConditions) { this.healthConditions = healthConditions; }

    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }

    public String getMedications() { return medications; }
    public void setMedications(String medications) { this.medications = medications; }
}