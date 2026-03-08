package com.example.oidcsimpleexample.web

import com.example.oidcsimpleexample.security.ApiSecurityLevel
import com.example.oidcsimpleexample.security.ApiSecurityTier
import com.example.oidcsimpleexample.service.SessionLookupService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.access.prepost.PreAuthorize
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
class OidcApiController(
    private val sessionLookupService: SessionLookupService,
) {

    @GetMapping("/me")
    @ApiSecurityTier(ApiSecurityLevel.P2_STANDARD)
    fun me(
        @AuthenticationPrincipal user: OidcUser,
        request: HttpServletRequest,
    ): UserInfoResponse {
        return UserInfoResponse(
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
            message = "민감 API 예시입니다. 인터셉터가 짧은 TTL로 세션 재검증을 수행합니다.",
            principal = user.preferredUsername ?: user.subject,
            sessionId = request.getSession(false)?.id,
            checkedAt = Instant.now(),
        )
    }

    @PostMapping("/admin/users/{username}/logout-all")
    @PreAuthorize("hasRole('admin')")
    @ApiSecurityTier(ApiSecurityLevel.P0_CRITICAL)
    fun logoutAllSessions(
        @PathVariable username: String,
    ): LogoutResultResponse {
        val invalidated = sessionLookupService.invalidateUserSessions(username)
        return LogoutResultResponse(
            username = username,
            invalidatedSessions = invalidated,
        )
    }

    data class UserInfoResponse(
        val username: String,
        val subject: String,
        val email: String?,
        val roles: List<String>,
        val sessionId: String?,
        val issuedAt: Instant?,
        val expiresAt: Instant?,
    )

    data class SensitiveResponse(
        val message: String,
        val principal: String,
        val sessionId: String?,
        val checkedAt: Instant,
    )

    data class LogoutResultResponse(
        val username: String,
        val invalidatedSessions: Int,
    )
}
