package com.example.app2.web

import com.example.app2.config.App2ViewProperties
import com.example.oidccommon.config.OidcSecurityProperties
import com.example.sessioncommon.config.SessionPolicyProperties
import com.example.sessioncommon.security.ApiSecurityLevel
import com.example.sessioncommon.security.ApiSecurityTier
import com.example.sessioncommon.session.SessionLookupService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api")
class App2ApiController(
    private val sessionLookupService: SessionLookupService,
    private val app2ViewProperties: App2ViewProperties,
    private val oidcSecurityProperties: OidcSecurityProperties,
    private val sessionPolicyProperties: SessionPolicyProperties,
) {

    @GetMapping("/me")
    @ApiSecurityTier(ApiSecurityLevel.P2_STANDARD)
    fun me(
        @AuthenticationPrincipal user: OidcUser,
        request: HttpServletRequest,
    ): UserInfoResponse {
        return UserInfoResponse(
            app = app2ViewProperties.appName,
            clientId = app2ViewProperties.clientId,
            username = user.preferredUsername ?: user.subject,
            subject = user.subject,
            email = user.email,
            roles = user.authorities.map { it.authority }.sorted(),
            sessionId = request.getSession(false)?.id,
            issuedAt = user.idToken.issuedAt,
            expiresAt = user.idToken.expiresAt,
        )
    }

    @GetMapping("/sensitive")
    @ApiSecurityTier(ApiSecurityLevel.P1_SENSITIVE)
    fun sensitive(
        @AuthenticationPrincipal user: OidcUser,
        request: HttpServletRequest,
    ): SensitiveResponse {
        return SensitiveResponse(
            app = app2ViewProperties.appName,
            message = "App2 민감 API입니다. App1와 같은 공통 보안 모듈을 재사용합니다.",
            principal = user.preferredUsername ?: user.subject,
            sessionId = request.getSession(false)?.id,
            checkedAt = Instant.now(),
        )
    }

    @PostMapping("/admin/users/{username}/logout-all")
    @ApiSecurityTier(ApiSecurityLevel.P0_CRITICAL)
    fun logoutAllSessions(
        @PathVariable username: String,
        authentication: Authentication,
    ): LogoutResultResponse {
        val masterAdmin = oidcSecurityProperties.isMasterAdmin(authentication.authorities)
        val invalidated = sessionLookupService.invalidateUserSessions(
            principalName = username,
            appId = if (masterAdmin) null else sessionPolicyProperties.appId,
        )
        return LogoutResultResponse(
            app = app2ViewProperties.appName,
            username = username,
            scope = if (masterAdmin) "all-apps" else sessionPolicyProperties.appId,
            invalidatedSessions = invalidated,
        )
    }

    data class UserInfoResponse(
        val app: String,
        val clientId: String,
        val username: String,
        val subject: String,
        val email: String?,
        val roles: List<String>,
        val sessionId: String?,
        val issuedAt: Instant?,
        val expiresAt: Instant?,
    )

    data class SensitiveResponse(
        val app: String,
        val message: String,
        val principal: String,
        val sessionId: String?,
        val checkedAt: Instant,
    )

    data class LogoutResultResponse(
        val app: String,
        val username: String,
        val scope: String,
        val invalidatedSessions: Int,
    )
}
