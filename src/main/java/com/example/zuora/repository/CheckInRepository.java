package com.example.zuora.repository;

import com.example.zuora.model.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    /**
     * Get all check-ins for a user, most recent first
     */
    List<CheckIn> findByUserIdOrderByCheckInTimeDesc(Long userId);

    /**
     * Get the most recent active check-in (not checked out)
     */
    Optional<CheckIn> findFirstByUserIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(Long userId);

    /**
     * Get check-ins for a user within a date range
     */
    List<CheckIn> findByUserIdAndCheckInTimeBetween(Long userId, LocalDateTime start, LocalDateTime end);

    /**
     * Count total visits for a user
     */
    long countByUserId(Long userId);

    /**
     * Check if user is currently checked in
     */
    boolean existsByUserIdAndCheckOutTimeIsNull(Long userId);

    /**
     * Get all check-ins for a facility
     */
    List<CheckIn> findByFacilityIdOrderByCheckInTimeDesc(Long facilityId);

    /**
     * Get all check-ins for today
     */
    List<CheckIn> findByCheckInTimeBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Count active check-ins (users currently in facility)
     */
    long countByCheckOutTimeIsNull();

    /**
     * Get check-ins by verification method
     */
    List<CheckIn> findByVerificationMethod(CheckIn.VerificationMethod verificationMethod);
}