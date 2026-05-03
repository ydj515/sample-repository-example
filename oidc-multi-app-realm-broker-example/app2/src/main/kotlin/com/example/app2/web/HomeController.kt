package com.example.app2.web

import com.example.app2.config.App2ViewProperties
import com.example.oidccommon.config.OidcSecurityProperties
import com.example.sessioncommon.config.SessionPolicyProperties
import com.example.sessioncommon.session.SessionLookupService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Controller
class HomeController(
    private val sessionLookupService: SessionLookupService,
    private val app2ViewProperties: App2ViewProperties,
    private val oidcSecurityProperties: OidcSecurityProperties,
    private val sessionPolicyProperties: SessionPolicyProperties,
) {

    @GetMapping("/")
    fun index(
        authentication: Authentication?,
        request: HttpServletRequest,
        model: Model,
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) demo: String?,
    ): String {
        val oidcUser = authentication?.principal as? OidcUser
        val grantedAuthorities = authentication?.authorities.orEmpty()
        if (oidcUser != null && !oidcSecurityProperties.hasAccess(grantedAuthorities)) {
            throw AccessDeniedException("App 2 접근 권한이 없습니다.")
        }

        populateHomeModel(
            model = model,
            authentication = authentication,
            request = request,
            from = from,
            demo = demo,
        )

        return "index"
    }

    @GetMapping("/access-denied")
    fun accessDeniedPage(
        authentication: Authentication?,
        request: HttpServletRequest,
        model: Model,
        @RequestParam(required = false) from: String?,
    ): String {
        populateDeniedModel(
            model = model,
            authentication = authentication,
            request = request,
            from = from,
        )
        return "access-denied"
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        exception: AccessDeniedException,
        authentication: Authentication?,
        request: HttpServletRequest,
        response: HttpServletResponse,
        model: Model,
    ): String {
        response.status = HttpServletResponse.SC_FORBIDDEN
        populateDeniedModel(
            model = model,
            authentication = authentication,
            request = request,
            from = app2ViewProperties.appName,
        )
        model.addAttribute("accessDeniedMessage", exception.message ?: "접근 권한이 없습니다.")
        return "access-denied"
    }

    private fun populateHomeModel(
        model: Model,
        authentication: Authentication?,
        request: HttpServletRequest,
        from: String?,
        demo: String?,
    ) {
        val oidcUser = authentication?.principal as? OidcUser
        val grantedAuthorities = authentication?.authorities.orEmpty()
        val authorityNames = grantedAuthorities.map { it.authority }
        val activeSessions = authentication?.name?.let { username ->
            sessionLookupService.findUserSessions(username, sessionPolicyProperties.appId)
        }.orEmpty()
        val username = oidcUser?.preferredUsername ?: authentication?.name ?: "anonymous"
        val encodedAppName = URLEncoder.encode(app2ViewProperties.appName, StandardCharsets.UTF_8)
        val peerLandingUrl = "${app2ViewProperties.peerAppUrl}?from=$encodedAppName&demo=sso"
        val peerAuthorizationUrl = "${app2ViewProperties.peerAppUrl}/oauth2/authorization/keycloak"
        val isMasterAdmin = oidcSecurityProperties.isMasterAdmin(grantedAuthorities)
        val canManageCurrentApp = authorityNames.any(oidcSecurityProperties.adminAuthorities().toSet()::contains)
        val canAccessPeerApp = canAccessPeerApp(grantedAuthorities, username)
        val accountInsight = accountInsight(username, authorityNames, canAccessPeerApp, canManageCurrentApp, isMasterAdmin)

        model.addAttribute("environmentName", app2ViewProperties.environmentName)
        model.addAttribute("suiteName", app2ViewProperties.suiteName)
        model.addAttribute("appName", app2ViewProperties.appName)
        model.addAttribute("description", app2ViewProperties.appDescription)
        model.addAttribute("clientId", app2ViewProperties.clientId)
        model.addAttribute("peerAppName", app2ViewProperties.peerAppName)
        model.addAttribute("peerAppUrl", app2ViewProperties.peerAppUrl)
        model.addAttribute("peerLandingUrl", peerLandingUrl)
        model.addAttribute("peerAuthorizationUrl", peerAuthorizationUrl)
        model.addAttribute("authenticated", authentication?.isAuthenticated == true && oidcUser != null)
        model.addAttribute("username", username)
        model.addAttribute("personaLabel", resolvePersonaLabel(username, authorityNames))
        model.addAttribute("accountInsight", accountInsight)
        model.addAttribute("authorities", authorityNames)
        model.addAttribute("claims", oidcUser?.claims.orEmpty())
        model.addAttribute("sessionId", request.getSession(false)?.id)
        model.addAttribute("activeSessions", activeSessions)
        model.addAttribute("sessionCount", activeSessions.size)
        model.addAttribute("accounts", demoAccounts())
        model.addAttribute("journeySteps", ssoJourney())
        model.addAttribute("auditEvents", auditEvents(authentication?.name ?: "anonymous", canManageCurrentApp, isMasterAdmin))
        model.addAttribute("showSsoBanner", !from.isNullOrBlank() || demo == "sso" || oidcUser != null)
        model.addAttribute("fromApp", from ?: "")
        model.addAttribute("demoMode", demo ?: "")
        model.addAttribute("canAccessPeerApp", canAccessPeerApp)
        model.addAttribute("canManageCurrentApp", canManageCurrentApp)
        model.addAttribute("isMasterAdmin", isMasterAdmin)
        model.addAttribute(
            "adminScopeLabel",
            when {
                isMasterAdmin -> "전체 앱 세션 강제 로그아웃 가능"
                canManageCurrentApp -> "${app2ViewProperties.appName} 세션 강제 로그아웃 가능"
                else -> "관리자 권한 없음"
            },
        )
        model.addAttribute(
            "ssoBannerTitle",
            when {
                oidcUser != null && canAccessPeerApp ->
                    "현재 계정은 ${app2ViewProperties.peerAppName}까지 연속 인증이 가능합니다."
                oidcUser != null ->
                    "현재 계정은 ${app2ViewProperties.peerAppName} 접근 권한이 없습니다."
                !from.isNullOrBlank() ->
                    "${from}에서 이어진 SSO 데모 흐름을 준비했습니다."
                else ->
                    "다중 앱 SSO를 한 화면에서 체험할 수 있습니다."
            },
        )
        model.addAttribute(
            "ssoBannerBody",
            when {
                oidcUser != null && canAccessPeerApp ->
                    "원클릭 전환 버튼을 누르면 Keycloak 세션을 재사용해 ${app2ViewProperties.peerAppName} 인증을 바로 이어갑니다."
                oidcUser != null ->
                    "이 계정은 현재 앱 전용 계정입니다. 아래 권한 매트릭스에서 멀티앱 계정 또는 ${app2ViewProperties.peerAppName} 관리자 계정을 확인해보세요."
                !from.isNullOrBlank() ->
                    "같은 브라우저에서 IdP 세션을 유지한 채 앱을 오가면 재로그인 없이 인증 흐름이 이어집니다."
                else ->
                    "계정 매트릭스와 자동 전환 버튼으로 실제 대형 서비스의 중앙 인증 포털처럼 데모할 수 있게 구성했습니다."
            },
        )
    }

    private fun populateDeniedModel(
        model: Model,
        authentication: Authentication?,
        request: HttpServletRequest,
        from: String?,
    ) {
        val grantedAuthorities = authentication?.authorities.orEmpty()
        val authorityNames = grantedAuthorities.map { it.authority }
        val username = (authentication?.principal as? OidcUser)?.preferredUsername ?: authentication?.name ?: "anonymous"
        val encodedAppName = URLEncoder.encode(app2ViewProperties.appName, StandardCharsets.UTF_8)

        model.addAttribute("environmentName", app2ViewProperties.environmentName)
        model.addAttribute("suiteName", app2ViewProperties.suiteName)
        model.addAttribute("appName", app2ViewProperties.appName)
        model.addAttribute("peerAppName", app2ViewProperties.peerAppName)
        model.addAttribute("peerAppUrl", app2ViewProperties.peerAppUrl)
        model.addAttribute("username", username)
        model.addAttribute("authorities", authorityNames)
        model.addAttribute("requiredRoles", oidcSecurityProperties.accessAuthorities().toList())
        model.addAttribute("accessDeniedMessage", "현재 계정에는 ${app2ViewProperties.appName} 접근 권한이 없습니다.")
        model.addAttribute("fromApp", from ?: "")
        model.addAttribute("loginUrl", "/oauth2/authorization/keycloak")
        model.addAttribute("homeUrl", "/")
        model.addAttribute("peerLandingUrl", "${app2ViewProperties.peerAppUrl}?from=$encodedAppName&demo=sso")
        model.addAttribute("requestPath", request.requestURI)
        model.addAttribute("accounts", demoAccounts())
        model.addAttribute("auditEvents", auditEvents(username, false, false))
    }

    private fun canAccessPeerApp(authorities: Collection<GrantedAuthority>, username: String): Boolean {
        val authorityNames = authorities.map { it.authority }
        val peerAuthorities = app2ViewProperties.peerAccessRoles.map { "ROLE_$it" }
        return username == MULTI_USER ||
            authorityNames.any(peerAuthorities::contains) ||
            oidcSecurityProperties.isMasterAdmin(authorities)
    }

    private fun resolvePersonaLabel(username: String, authorities: List<String>): String {
        return when {
            authorities.contains("ROLE_master-admin") -> "Master Administrator"
            username == MULTI_USER -> "Broker Linked Multi User"
            authorities.contains("ROLE_app1-admin") -> "Agency A Realm Administrator"
            authorities.contains("ROLE_app2-admin") -> "Agency B Realm Administrator"
            authorities.contains("ROLE_app1-user") && authorities.contains("ROLE_app2-user") -> "Multi App User"
            authorities.contains("ROLE_app1-user") -> "Agency A Service User"
            authorities.contains("ROLE_app2-user") -> "Agency B Service User"
            else -> "Guest Session"
        }
    }

    private fun accountInsight(
        username: String,
        authorities: List<String>,
        canAccessPeerApp: Boolean,
        canManageCurrentApp: Boolean,
        isMasterAdmin: Boolean,
    ): AccountInsight {
        return when {
            isMasterAdmin -> AccountInsight(
                tone = "admin",
                label = "master-admin",
                title = "통합 관리자 계정",
                detail = "agency-a, agency-b 두 realm에 연결된 전역 계정입니다. 현재 앱과 peer 앱 모두에서 접근 가능하며 broker 기반 로그아웃 흐름까지 함께 검증할 수 있습니다.",
                currentAppAccess = "${app2ViewProperties.appName} 접근 가능",
                peerAppAccess = "${app2ViewProperties.peerAppName} 접근 가능",
                adminAccess = "전체 앱 세션 관리",
                nextStep = "관리 패널에서 대상 사용자를 입력해 앱 경계별 세션 무효화를 확인하세요.",
            )
            username == MULTI_USER || authorities.contains("ROLE_app1-user") && authorities.contains("ROLE_app2-user") -> AccountInsight(
                tone = "ready",
                label = "multi-user",
                title = "멀티 앱 SSO 사용자",
                detail = "platform-broker realm의 동일 계정이 agency-a와 agency-b local user에 모두 연결된 상태입니다. service realm이 달라도 브로커 세션이 이어져 재로그인 없이 peer 앱으로 이동할 수 있습니다.",
                currentAppAccess = "${app2ViewProperties.appName} 접근 가능",
                peerAppAccess = "${app2ViewProperties.peerAppName} 접근 가능",
                adminAccess = "관리 권한 없음",
                nextStep = "Peer 앱으로 이동해 agency-a realm이 platform-broker 세션을 재사용하는지 확인하세요.",
            )
            canManageCurrentApp -> AccountInsight(
                tone = "manager",
                label = "app-admin",
                title = "${app2ViewProperties.appName} 관리자 계정",
                detail = "agency-b realm에서만 관리자 role을 가진 계정입니다. App 2 세션은 관리할 수 있지만 agency-a realm에는 운영 권한이 없어 peer 앱에서는 접근이 거부됩니다.",
                currentAppAccess = "${app2ViewProperties.appName} 접근 가능",
                peerAppAccess = if (canAccessPeerApp) "${app2ViewProperties.peerAppName} 접근 가능" else "${app2ViewProperties.peerAppName} 403 확인 대상",
                adminAccess = "${app2ViewProperties.appName} 세션 관리",
                nextStep = "강제 로그아웃 패널에서 현재 앱 범위만 종료되는지 확인하세요.",
            )
            else -> AccountInsight(
                tone = "restricted",
                label = "agency-b-user",
                title = "현재 앱 전용 사용자",
                detail = "agency-b realm의 local role만 가진 계정입니다. platform-broker 인증은 성공하지만 peer 앱으로 이동하면 agency-a realm의 인가 단계에서 차단됩니다.",
                currentAppAccess = "${app2ViewProperties.appName} 접근 가능",
                peerAppAccess = "${app2ViewProperties.peerAppName} 403 확인 대상",
                adminAccess = "관리 권한 없음",
                nextStep = "Peer 앱으로 이동해 재로그인 없이 인증된 뒤 필요한 role 비교 화면을 확인하세요.",
            )
        }
    }

    private fun demoAccounts(): List<AccountGuide> {
        return listOf(
            AccountGuide("agency-a-user", "agencyauser1234", "App 1", "없음", "agency-a realm에서만 app1-user role을 가진 사용자"),
            AccountGuide("multi-user", "multi1234", "App 1, App 2", "없음", "두 service realm에 모두 연결된 대표 브로커 데모 계정"),
            AccountGuide("agency-b-user", "agencybuser1234", "App 2", "없음", "agency-b realm에서만 app2-user role을 가진 사용자"),
            AccountGuide("agency-a-admin", "agencyaadmin1234", "App 1", "App 1 세션 강제 로그아웃", "agency-a realm 운영 관리자"),
            AccountGuide("agency-b-admin", "agencybadmin1234", "App 2", "App 2 세션 강제 로그아웃", "agency-b realm 운영 관리자"),
            AccountGuide("master-admin", "master1234", "App 1, App 2", "전체 앱 세션 강제 로그아웃", "두 realm을 모두 관리하는 통합 IAM 관리자"),
        )
    }

    private fun ssoJourney(): List<JourneyStep> {
        return listOf(
            JourneyStep("1", "권장 계정 선택", "`multi-user`로 로그인하면 가장 자연스러운 멀티앱 데모를 볼 수 있습니다."),
            JourneyStep("2", "현재 앱 인증", "agency-b realm이 요청을 받고 `kc_idp_hint=platform-broker`로 공통 broker realm 로그인을 시작합니다."),
            JourneyStep("3", "원클릭 전환", "아래 자동 전환 버튼 또는 바로가기 버튼으로 ${app2ViewProperties.peerAppName} 인증을 이어가면 agency-a realm이 같은 broker 세션을 재사용합니다."),
            JourneyStep("4", "권한 차이 확인", "`agency-a-user`, `agency-b-user`, `agency-b-admin`으로 403과 관리자 범위를 비교해볼 수 있습니다."),
        )
    }

    private fun auditEvents(
        username: String,
        canManageCurrentApp: Boolean,
        isMasterAdmin: Boolean,
    ): List<AuditEvent> {
        return listOf(
            AuditEvent("방금", "OIDC Federation", "${app2ViewProperties.suiteName}에서 ${app2ViewProperties.clientId} 클라이언트를 준비했습니다."),
            AuditEvent("1분 전", "User Context", "`$username` 기준 접근 컨텍스트와 앱 간 role 매핑을 계산했습니다."),
            AuditEvent("3분 전", "Session Index", "Redis indexed session 저장소에서 현재 앱 세션을 조회할 준비가 완료되었습니다."),
            AuditEvent(
                "5분 전",
                "Admin Scope",
                when {
                    isMasterAdmin -> "현재 계정은 모든 앱 세션 강제 로그아웃 권한을 보유합니다."
                    canManageCurrentApp -> "현재 계정은 ${app2ViewProperties.appName} 범위 세션만 강제 종료할 수 있습니다."
                    else -> "현재 계정은 읽기 전용 콘솔 사용자로 동작합니다."
                },
            ),
        )
    }

    data class AccountGuide(
        val username: String,
        val password: String,
        val accessApps: String,
        val adminScope: String,
        val summary: String,
    )

    data class JourneyStep(
        val step: String,
        val title: String,
        val detail: String,
    )

    data class AuditEvent(
        val timestamp: String,
        val title: String,
        val detail: String,
    )

    data class AccountInsight(
        val tone: String,
        val label: String,
        val title: String,
        val detail: String,
        val currentAppAccess: String,
        val peerAppAccess: String,
        val adminAccess: String,
        val nextStep: String,
    )

    private companion object {
        private const val MULTI_USER = "multi-user"
    }
}
