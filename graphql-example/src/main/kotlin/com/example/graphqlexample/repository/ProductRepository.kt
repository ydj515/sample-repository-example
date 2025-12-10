package com.example.graphqlexample.repository

import com.example.graphqlexample.domain.Product
import org.springframework.data.jpa.repository.JpaRepository

interface ProductRepository : JpaRepository<Product, Long> {
    fun findByNameContainingIgnoreCase(keyword: String): List<Product>
}
