package com.example.zuora.repository;

import com.example.zuora.model.RatePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatePlanRepository extends JpaRepository<RatePlan, Long> {
    List<RatePlan> findByProductId(Long productId);
    List<RatePlan> findByProductIdAndStatus(Long productId, RatePlan.Status status);
    Optional<RatePlan> findByZuoraRatePlanId(String zuoraRatePlanId);
}
