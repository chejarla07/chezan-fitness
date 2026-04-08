package com.example.zuora.repository;

import com.example.zuora.model.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    List<PaymentMethod> findByUserId(Long userId);
    List<PaymentMethod> findByUserIdAndIsActive(Long userId, Boolean isActive);
    Optional<PaymentMethod> findByUserIdAndIsDefault(Long userId, Boolean isDefault);
    Optional<PaymentMethod> findByZuoraPaymentMethodId(String zuoraPaymentMethodId);
}
