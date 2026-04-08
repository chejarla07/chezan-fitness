package com.example.zuora.repository;

import com.example.zuora.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserId(Long userId);
    List<Payment> findByInvoiceId(Long invoiceId);
    Optional<Payment> findByZuoraPaymentId(String zuoraPaymentId);
}
