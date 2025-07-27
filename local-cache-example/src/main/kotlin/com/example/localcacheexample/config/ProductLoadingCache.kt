package com.example.localcacheexample.config

import com.example.localcacheexample.domain.product.extensions.toInfo
import com.example.localcacheexample.domain.product.repository.ProductRepository
import com.example.localcacheexample.domain.product.service.ProductInfo
import com.github.benmanes.caffeine.cache.CacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
class ProductLoadingCache(
    private val productRepository: ProductRepository,
) {

    companion object {
        private val log = LoggerFactory.getLogger(ProductLoadingCache::class.java)
    }

    @Bean
    fun productCache(): LoadingCache<Long, ProductInfo> {
        return Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .refreshAfterWrite(3, TimeUnit.SECONDS)
            .expireAfterAccess(5, TimeUnit.SECONDS)
            .maximumSize(10000)
            .recordStats()
            .removalListener<Long, ProductInfo> { key, value, cause ->
                log.info("캐시 제거됨 - key: {}, cause: {}", key, cause)
            }
            .build(object : CacheLoader<Long, ProductInfo> {
                override fun load(key: Long): ProductInfo {
                    log.info("LOAD - DB 조회 productId={}", key)
                    return findProductInfo(key)
                }

                override fun reload(key: Long, oldValue: ProductInfo): ProductInfo {
                    log.info("REFRESH - DB 갱신 productId={}", key)
                    return findProductInfo(key)
                }
            })
    }

    private fun findProductInfo(id: Long): ProductInfo {
        val product = productRepository.getProduct(id)
            ?: throw IllegalArgumentException("Product with id $id not found")
        return product.toInfo()
    }
}