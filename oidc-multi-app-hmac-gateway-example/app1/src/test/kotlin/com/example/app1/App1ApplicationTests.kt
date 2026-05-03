package com.example.app1

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.session.MapSession

@SpringBootTest(
    properties = [
        "app.session.redis-enabled=false",
        "app.session.app-id=app1",
        "app.security.end-session-uri=http://localhost:9000/realms/oidc-multi-app-example/protocol/openid-connect/logout",
        "app.security.access.user-roles[0]=app1-user",
        "app.security.access.admin-roles[0]=app1-admin",
        "app.security.access.master-admin-role=master-admin",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.security.oauth2.client.provider.keycloak.authorization-uri=http://localhost:9000/auth",
        "spring.security.oauth2.client.provider.keycloak.token-uri=http://localhost:9000/token",
        "spring.security.oauth2.client.provider.keycloak.user-info-uri=http://localhost:9000/userinfo",
        "spring.security.oauth2.client.provider.keycloak.jwk-set-uri=http://localhost:9000/certs",
        "spring.security.oauth2.client.provider.keycloak.user-name-attribute=preferred_username",
        "spring.security.oauth2.client.registration.keycloak.provider=keycloak",
    ],
)
@Import(App1ApplicationTests.TestSessionRepositoryConfig::class)
class App1ApplicationTests {

    @Test
    fun contextLoads() {
    }

    @TestConfiguration
    class TestSessionRepositoryConfig {
        @Bean
        fun sessionRepository(): FindByIndexNameSessionRepository<MapSession> {
            return object : FindByIndexNameSessionRepository<MapSession> {
                override fun createSession(): MapSession = MapSession()

                override fun save(session: MapSession) {
                }

                override fun findById(id: String): MapSession? = null

                override fun deleteById(id: String) {
                }

                override fun findByIndexNameAndIndexValue(
                    indexName: String,
                    indexValue: String,
                ): MutableMap<String, MapSession> = linkedMapOf()
            }
        }
    }
}
