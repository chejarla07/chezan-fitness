package com.example.zuora.repository;

import com.example.zuora.model.RatePlanCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatePlanChargeRepository extends JpaRepository<RatePlanCharge, Long> {
    List<RatePlanCharge> findByRatePlanId(Long ratePlanId);
    Optional<RatePlanCharge> findByZuoraChargeId(String zuoraChargeId);
}
