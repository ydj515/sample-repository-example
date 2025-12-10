package com.example.graphqlexample.repository

import com.example.graphqlexample.domain.CartItem
import org.springframework.data.jpa.repository.JpaRepository

interface CartItemRepository : JpaRepository<CartItem, Long>
