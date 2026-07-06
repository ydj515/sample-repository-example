package com.example.timescaledbapistatsexample.presentation.response

data class ProductResponse(
    val id: Long,
    val name: String,
    val price: Int,
)

data class OrderResponse(
    val orderId: Long,
    val productId: Long,
    val quantity: Int,
)

data class SalesReportResponse(
    val totalSales: Long,
    val orderCount: Int,
)
