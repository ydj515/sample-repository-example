package com.example.timescaledbapistatsexample.presentation.request

data class CreateOrderRequest(
    val productId: Long,
    val quantity: Int,
)
