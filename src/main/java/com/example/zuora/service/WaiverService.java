package com.example.zuora.service;

import com.example.zuora.dto.WaiverRequest;
import com.example.zuora.model.User;
import com.example.zuora.model.WaiverAcceptance;
import com.example.zuora.repository.WaiverAcceptanceRepository;
import com.example.zuora.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for handling liability waiver acceptances
 */
@Service
public class WaiverService {

    private final WaiverAcceptanceRepository waiverRepository;
    private final UserRepository userRepository;

    @Value("${app.waiver.current-version:2024-01}")
    private String currentWaiverVersion;

    public WaiverService(WaiverAcceptanceRepository waiverRepository,
                        UserRepository userRepository) {
        this.waiverRepository = waiverRepository;
        this.userRepository = userRepository;
    }

    /**
     * Accept the liability waiver
     */
    @Transactional
    public WaiverAcceptance acceptWaiver(User user, WaiverRequest request, HttpServletRequest httpRequest) {
        WaiverAcceptance waiver = new WaiverAcceptance();
        waiver.setUser(user);
        waiver.setSignatureName(request.getSignatureName());
        waiver.setSignatureData(request.getSignatureData());
        waiver.setVersion(request.getWaiverVersion());
        waiver.setWaiverType("LIABILITY");
        waiver.setIpAddress(httpRequest.getRemoteAddr());
        waiver.setUserAgent(httpRequest.getHeader("User-Agent"));
        waiver.setContentHash(generateContentHash(request.getWaiverVersion()));

        waiver = waiverRepository.save(waiver);

        // Update user's waiver tracking
        user.setWaiverAcceptedAt(LocalDateTime.now());
        user.setWaiverVersion(request.getWaiverVersion());
        userRepository.save(user);

        return waiver;
    }

    /**
     * Check if user has accepted the waiver
     */
    public boolean hasAcceptedWaiver(Long userId) {
        return waiverRepository.existsByUserId(userId);
    }

    /**
     * Check if user has accepted the current version of the waiver
     */
    public boolean hasAcceptedCurrentWaiver(Long userId) {
        WaiverAcceptance latest = getLatestWaiver(userId);
        if (latest == null) {
            return false;
        }
        return currentWaiverVersion.equals(latest.getVersion());
    }

    /**
     * Get the latest waiver acceptance for a user
     */
    public WaiverAcceptance getLatestWaiver(Long userId) {
        return waiverRepository.findFirstByUserIdOrderByAcceptedAtDesc(userId).orElse(null);
    }

    /**
     * Get all waiver acceptances for a user
     */
    public List<WaiverAcceptance> getWaiverHistory(Long userId) {
        return waiverRepository.findByUserId(userId);
    }

    /**
     * Get the current waiver version
     */
    public String getCurrentWaiverVersion() {
        return currentWaiverVersion;
    }

    /**
     * Generate content hash for waiver verification
     */
    private String generateContentHash(String version) {
        // In production, this would hash the actual waiver text content
        return "waiver-hash-" + version + "-" + System.currentTimeMillis();
    }
}