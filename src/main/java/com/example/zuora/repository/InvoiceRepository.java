package com.example.zuora.repository;

import com.example.zuora.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByUserId(Long userId);
    List<Invoice> findByUserIdAndStatus(Long userId, Invoice.InvoiceStatus status);
    Optional<Invoice> findByZuoraInvoiceId(String zuoraInvoiceId);
}
