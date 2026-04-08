package com.example.zuora.model;

import com.example.zuora.util.PhoneFormatter;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // ==================== NEW FIELDS ====================

    // Personal Information
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

    // Address Fields (local storage)
    @Column(name = "address_line_1")
    private String addressLine1;

    @Column(name = "address_line_2")
    private String addressLine2;

    private String city;

    private String state;

    @Column(name = "zip_code")
    private String zipCode;

    private String country;

    // Emergency Contact
    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone")
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relationship")
    private String emergencyContactRelationship;

    // Health Information
    @Column(name = "health_conditions", columnDefinition = "TEXT")
    private String healthConditions;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Column(columnDefinition = "TEXT")
    private String medications;

    // PAR-Q Questionnaire Tracking
    @Column(name = "parq_completed_at")
    private LocalDateTime parqCompletedAt;

    @Column(name = "parq_version")
    private String parqVersion;

    // Liability Waiver Tracking
    @Column(name = "waiver_accepted_at")
    private LocalDateTime waiverAcceptedAt;

    @Column(name = "waiver_version")
    private String waiverVersion;

    // Terms Acceptance Tracking
    @Column(name = "terms_accepted_at")
    private LocalDateTime termsAcceptedAt;

    @Column(name = "terms_version")
    private String termsVersion;

    // Membership Tracking
    @Column(name = "membership_start_date")
    private LocalDate membershipStartDate;

    @Column(name = "last_check_in_at")
    private LocalDateTime lastCheckInAt;

    // Member Status
    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    // Email Verification
    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "verification_token", unique = true)
    private String verificationToken;

    @Column(name = "verification_token_expires")
    private LocalDateTime verificationTokenExpires;

    // Password Reset
    @Column(name = "password_reset_token", unique = true)
    private String passwordResetToken;

    @Column(name = "password_reset_expires")
    private LocalDateTime passwordResetExpires;

    // ==================== END NEW FIELDS ====================

    // Zuora Integration Fields
    @Column(name = "zuora_account_id")
    private String zuoraAccountId;

    @Column(name = "zuora_account_number")
    private String zuoraAccountNumber;

    @Column(name = "zuora_bill_to_contact_id")
    private String zuoraBillToContactId;

    @Column(name = "zuora_sold_to_contact_id")
    private String zuoraSoldToContactId;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== ENUMS ====================

    public enum Role {
        ADMIN, CUSTOMER
    }

    public enum Gender {
        MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
    }

    public enum MemberStatus {
        ACTIVE, SUSPENDED, CANCELLED, BANNED
    }

    // ==================== LIFECYCLE CALLBACKS ====================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (emailVerified == null) {
            emailVerified = false;
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== GETTERS AND SETTERS ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    // Personal Information
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }

    // Address
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

    // Emergency Contact
    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }

    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }

    public String getEmergencyContactRelationship() { return emergencyContactRelationship; }
    public void setEmergencyContactRelationship(String emergencyContactRelationship) { this.emergencyContactRelationship = emergencyContactRelationship; }

    // Health Information
    public String getHealthConditions() { return healthConditions; }
    public void setHealthConditions(String healthConditions) { this.healthConditions = healthConditions; }

    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }

    public String getMedications() { return medications; }
    public void setMedications(String medications) { this.medications = medications; }

    // PAR-Q Tracking
    public LocalDateTime getParqCompletedAt() { return parqCompletedAt; }
    public void setParqCompletedAt(LocalDateTime parqCompletedAt) { this.parqCompletedAt = parqCompletedAt; }

    public String getParqVersion() { return parqVersion; }
    public void setParqVersion(String parqVersion) { this.parqVersion = parqVersion; }

    // Waiver Tracking
    public LocalDateTime getWaiverAcceptedAt() { return waiverAcceptedAt; }
    public void setWaiverAcceptedAt(LocalDateTime waiverAcceptedAt) { this.waiverAcceptedAt = waiverAcceptedAt; }

    public String getWaiverVersion() { return waiverVersion; }
    public void setWaiverVersion(String waiverVersion) { this.waiverVersion = waiverVersion; }

    // Terms Tracking
    public LocalDateTime getTermsAcceptedAt() { return termsAcceptedAt; }
    public void setTermsAcceptedAt(LocalDateTime termsAcceptedAt) { this.termsAcceptedAt = termsAcceptedAt; }

    public String getTermsVersion() { return termsVersion; }
    public void setTermsVersion(String termsVersion) { this.termsVersion = termsVersion; }

    // Membership Tracking
    public LocalDate getMembershipStartDate() { return membershipStartDate; }
    public void setMembershipStartDate(LocalDate membershipStartDate) { this.membershipStartDate = membershipStartDate; }

    public LocalDateTime getLastCheckInAt() { return lastCheckInAt; }
    public void setLastCheckInAt(LocalDateTime lastCheckInAt) { this.lastCheckInAt = lastCheckInAt; }

    // Member Status
    public MemberStatus getStatus() { return status; }
    public void setStatus(MemberStatus status) { this.status = status; }

    // Email Verification
    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }

    public LocalDateTime getVerificationTokenExpires() { return verificationTokenExpires; }
    public void setVerificationTokenExpires(LocalDateTime verificationTokenExpires) { this.verificationTokenExpires = verificationTokenExpires; }

    // Password Reset
    public String getPasswordResetToken() { return passwordResetToken; }
    public void setPasswordResetToken(String passwordResetToken) { this.passwordResetToken = passwordResetToken; }

    public LocalDateTime getPasswordResetExpires() { return passwordResetExpires; }
    public void setPasswordResetExpires(LocalDateTime passwordResetExpires) { this.passwordResetExpires = passwordResetExpires; }

    // Zuora Integration
    public String getZuoraAccountId() { return zuoraAccountId; }
    public void setZuoraAccountId(String zuoraAccountId) { this.zuoraAccountId = zuoraAccountId; }

    public String getZuoraAccountNumber() { return zuoraAccountNumber; }
    public void setZuoraAccountNumber(String zuoraAccountNumber) { this.zuoraAccountNumber = zuoraAccountNumber; }

    public String getZuoraBillToContactId() { return zuoraBillToContactId; }
    public void setZuoraBillToContactId(String zuoraBillToContactId) { this.zuoraBillToContactId = zuoraBillToContactId; }

    public String getZuoraSoldToContactId() { return zuoraSoldToContactId; }
    public void setZuoraSoldToContactId(String zuoraSoldToContactId) { this.zuoraSoldToContactId = zuoraSoldToContactId; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ==================== HELPER METHODS ====================

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isActiveMember() {
        return status == null || status == MemberStatus.ACTIVE;
    }

    public boolean hasCompletedParq() {
        return parqCompletedAt != null;
    }

    public boolean hasAcceptedWaiver() {
        return waiverAcceptedAt != null;
    }

    public boolean hasAcceptedTerms() {
        return termsAcceptedAt != null;
    }

    public boolean canCheckIn() {
        return isActiveMember() && hasCompletedParq() && hasAcceptedWaiver();
    }

    /**
     * Returns the phone number formatted as (XXX) XXX-XXXX
     */
    public String getFormattedPhone() {
        return PhoneFormatter.format(this.phone);
    }

    /**
     * Returns the emergency contact phone formatted as (XXX) XXX-XXXX
     */
    public String getFormattedEmergencyPhone() {
        return PhoneFormatter.format(this.emergencyContactPhone);
    }
}