package com.example.oidcsimpleexample.service

import org.springframework.security.core.context.SecurityContext
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.session.Session
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class SessionLookupService(
    private val sessionRepository: FindByIndexNameSessionRepository<out Session>,
) {

    private val cache = ConcurrentHashMap<String, CachedValidation>()

    fun revalidateSession(
        sessionId: String,
        principalName: String,
        cacheTtl: Duration,
    ): Boolean {
        val now = Instant.now()
        val cached = cache[sessionId]
        if (cached != null &&
            cached.principalName == principalName &&
            cached.validUntil.isAfter(now)
        ) {
            return cached.valid
        }

        val valid = lookupSession(sessionId, principalName)
        val validUntil = now.plus(cacheTtl)
        cache[sessionId] = CachedValidation(
            principalName = principalName,
            valid = valid,
            validUntil = validUntil,
        )
        return valid
    }

    fun invalidateUserSessions(principalName: String): Int {
        val sessions = sessionRepository.findByPrincipalName(principalName)
        sessions.keys.forEach { sessionId ->
            sessionRepository.deleteById(sessionId)
            cache.remove(sessionId)
        }
        return sessions.size
    }

    fun findUserSessions(principalName: String): List<SessionSummary> {
        return sessionRepository
            .findByPrincipalName(principalName)
            .map { (sessionId, session) ->
                SessionSummary(
                    sessionId = sessionId,
                    principalName = principalName,
                    creationTime = session.creationTime,
                    lastAccessedTime = session.lastAccessedTime,
                    maxInactiveInterval = session.maxInactiveInterval,
                )
            }
            .sortedByDescending { it.lastAccessedTime }
    }

    private fun lookupSession(
        sessionId: String,
        principalName: String,
    ): Boolean {
        val session = sessionRepository.findById(sessionId) ?: return false
        val securityContext = session.getAttribute<SecurityContext>(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
        ) ?: return false

        return securityContext.authentication?.name == principalName
    }

    data class SessionSummary(
        val sessionId: String,
        val principalName: String,
        val creationTime: Instant,
        val lastAccessedTime: Instant,
        val maxInactiveInterval: Duration,
    )

    private data class CachedValidation(
        val principalName: String,
        val valid: Boolean,
        val validUntil: Instant,
    )
}
