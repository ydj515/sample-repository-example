package com.example.webfluxwithredisexample.infrastructure.repository.usecase.orderevent;

import com.example.webfluxwithredisexample.domain.usecase.orderevent.OrderEvent;
import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.*;

@Repository
@RequiredArgsConstructor
public class OrderPubSubRepository {
    private final ReactiveRedisTemplate<String, String> template;
    private final ReactiveRedisMessageListenerContainer listener;
    private final Gson gson;

    public Mono<Long> publish(OrderEvent event) {
        return template.convertAndSend("usecase:orders:channel", gson.toJson(event));
    }

    public Flux<OrderEvent> listen() {
        return listener.receive(new ChannelTopic("usecase:orders:channel"))
                .map(message -> gson.fromJson(message.getMessage(), OrderEvent.class));
    }
}
