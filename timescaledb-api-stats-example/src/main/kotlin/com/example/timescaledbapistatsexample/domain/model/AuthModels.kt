package com.example.timescaledbapistatsexample.domain.model

data class ApiClient(
    val id: Long,
    val name: String,
    val apiKeyHash: String,
    val enabled: Boolean,
)

data class ApiRoute(
    val id: Long,
    val method: String,
    val pathPattern: String,
    val description: String,
)

data class ApiClientPrincipal(
    val id: Long,
    val name: String,
)

enum class ApiAuthResult {
    ALLOWED,
    MISSING_API_KEY,
    INVALID_API_KEY,
    FORBIDDEN,
}

data class ApiAccessSnapshot(
    val clientsByApiKeyHash: Map<String, ApiClient>,
    val routes: List<ApiRoute>,
    val routeIdsByClientId: Map<Long, Set<Long>>,
)
