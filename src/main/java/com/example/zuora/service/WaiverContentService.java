package com.example.zuora.service;

import com.example.zuora.model.User;
import com.example.zuora.model.WaiverContent;
import com.example.zuora.repository.WaiverContentRepository;
import com.example.zuora.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing waiver content versions
 */
@Service
public class WaiverContentService {

    private final WaiverContentRepository waiverContentRepository;
    private final UserRepository userRepository;

    public WaiverContentService(WaiverContentRepository waiverContentRepository,
                                UserRepository userRepository) {
        this.waiverContentRepository = waiverContentRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get all waiver versions
     */
    public List<WaiverContent> getAllWaiverVersions() {
        return waiverContentRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Get active waiver content
     */
    public WaiverContent getActiveWaiver() {
        return waiverContentRepository.findByActiveTrue().orElse(null);
    }

    /**
     * Get waiver by ID
     */
    public Optional<WaiverContent> getWaiverById(Long id) {
        return waiverContentRepository.findById(id);
    }

    /**
     * Create new waiver version
     */
    @Transactional
    public WaiverContent createWaiver(String version, String title, String content, Long createdById) {
        // Check if version already exists
        if (waiverContentRepository.existsByVersion(version)) {
            throw new IllegalArgumentException("Waiver version '" + version + "' already exists");
        }

        User createdBy = createdById != null ? userRepository.findById(createdById).orElse(null) : null;

        WaiverContent waiver = new WaiverContent();
        waiver.setVersion(version);
        waiver.setTitle(title);
        waiver.setContent(content);
        waiver.setActive(false);
        waiver.setCreatedBy(createdBy);

        return waiverContentRepository.save(waiver);
    }

    /**
     * Update waiver content
     */
    @Transactional
    public WaiverContent updateWaiver(Long id, String title, String content) {
        WaiverContent waiver = waiverContentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Waiver not found with id: " + id));

        waiver.setTitle(title);
        waiver.setContent(content);

        return waiverContentRepository.save(waiver);
    }

    /**
     * Activate a waiver version (deactivates all others)
     */
    @Transactional
    public WaiverContent activateWaiver(Long id) {
        WaiverContent waiver = waiverContentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Waiver not found with id: " + id));

        // Deactivate all other waivers
        waiverContentRepository.findAll().forEach(w -> w.setActive(false));

        // Activate this one
        waiver.setActive(true);

        return waiverContentRepository.save(waiver);
    }

    /**
     * Delete a waiver version
     */
    @Transactional
    public void deleteWaiver(Long id) {
        WaiverContent waiver = waiverContentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Waiver not found with id: " + id));

        if (waiver.getActive()) {
            throw new IllegalArgumentException("Cannot delete active waiver version");
        }

        waiverContentRepository.delete(waiver);
    }

    /**
     * Check if any waiver exists
     */
    public boolean hasWaivers() {
        return waiverContentRepository.count() > 0;
    }

    /**
     * Initialize default waiver if none exists
     */
    @Transactional
    public void initializeDefaultWaiver() {
        if (waiverContentRepository.count() > 0) {
            return; // Waivers already exist
        }

        WaiverContent defaultWaiver = new WaiverContent();
        defaultWaiver.setVersion("2024-01");
        defaultWaiver.setTitle("Liability Waiver and Release");
        defaultWaiver.setContent("LIABILITY WAIVER AND RELEASE FORM\n\n" +
            "In consideration of being allowed to participate in the fitness programs and use the facilities, equipment, and services offered by Chezan Fitness (hereinafter referred to as \"the Facility\"), I hereby acknowledge and agree to the following:\n\n" +
            "1. ASSUMPTION OF RISK\n" +
            "I acknowledge that participation in physical activities involves inherent risks, including but not limited to the risk of serious injury, disability, or death. I voluntarily assume all risks associated with my participation in any fitness program or use of any equipment at the Facility.\n\n" +
            "2. RELEASE OF LIABILITY\n" +
            "I hereby release, waive, discharge, and covenant not to sue Chezan Fitness, its officers, directors, employees, agents, and affiliates from any and all liability, claims, demands, actions, and causes of action whatsoever arising out of or related to any loss, damage, or injury, including death, that may be sustained by me, or any property belonging to me, while participating in any activity at the Facility.\n\n" +
            "3. MEDICAL ACKNOWLEDGMENT\n" +
            "I represent and warrant that I am physically fit and have no medical condition that would prevent my safe participation in physical activities. I understand that I should consult with a physician before beginning any exercise program.\n\n" +
            "4. INDEMNIFICATION\n" +
            "I agree to indemnify and hold harmless Chezan Fitness from any and all claims, actions, suits, costs, expenses, damages, and liabilities, including attorney's fees, arising out of my use of the Facility or participation in any fitness program.\n\n" +
            "5. EMERGENCY MEDICAL TREATMENT\n" +
            "I authorize Chezan Fitness to seek emergency medical treatment on my behalf in case of injury or medical emergency.\n\n" +
            "6. PERSONAL PROPERTY\n" +
            "I understand that Chezan Fitness is not responsible for any personal property that is lost, stolen, or damaged while at the Facility.\n\n" +
            "7. PHOTOGRAPH AND VIDEO RELEASE\n" +
            "I grant Chezan Fitness permission to use my photograph, video, or image for promotional purposes without compensation.\n\n" +
            "8. GOVERNING LAW\n" +
            "This Waiver shall be governed by and construed in accordance with the laws of the jurisdiction in which the Facility is located.\n\n" +
            "BY SIGNING BELOW, I ACKNOWLEDGE THAT I HAVE READ AND UNDERSTOOD THIS WAIVER AND RELEASE, AND I AGREE TO ALL OF ITS TERMS AND CONDITIONS VOLUNTARILY AND OF MY OWN FREE WILL.\n\n" +
            "Participant Signature: ________________________\n" +
            "Printed Name: ________________________\n" +
            "Date: ________________________\n" +
            "Emergency Contact Name: ________________________\n" +
            "Emergency Contact Phone: ________________________");
        defaultWaiver.setActive(true);

        waiverContentRepository.save(defaultWaiver);
    }
}