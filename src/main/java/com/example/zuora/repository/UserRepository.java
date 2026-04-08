package com.example.zuora.repository;

import com.example.zuora.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByZuoraAccountId(String zuoraAccountId);

    // Email verification
    Optional<User> findByVerificationToken(String verificationToken);

    // Password reset
    Optional<User> findByPasswordResetToken(String passwordResetToken);

    // Status-based queries
    List<User> findByStatus(User.MemberStatus status);

    List<User> findByIsActiveTrue();

    // Check for existing active subscription users
    List<User> findByRole(User.Role role);
}