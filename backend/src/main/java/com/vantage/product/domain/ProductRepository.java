package com.vantage.product.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
@org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"inventories"})
@org.springframework.data.jpa.repository.Query("SELECT p FROM Product p")
java.util.List<Product> findAllWithInventories();
}
