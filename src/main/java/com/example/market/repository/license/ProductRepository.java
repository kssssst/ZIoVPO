package com.example.market.repository.license;

import com.example.market.model.license.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}