package com.example.zuora.repository;

import com.example.zuora.model.Discount;
import com.example.zuora.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {

    List<Discount> findByStatus(Discount.Status status);

    List<Discount> findByProduct(Product product);

    List<Discount> findByRatePlanId(Long ratePlanId);

    Optional<Discount> findByDiscountCode(String discountCode);

    Optional<Discount> findByZuoraDiscountId(String zuoraDiscountId);

    @Query("SELECT d FROM Discount d WHERE d.status = :status AND " +
           "(d.startDate IS NULL OR d.startDate <= :today) AND " +
           "(d.endDate IS NULL OR d.endDate >= :today) AND " +
           "(d.maxRedemptions IS NULL OR d.currentRedemptions < d.maxRedemptions)")
    List<Discount> findActiveDiscounts(@Param("status") Discount.Status status, @Param("today") LocalDate today);

    @Query("SELECT d FROM Discount d WHERE d.product.id = :productId AND d.status = :status")
    List<Discount> findByProductIdAndStatus(@Param("productId") Long productId, @Param("status") Discount.Status status);

    @Query("SELECT d FROM Discount d WHERE d.ratePlan.id = :ratePlanId AND d.status = :status")
    List<Discount> findByRatePlanIdAndStatus(@Param("ratePlanId") Long ratePlanId, @Param("status") Discount.Status status);

    boolean existsByDiscountCode(String discountCode);
}