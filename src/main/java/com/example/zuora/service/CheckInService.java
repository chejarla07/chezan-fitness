package com.example.zuora.service;

import com.example.zuora.model.CheckIn;
import com.example.zuora.model.User;
import com.example.zuora.repository.CheckInRepository;
import com.example.zuora.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling gym check-in/check-out
 */
@Service
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final UserRepository userRepository;

    public CheckInService(CheckInRepository checkInRepository,
                          UserRepository userRepository) {
        this.checkInRepository = checkInRepository;
        this.userRepository = userRepository;
    }

    /**
     * Check in a user
     */
    @Transactional
    public CheckIn checkIn(User user, String qrCode, String ipAddress) {
        // Validate user can check in
        validateUserCanCheckIn(user);

        // Check if already checked in
        if (checkInRepository.existsByUserIdAndCheckOutTimeIsNull(user.getId())) {
            throw new RuntimeException("User is already checked in. Please check out first.");
        }

        CheckIn checkIn = new CheckIn();
        checkIn.setUser(user);
        checkIn.setQrCodeUsed(qrCode);
        checkIn.setIpAddress(ipAddress);
        checkIn.setVerificationMethod(CheckIn.VerificationMethod.QR_CODE);

        // Update user's last check-in time
        user.setLastCheckInAt(LocalDateTime.now());
        userRepository.save(user);

        return checkInRepository.save(checkIn);
    }

    /**
     * Check out a user
     */
    @Transactional
    public CheckIn checkOut(User user) {
        CheckIn checkIn = checkInRepository
            .findFirstByUserIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(user.getId())
            .orElseThrow(() -> new RuntimeException("No active check-in found. Please check in first."));

        checkIn.setCheckOutTime(LocalDateTime.now());
        return checkInRepository.save(checkIn);
    }

    /**
     * Get check-in history for a user
     */
    public List<CheckIn> getCheckInHistory(Long userId) {
        return checkInRepository.findByUserIdOrderByCheckInTimeDesc(userId);
    }

    /**
     * Check if user is currently checked in
     */
    public boolean isCheckedIn(Long userId) {
        return checkInRepository.existsByUserIdAndCheckOutTimeIsNull(userId);
    }

    /**
     * Get current active check-in for a user
     */
    public CheckIn getCurrentCheckIn(Long userId) {
        return checkInRepository
            .findFirstByUserIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(userId)
            .orElse(null);
    }

    /**
     * Generate a unique QR code for a user
     */
    public String generateQrCode(User user) {
        return "CF-" + user.getId() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Get total visit count for a user
     */
    public long getVisitCount(Long userId) {
        return checkInRepository.countByUserId(userId);
    }

    /**
     * Get check-ins within a date range
     */
    public List<CheckIn> getCheckInsBetween(Long userId, LocalDateTime start, LocalDateTime end) {
        return checkInRepository.findByUserIdAndCheckInTimeBetween(userId, start, end);
    }

    /**
     * Validate that user can check in
     */
    private void validateUserCanCheckIn(User user) {
        // Check member status
        if (user.getStatus() == User.MemberStatus.SUSPENDED) {
            throw new RuntimeException("Your account is suspended. Please contact support.");
        }
        if (user.getStatus() == User.MemberStatus.CANCELLED) {
            throw new RuntimeException("Your membership is cancelled. Please renew to continue.");
        }
        if (user.getStatus() == User.MemberStatus.BANNED) {
            throw new RuntimeException("Your account has been banned. Please contact management.");
        }

        // Check PAR-Q
        if (user.getParqCompletedAt() == null) {
            throw new RuntimeException("Please complete the health questionnaire (PAR-Q) before checking in.");
        }

        // Check waiver
        if (user.getWaiverAcceptedAt() == null) {
            throw new RuntimeException("Please accept the liability waiver before checking in.");
        }
    }

    /**
     * Count currently checked-in users
     */
    public long countCurrentlyCheckedIn() {
        return checkInRepository.countByCheckOutTimeIsNull();
    }
}