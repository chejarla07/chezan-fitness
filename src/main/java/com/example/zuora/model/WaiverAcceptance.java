package com.example.zuora.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Liability Waiver Acceptance entity
 * Records member's acceptance of gym liability waiver
 */
@Entity
@Table(name = "waiver_acceptances")
public class WaiverAcceptance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Waiver Details
    @Column(name = "waiver_type", nullable = false)
    private String waiverType = "LIABILITY"; // For future extensibility (e.g., EQUIPMENT, TRAINING)

    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "accepted_at", nullable = false)
    private LocalDateTime acceptedAt;

    // Digital Signature
    @Column(name = "signature_data", columnDefinition = "TEXT")
    private String signatureData; // Base64 encoded signature image (optional)

    @Column(name = "signature_name", nullable = false)
    private String signatureName; // Typed name as signature confirmation

    // Tracking Information
    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    // Content hash for verification (ensures waiver text hasn't changed)
    @Column(name = "content_hash")
    private String contentHash;

    @PrePersist
    protected void onCreate() {
        acceptedAt = LocalDateTime.now();
    }

    // ==================== GETTERS AND SETTERS ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getWaiverType() { return waiverType; }
    public void setWaiverType(String waiverType) { this.waiverType = waiverType; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }

    public String getSignatureData() { return signatureData; }
    public void setSignatureData(String signatureData) { this.signatureData = signatureData; }

    public String getSignatureName() { return signatureName; }
    public void setSignatureName(String signatureName) { this.signatureName = signatureName; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
}