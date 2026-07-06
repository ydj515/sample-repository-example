package com.example.timescaledbapistatsexample.domain.service

import com.example.timescaledbapistatsexample.domain.model.ApiRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoutePatternMatcherTest {
    private val matcher = RoutePatternMatcher(
        listOf(
            ApiRoute(id = 1, method = "GET", pathPattern = "/api/products", description = "상품 목록 조회"),
            ApiRoute(id = 2, method = "GET", pathPattern = "/api/products/{id}", description = "상품 단건 조회"),
            ApiRoute(id = 3, method = "POST", pathPattern = "/api/orders", description = "주문 생성"),
        ),
    )

    @Test
    fun `정확히 일치하는 route를 찾는다`() {
        val route = matcher.find("GET", "/api/products")

        assertEquals("/api/products", route?.pathPattern)
    }

    @Test
    fun `path variable route를 찾는다`() {
        val route = matcher.find("GET", "/api/products/42")

        assertEquals("/api/products/{id}", route?.pathPattern)
    }

    @Test
    fun `method가 다르면 route를 찾지 않는다`() {
        val route = matcher.find("DELETE", "/api/products/42")

        assertNull(route)
    }
}
