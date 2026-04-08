package com.example.zuora.repository;

import com.example.zuora.model.WaiverAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaiverAcceptanceRepository extends JpaRepository<WaiverAcceptance, Long> {

    /**
     * Get the most recent waiver acceptance for a user
     */
    Optional<WaiverAcceptance> findFirstByUserIdOrderByAcceptedAtDesc(Long userId);

    /**
     * Get all waiver acceptances for a user
     */
    List<WaiverAcceptance> findByUserId(Long userId);

    /**
     * Check if user has accepted a waiver
     */
    boolean existsByUserId(Long userId);

    /**
     * Get acceptances for a specific waiver version
     */
    List<WaiverAcceptance> findByVersion(String version);

    /**
     * Get acceptances by waiver type
     */
    List<WaiverAcceptance> findByWaiverType(String waiverType);
}