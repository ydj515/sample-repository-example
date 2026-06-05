package com.example.webfluxwithredisexample.application;

import com.example.webfluxwithredisexample.infrastructure.repository.PubSubRepository;
import com.example.webfluxwithredisexample.presentation.router.pubsub.PubSubMessageResponse;
import com.example.webfluxwithredisexample.presentation.router.pubsub.PubSubPublishRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PubSubAsyncService {
    private final PubSubRepository redis;

    public Mono<Long> publish(PubSubPublishRequest req) {
        return redis.publish(req.channel(), req.message());
    }

    public Flux<PubSubMessageResponse> subscribe(String channel) {
        return redis.subscribe(channel);
    }

    public Flux<PubSubMessageResponse> psubscribe(String pattern) {
        return redis.psubscribe(pattern);
    }
}
