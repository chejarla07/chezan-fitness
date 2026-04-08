package com.example.zuora.repository;

import com.example.zuora.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByUserId(Long userId);
    List<Subscription> findByUserIdAndStatus(Long userId, Subscription.SubscriptionStatus status);
    Optional<Subscription> findByZuoraSubscriptionId(String zuoraSubscriptionId);
    long countByUserIdAndStatus(Long userId, Subscription.SubscriptionStatus status);
}
