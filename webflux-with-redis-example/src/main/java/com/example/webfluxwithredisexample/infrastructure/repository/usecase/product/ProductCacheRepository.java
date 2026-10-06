package com.example.webfluxwithredisexample.infrastructure.repository.usecase.product;

import com.example.webfluxwithredisexample.domain.usecase.product.Product;
import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class ProductCacheRepository {
    private final ReactiveRedisTemplate<String, String> template;
    private final Gson gson;

    public Mono<Product> get(String id) {
        return template.opsForValue().get(key(id)).map(json -> gson.fromJson(json, Product.class));
    }

    public Mono<Boolean> put(Product product) {
        return template.opsForValue()
                .set(key(product.id()), gson.toJson(product), Duration.ofSeconds(30));
    }

    public Mono<Long> ttl(String id) {
        return template.getExpire(key(id)).map(Duration::getSeconds);
    }

    private String key(String id) {
        return "usecase:product:" + id;
    }
}
