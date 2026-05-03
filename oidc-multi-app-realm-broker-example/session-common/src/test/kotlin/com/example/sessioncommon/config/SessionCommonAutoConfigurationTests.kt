package com.example.sessioncommon.config

import com.example.sessioncommon.security.ApiSecurityTierResolver
import com.example.sessioncommon.security.SessionAppTaggingFilter
import com.example.sessioncommon.security.SessionRevalidationInterceptor
import com.example.sessioncommon.session.SessionLookupService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.session.MapSession

class SessionCommonAutoConfigurationTests {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SessionCommonAutoConfiguration::class.java))
        .withPropertyValues(
            "app.session.redis-enabled=false",
            "app.session.app-id=app1",
            "app.session.revalidation.standard-ttl=5s",
            "app.session.revalidation.sensitive-ttl=1s",
        )
        .withUserConfiguration(TestSessionRepositoryConfig::class.java)

    @Test
    fun `session-common 자동 설정이 핵심 세션 빈을 등록한다`() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(SessionPolicyProperties::class.java)
            assertThat(context).hasSingleBean(ApiSecurityTierResolver::class.java)
            assertThat(context).hasSingleBean(SessionLookupService::class.java)
            assertThat(context).hasSingleBean(SessionRevalidationInterceptor::class.java)
            assertThat(context).hasSingleBean(SessionAppTaggingFilter::class.java)
            assertThat(context).hasSingleBean(WebMvcConfig::class.java)
        }
    }

    @Test
    fun `사용자 정의 빈이 있으면 자동 설정이 기본 tier resolver를 덮어쓰지 않는다`() {
        contextRunner
            .withUserConfiguration(CustomTierResolverConfig::class.java)
            .run { context ->
                assertThat(context).hasSingleBean(ApiSecurityTierResolver::class.java)
                assertThat(context.getBean(ApiSecurityTierResolver::class.java))
                    .isSameAs(context.getBean("customApiSecurityTierResolver"))
            }
    }

    @Configuration(proxyBeanMethods = false)
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

    @Configuration(proxyBeanMethods = false)
    class CustomTierResolverConfig {
        @Bean("customApiSecurityTierResolver")
        fun apiSecurityTierResolver(): ApiSecurityTierResolver {
            return ApiSecurityTierResolver()
        }
    }
}
