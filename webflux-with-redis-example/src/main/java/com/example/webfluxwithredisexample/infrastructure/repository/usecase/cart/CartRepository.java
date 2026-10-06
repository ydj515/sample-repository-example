package com.example.webfluxwithredisexample.infrastructure.repository.usecase.cart;

import com.example.webfluxwithredisexample.domain.usecase.cart.CartItem;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CartRepository {
    private final ReactiveRedisTemplate<String, String> template;
    private static final RedisScript<Long> UPDATE = RedisScript.of(
            new ClassPathResource("lua/usecase/cart/update-and-renew.lua"), Long.class
    );
    private static final RedisScript<List> READ = RedisScript.of(
            new ClassPathResource("lua/usecase/cart/read-and-renew.lua"), List.class
    );

    public Mono<Long> put(String session, CartItem item) {
        return template.execute(
                        UPDATE,
                        List.of(key(session)),
                        item.productId(),
                        Integer.toString(item.quantity()),
                        "1800")
                .single();
    }

    public Mono<List<CartItem>> get(String session) {
        return template.execute(READ, List.of(key(session)), "1800")
                .single()
                .map(
                        values -> {
                            ArrayList<CartItem> items = new ArrayList<>();
                            for (int i = 0; i < values.size(); i += 2) {
                                items.add(
                                        new CartItem(
                                                values.get(i).toString(),
                                                Integer.parseInt(values.get(i + 1).toString())));
                            }
                            return List.copyOf(items);
                        });
    }

    private String key(String session) {
        return "usecase:cart:" + session;
    }
}
