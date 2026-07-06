package com.example.timescaledbapistatsexample.presentation

import com.example.timescaledbapistatsexample.presentation.request.CreateOrderRequest
import com.example.timescaledbapistatsexample.presentation.response.OrderResponse
import com.example.timescaledbapistatsexample.presentation.response.ProductResponse
import com.example.timescaledbapistatsexample.presentation.response.SalesReportResponse
import kotlin.random.Random
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class SampleApiController {
    @GetMapping("/products")
    fun products(): List<ProductResponse> {
        return listOf(
            ProductResponse(id = 1, name = "keyboard", price = 120_000),
            ProductResponse(id = 2, name = "mouse", price = 80_000),
            ProductResponse(id = 3, name = "monitor", price = 350_000),
        )
    }

    @GetMapping("/products/{id}")
    fun product(@PathVariable id: Long): ProductResponse {
        return ProductResponse(id = id, name = "product-$id", price = 10_000 + id.toInt())
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    fun createOrder(@RequestBody request: CreateOrderRequest): OrderResponse {
        return OrderResponse(orderId = Random.nextLong(1_000, 9_999), productId = request.productId, quantity = request.quantity)
    }

    @GetMapping("/reports/sales")
    fun salesReport(): SalesReportResponse {
        return SalesReportResponse(totalSales = 12_500_000, orderCount = 128)
    }

    @GetMapping("/admin/health-check")
    fun adminHealthCheck(): Map<String, String> {
        return mapOf("status" to "ok")
    }

    @GetMapping("/unstable")
    fun unstable(): Map<String, String> {
        if (Random.nextInt(100) < 20) {
            throw IllegalStateException("unstable sample failure")
        }
        return mapOf("status" to "lucky")
    }
}
