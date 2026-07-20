package com.example.timescaledbapistatsexample.application

import com.example.timescaledbapistatsexample.domain.model.ApiAccessSnapshot
import com.example.timescaledbapistatsexample.domain.model.ApiAuthResult
import com.example.timescaledbapistatsexample.domain.model.ApiClientPrincipal
import com.example.timescaledbapistatsexample.domain.model.ApiRoute
import com.example.timescaledbapistatsexample.domain.port.ApiAccessSnapshotProvider
import com.example.timescaledbapistatsexample.domain.service.RoutePatternMatcher
import com.example.timescaledbapistatsexample.domain.service.Sha256ApiKeyHasher
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
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
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * snapshot과 matcher를 한 객체로 묶어 volatile 쓰기 한 번으로 교체한다.
     *
     * 두 필드를 따로 두면 갱신 중간 상태가 노출된다. 주기 갱신이 새 snapshot을 쓴 직후
     * matcher를 쓰기 전에 요청 스레드가 끼어들면, 옛 matcher가 찾아 준 route.id를
     * 새 snapshot의 권한 맵과 대조하게 된다. 두 값은 항상 같은 세대여야 한다.
     */
    private data class AccessState(
        val snapshot: ApiAccessSnapshot,
        val matcher: RoutePatternMatcher,
    )

    @Volatile
    private var state: AccessState? = null

    /**
     * 기동 시 스냅샷을 미리 적재한다.
     *
     * 실패해도 예외를 던지지 않는다. 기동 시점에 DB가 아직 안 떠 있다는 이유로 앱 전체가
     * 죽는 것보다, 첫 요청이나 다음 주기 갱신에서 다시 시도하는 편이 낫다.
     */
    @PostConstruct
    fun initialize() {
        if (!loadOnStartup) return

        runCatching { refresh() }.onFailure { ex ->
            log.warn("Failed to load API access snapshot on startup; will retry on next refresh", ex)
        }
    }

    /**
     * 스냅샷을 주기적으로 다시 읽는다.
     *
     * 이게 없으면 DB에 client나 route를 추가해도 앱을 재시작해야 반영된다.
     */
    @Scheduled(fixedDelayString = "\${api-stats.auth.refresh-interval-ms:60000}")
    fun refreshPeriodically() {
        runCatching { refresh() }.onFailure { ex ->
            // 갱신 실패 시 직전 스냅샷을 그대로 유지한다. 인증이 통째로 막히는 것보다 낫다.
            log.warn("Failed to refresh API access snapshot; keeping the previous one", ex)
        }
    }

    fun refresh() {
        // route 정규식 컴파일/정렬 비용을 요청마다 치르지 않도록 snapshot 갱신 시 matcher도 함께 만든다.
        state = loadState()
    }

    fun authorize(apiKey: String?, method: String, path: String): AuthorizationDecision {
        // 한 번만 읽어 지역 변수에 담는다. 처리 도중 갱신이 일어나도 같은 세대를 계속 본다.
        val current = state ?: loadState().also { state = it }
        val snapshot = current.snapshot
        val route = current.matcher.find(method, path)

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

    private fun loadState(): AccessState {
        val snapshot = snapshotProvider.loadSnapshot()
        return AccessState(snapshot, RoutePatternMatcher(snapshot.routes))
    }
}
