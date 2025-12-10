package com.example.graphqlexample.repository

import com.example.graphqlexample.domain.Cart
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.EntityGraph

interface CartRepository : JpaRepository<Cart, Long> {
    @EntityGraph(attributePaths = ["items", "items.product"])
    fun findByUserId(userId: Long): Cart?
}
