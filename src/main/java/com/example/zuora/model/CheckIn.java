package com.example.zuora.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Check-In entity for tracking gym visits
 */
@Entity
@Table(name = "check_ins")
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "check_in_time", nullable = false)
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    // Location/Facility tracking (for future multi-location support)
    @Column(name = "facility_id")
    private Long facilityId;

    @Column(name = "facility_name")
    private String facilityName;

    // Verification Method
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method")
    private VerificationMethod verificationMethod = VerificationMethod.QR_CODE;

    @Column(name = "verified_by") // Staff ID if manual check-in
    private Long verifiedBy;

    // QR Code used (for audit trail)
    @Column(name = "qr_code_used")
    private String qrCodeUsed;

    @Column(name = "ip_address")
    private String ipAddress;

    public enum VerificationMethod {
        QR_CODE, MANUAL, NFC, BIOMETRIC
    }

    @PrePersist
    protected void onCreate() {
        checkInTime = LocalDateTime.now();
    }

    // ==================== HELPER METHODS ====================

    public boolean isCheckedOut() {
        return checkOutTime != null;
    }

    public long getDurationMinutes() {
        if (checkOutTime == null) return 0;
        return java.time.Duration.between(checkInTime, checkOutTime).toMinutes();
    }

    public String getDurationFormatted() {
        long minutes = getDurationMinutes();
        long hours = minutes / 60;
        long mins = minutes % 60;
        if (hours > 0) {
            return hours + "h " + mins + "m";
        }
        return mins + "m";
    }

    // ==================== GETTERS AND SETTERS ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }

    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }

    public Long getFacilityId() { return facilityId; }
    public void setFacilityId(Long facilityId) { this.facilityId = facilityId; }

    public String getFacilityName() { return facilityName; }
    public void setFacilityName(String facilityName) { this.facilityName = facilityName; }

    public VerificationMethod getVerificationMethod() { return verificationMethod; }
    public void setVerificationMethod(VerificationMethod verificationMethod) { this.verificationMethod = verificationMethod; }

    public Long getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(Long verifiedBy) { this.verifiedBy = verifiedBy; }

    public String getQrCodeUsed() { return qrCodeUsed; }
    public void setQrCodeUsed(String qrCodeUsed) { this.qrCodeUsed = qrCodeUsed; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}