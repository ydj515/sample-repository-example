package com.example.timescaledbapistatsexample.application

import com.example.timescaledbapistatsexample.domain.model.ApiAccessSnapshot
import com.example.timescaledbapistatsexample.domain.model.ApiAuthResult
import com.example.timescaledbapistatsexample.domain.model.ApiClientPrincipal
import com.example.timescaledbapistatsexample.domain.model.ApiRoute
import com.example.timescaledbapistatsexample.domain.port.ApiAccessSnapshotProvider
import com.example.timescaledbapistatsexample.domain.service.RoutePatternMatcher
import com.example.timescaledbapistatsexample.domain.service.Sha256ApiKeyHasher
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

data class AuthorizationDecision(
    val authResult: ApiAuthResult,
    val principal: ApiClientPrincipal?,
    val route: ApiRoute?,
    val deniedReason: String?,
)

@Service
class ApiAccessService(
    private val snapshotProvider: ApiAccessSnapshotProvider,
    @Value("\${api-stats.auth.load-on-startup:true}") private val loadOnStartup: Boolean = true,
) {
    @Volatile
    private var cachedSnapshot: ApiAccessSnapshot? = null

    @Volatile
    private var cachedMatcher: RoutePatternMatcher? = null

    @PostConstruct
    fun initialize() {
        if (loadOnStartup) {
            refresh()
        }
    }

    fun refresh() {
        val snapshot = snapshotProvider.loadSnapshot()
        cachedSnapshot = snapshot
        // route 정규식 컴파일/정렬 비용을 요청마다 치르지 않도록 snapshot 갱신 시 matcher도 함께 캐싱한다.
        cachedMatcher = RoutePatternMatcher(snapshot.routes)
    }

    fun authorize(apiKey: String?, method: String, path: String): AuthorizationDecision {
        val snapshot = currentSnapshot()
        val matcher = cachedMatcher ?: RoutePatternMatcher(snapshot.routes).also { cachedMatcher = it }
        val route = matcher.find(method, path)

        if (route == null) {
            return AuthorizationDecision(
                authResult = ApiAuthResult.ALLOWED,
                principal = null,
                route = null,
                deniedReason = null,
            )
        }

        if (apiKey.isNullOrBlank()) {
            return AuthorizationDecision(
                authResult = ApiAuthResult.MISSING_API_KEY,
                principal = null,
                route = route,
                deniedReason = "API key header is missing",
            )
        }

        val apiKeyHash = Sha256ApiKeyHasher.hash(apiKey)
        val client = snapshot.clientsByApiKeyHash[apiKeyHash]
            ?: return AuthorizationDecision(
                authResult = ApiAuthResult.INVALID_API_KEY,
                principal = null,
                route = route,
                deniedReason = "API key is invalid",
            )

        val allowedRouteIds = snapshot.routeIdsByClientId[client.id].orEmpty()
        if (route.id !in allowedRouteIds) {
            return AuthorizationDecision(
                authResult = ApiAuthResult.FORBIDDEN,
                principal = ApiClientPrincipal(client.id, client.name),
                route = route,
                deniedReason = "Client is not allowed to call this route",
            )
        }

        return AuthorizationDecision(
            authResult = ApiAuthResult.ALLOWED,
            principal = ApiClientPrincipal(client.id, client.name),
            route = route,
            deniedReason = null,
        )
    }

    private fun currentSnapshot(): ApiAccessSnapshot {
        return cachedSnapshot ?: snapshotProvider.loadSnapshot().also { cachedSnapshot = it }
    }
}
