package com.example.graphqlexample.graphql.dto

import com.example.graphqlexample.domain.ProductType
import java.time.OffsetDateTime

sealed interface SearchResult

sealed interface Product : SearchResult {
    val id: Long
    val name: String
    val price: Int
    val productType: ProductType
}

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val createdAt: OffsetDateTime,
    val cart: Cart? = null
) : SearchResult

data class Electronics(
    override val id: Long,
    override val name: String,
    override val price: Int,
    override val productType: ProductType,
    val warrantyPeriod: Int
) : Product

data class Clothing(
    override val id: Long,
    override val name: String,
    override val price: Int,
    override val productType: ProductType,
    val size: String
) : Product

data class CartItem(
    val id: Long,
    val product: Product,
    val quantity: Int
)

data class Cart(
    val id: Long,
    val user: User,
    val items: List<CartItem>,
    val totalAmount: Int
)
