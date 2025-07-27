package com.example.localcacheexample.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {

    companion object {
        private val log = LoggerFactory.getLogger(CacheConfig::class.java)
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    // refreshAfterWrite는 LoadingCache으로 사용해야 사용 가능
    // refreshAfterWrite(1, TimeUnit.MINUTES) // 1분마다 refresh
    @Bean
    fun caffeineConfig(): Caffeine<Any, Any> {
        return Caffeine.newBuilder()
            .expireAfterWrite(3, TimeUnit.MINUTES) // 3분 후 만료
            .expireAfterAccess(5, TimeUnit.MINUTES) // 5분 미사용 시 만료
            .maximumSize(1000000)
            .recordStats()
            .removalListener { key, value, cause ->
                val now = LocalDateTime.now().format(DATE_TIME_FORMATTER)
                log.info("[{}] 캐시 제거됨 - key: {}, cause: {}", now, key, cause)
            }
    }

    @Bean
    fun cacheManager(caffeine: Caffeine<Any, Any>): CacheManager {
        return CaffeineCacheManager().apply {
            setCaffeine(caffeine)
            isAllowNullValues = false // 캐시에 null 안 넣게 설정 (선택)
        }
    }
}