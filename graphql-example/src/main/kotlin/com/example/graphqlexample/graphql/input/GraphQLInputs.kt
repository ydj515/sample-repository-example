package com.example.graphqlexample.graphql.input

import com.example.graphqlexample.domain.ProductType

data class CreateUserInput(
    val name: String,
    val email: String
)

data class AddProductInput(
    val name: String,
    val price: Int,
    val productType: ProductType,
    val warrantyPeriod: Int? = null,
    val size: String? = null
)

data class AddCartItemInput(
    val userId: Long,
    val productId: Long,
    val quantity: Int
)

data class RemoveCartItemInput(
    val userId: Long,
    val cartItemId: Long
)
