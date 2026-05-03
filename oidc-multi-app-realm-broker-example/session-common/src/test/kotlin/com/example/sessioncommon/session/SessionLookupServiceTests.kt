package com.example.sessioncommon.session

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.session.MapSession
import java.time.Duration

class SessionLookupServiceTests {

    @Test
    fun `세션 재검증 결과를 ttl 동안 캐시한다`() {
        val repository = InMemoryIndexedSessionRepository()
        val session = MapSession().apply {
            setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextImpl(UsernamePasswordAuthenticationToken("alice", "password")),
            )
            setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, "alice")
            setAttribute(SessionAttributeNames.APP_ID, "app1")
        }
        repository.save(session)

        val service = SessionLookupService(repository)

        assertTrue(service.revalidateSession(session.id, "alice", Duration.ofSeconds(5)))
        assertTrue(service.revalidateSession(session.id, "alice", Duration.ofSeconds(5)))
        assertEquals(1, repository.findByIdCallCount)
    }

    @Test
    fun `사용자 세션 무효화 시 저장소와 캐시가 함께 정리된다`() {
        val repository = InMemoryIndexedSessionRepository()
        val aliceSession = createSession("alice", "app1")
        val bobSession = createSession("bob", "app2")
        repository.save(aliceSession)
        repository.save(bobSession)

        val service = SessionLookupService(repository)

        val invalidatedCount = service.invalidateUserSessions("alice")

        assertEquals(1, invalidatedCount)
        assertFalse(repository.store.containsKey(aliceSession.id))
        assertTrue(repository.store.containsKey(bobSession.id))
    }

    @Test
    fun `앱별 관리자 세션 무효화는 해당 앱 세션만 제거한다`() {
        val repository = InMemoryIndexedSessionRepository()
        val app1Session = createSession("multi-user", "app1")
        val app2Session = createSession("multi-user", "app2")
        repository.save(app1Session)
        repository.save(app2Session)

        val service = SessionLookupService(repository)

        val invalidatedCount = service.invalidateUserSessions("multi-user", "app1")

        assertEquals(1, invalidatedCount)
        assertFalse(repository.store.containsKey(app1Session.id))
        assertTrue(repository.store.containsKey(app2Session.id))
    }

    private fun createSession(
        username: String,
        appId: String,
    ): MapSession {
        return MapSession().apply {
            setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextImpl(UsernamePasswordAuthenticationToken(username, "password")),
            )
            setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, username)
            setAttribute(SessionAttributeNames.APP_ID, appId)
        }
    }

    private class InMemoryIndexedSessionRepository : FindByIndexNameSessionRepository<MapSession> {
        val store = linkedMapOf<String, MapSession>()
        var findByIdCallCount: Int = 0

        override fun createSession(): MapSession = MapSession()

        override fun save(session: MapSession) {
            store[session.id] = MapSession(session)
        }

        override fun findById(id: String): MapSession? {
            findByIdCallCount += 1
            return store[id]?.let(::MapSession)
        }

        override fun deleteById(id: String) {
            store.remove(id)
        }

        override fun findByIndexNameAndIndexValue(
            indexName: String,
            indexValue: String,
        ): MutableMap<String, MapSession> {
            return store.values
                .filter { session -> session.getAttribute<String>(indexName) == indexValue }
                .associateByTo(linkedMapOf()) { it.id }
        }
    }
}
