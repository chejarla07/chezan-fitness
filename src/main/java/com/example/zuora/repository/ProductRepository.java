package com.example.zuora.repository;

import com.example.zuora.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByStatus(Product.Status status);
    List<Product> findByCategoryAndStatus(Product.Category category, Product.Status status);
    Optional<Product> findByZuoraProductId(String zuoraProductId);
}
