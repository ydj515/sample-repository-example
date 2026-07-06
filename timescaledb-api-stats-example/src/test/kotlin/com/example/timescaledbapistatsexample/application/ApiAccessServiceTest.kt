package com.example.timescaledbapistatsexample.application

import com.example.timescaledbapistatsexample.domain.model.ApiAccessSnapshot
import com.example.timescaledbapistatsexample.domain.model.ApiAuthResult
import com.example.timescaledbapistatsexample.domain.model.ApiClient
import com.example.timescaledbapistatsexample.domain.model.ApiRoute
import com.example.timescaledbapistatsexample.domain.port.ApiAccessSnapshotProvider
import com.example.timescaledbapistatsexample.domain.service.Sha256ApiKeyHasher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ApiAccessServiceTest {
    private val productsRoute = ApiRoute(id = 1, method = "GET", pathPattern = "/api/products", description = "상품 목록 조회")
    private val reportsRoute = ApiRoute(id = 2, method = "GET", pathPattern = "/api/reports/sales", description = "매출 리포트 조회")
    private val client = ApiClient(
        id = 1,
        name = "demo-client-01",
        apiKeyHash = Sha256ApiKeyHasher.hash("demo-key-client-01"),
        enabled = true,
    )

    private val service = ApiAccessService(
        snapshotProvider = ApiAccessSnapshotProvider {
            ApiAccessSnapshot(
                clientsByApiKeyHash = mapOf(client.apiKeyHash to client),
                routes = listOf(productsRoute, reportsRoute),
                routeIdsByClientId = mapOf(client.id to setOf(productsRoute.id)),
            )
        },
        loadOnStartup = false,
    )

    @Test
    fun `허용된 API key와 route면 ALLOWED를 반환한다`() {
        val result = service.authorize("demo-key-client-01", "GET", "/api/products")

        assertEquals(ApiAuthResult.ALLOWED, result.authResult)
        assertEquals("demo-client-01", result.principal?.name)
        assertEquals("/api/products", result.route?.pathPattern)
    }

    @Test
    fun `API key가 없으면 MISSING_API_KEY를 반환한다`() {
        val result = service.authorize(null, "GET", "/api/products")

        assertEquals(ApiAuthResult.MISSING_API_KEY, result.authResult)
        assertNull(result.principal)
    }

    @Test
    fun `잘못된 API key면 INVALID_API_KEY를 반환한다`() {
        val result = service.authorize("wrong-key", "GET", "/api/products")

        assertEquals(ApiAuthResult.INVALID_API_KEY, result.authResult)
        assertNull(result.principal)
    }

    @Test
    fun `허용되지 않은 route면 FORBIDDEN을 반환한다`() {
        val result = service.authorize("demo-key-client-01", "GET", "/api/reports/sales")

        assertEquals(ApiAuthResult.FORBIDDEN, result.authResult)
        assertEquals("demo-client-01", result.principal?.name)
        assertEquals("Client is not allowed to call this route", result.deniedReason)
    }
}
