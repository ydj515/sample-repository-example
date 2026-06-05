package com.example.webfluxwithredisexample.presentation.router.pubsub;

import com.example.webfluxwithredisexample.application.PubSubAsyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class PubSubHandler {
    private final PubSubAsyncService pubSubAsyncService;

    public Mono<ServerResponse> publish(ServerRequest request) {
        return request.bodyToMono(PubSubPublishRequest.class)
                .flatMap(pubSubAsyncService::publish)
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    public Mono<ServerResponse> subscribe(ServerRequest request) {
        String channel = request.queryParam("channel")
                .orElseThrow(() -> new IllegalArgumentException("Missing query param: channel"));

        Flux<ServerSentEvent<PubSubMessageResponse>> response = pubSubAsyncService.subscribe(channel)
                .map(message -> ServerSentEvent.builder(message)
                        .event("message")
                        .build());

        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(response, ServerSentEvent.class);
    }

    public Mono<ServerResponse> patternSubscribe(ServerRequest request) {
        String pattern = request.queryParam("pattern")
                .orElseThrow(() -> new IllegalArgumentException("Missing query param: pattern"));

        Flux<ServerSentEvent<PubSubMessageResponse>> response = pubSubAsyncService.psubscribe(pattern)
                .map(message -> ServerSentEvent.builder(message)
                        .event("pmessage")
                        .build());

        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(response, ServerSentEvent.class);
    }
}
