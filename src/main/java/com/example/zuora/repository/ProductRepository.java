package com.example.zuora.repository;

import com.example.zuora.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByStatus(Product.Status status);
    List<Product> findByCategoryAndStatus(Product.Category category, Product.Status status);
    Optional<Product> findByZuoraProductId(String zuoraProductId);

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.ratePlans WHERE p.category = :category AND p.status = :status")
    List<Product> findByCategoryAndStatusWithRatePlans(@Param("category") Product.Category category, @Param("status") Product.Status status);

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.ratePlans WHERE p.status = :status")
    List<Product> findByStatusWithRatePlans(@Param("status") Product.Status status);

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.ratePlans")
    List<Product> findAllWithRatePlans();
}
