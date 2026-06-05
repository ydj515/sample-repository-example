package com.example.webfluxwithredisexample.infrastructure.repository;

import com.example.webfluxwithredisexample.presentation.router.pubsub.PubSubMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Repository
public class PubSubRepository {
    private final ReactiveRedisTemplate<String, String> template;
    private final ReactiveRedisMessageListenerContainer listenerContainer;

    public Mono<Long> publish(String channel, String message) {
        return template.convertAndSend(channel, message);
    }

    public Flux<PubSubMessageResponse> subscribe(String channel) {
        return listenerContainer.receive(new ChannelTopic(channel))
                .map(message -> new PubSubMessageResponse(message.getChannel(), message.getMessage(), null));
    }

    public Flux<PubSubMessageResponse> psubscribe(String pattern) {
        return listenerContainer.receive(new PatternTopic(pattern))
                .map(message -> new PubSubMessageResponse(message.getChannel(), message.getMessage(), message.getPattern()));
    }
}
