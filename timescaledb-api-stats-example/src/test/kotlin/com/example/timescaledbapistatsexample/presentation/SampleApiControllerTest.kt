package com.example.timescaledbapistatsexample.presentation

import com.example.timescaledbapistatsexample.presentation.request.CreateOrderRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.springframework.http.HttpStatus

class SampleApiControllerTest {
    private val controller = SampleApiController()

    @Test
    fun `상품 목록을 반환한다`() {
        val products = controller.products()

        assertEquals(3, products.size)
        assertEquals("keyboard", products.first().name)
    }

    @Test
    fun `상품 단건을 반환한다`() {
        val product = controller.product(42)

        assertEquals(42, product.id)
        assertEquals("product-42", product.name)
    }

    @Test
    fun `주문 생성 응답을 반환한다`() {
        val order = controller.createOrder(CreateOrderRequest(productId = 1, quantity = 2))

        assertTrue(order.orderId in 1_000..9_998)
        assertEquals(1, order.productId)
        assertEquals(2, order.quantity)
    }

    @Test
    fun `관리자 헬스 체크 응답을 반환한다`() {
        val status = controller.adminHealthCheck()

        assertEquals(HttpStatus.OK.name.lowercase(), status["status"])
    }
}
