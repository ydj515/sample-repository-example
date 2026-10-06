package com.example.webfluxwithredisexample.infrastructure.repository.usecase.product;

import com.example.webfluxwithredisexample.domain.usecase.product.Product;

import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class ProductFakeDbRepository {
    private final Map<String, Product> products = Map.of(
            "1", new Product("1", "Keyboard", 50000),
            "2", new Product("2", "Mouse", 30000)
    );

    private final AtomicLong reads = new AtomicLong();

    public Mono<Product> findById(String id) {
        return Mono.defer(
                () -> {
                    reads.incrementAndGet();
                    return Mono.justOrEmpty(products.get(id)).delayElement(Duration.ofMillis(200));
                });
    }

    public long readCount() {
        return reads.get();
    }
}
