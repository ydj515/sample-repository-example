package com.example.webfluxwithredisexample.presentation.router.stream;

import com.example.webfluxwithredisexample.application.StreamAsyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class StreamHandler {
    private final StreamAsyncService streamAsyncService;

    public Mono<ServerResponse> add(ServerRequest request) {
        return request.bodyToMono(StreamAddRequest.class)
                .flatMap(streamAsyncService::add)
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    public Mono<ServerResponse> size(ServerRequest request) {
        String key = requiredQueryParam(request, "key");
        return ServerResponse.ok().body(streamAsyncService.size(key), Long.class);
    }

    public Mono<ServerResponse> read(ServerRequest request) {
        String key = requiredQueryParam(request, "key");
        String offset = request.queryParam("offset").orElse("0-0");
        long count = Long.parseLong(request.queryParam("count").orElse("10"));

        Flux<StreamEntryResponse> response = streamAsyncService.read(key, offset, count);
        return ServerResponse.ok().body(response, StreamEntryResponse.class);
    }

    public Mono<ServerResponse> createGroup(ServerRequest request) {
        return request.bodyToMono(StreamGroupRequest.class)
                .flatMap(streamAsyncService::createGroup)
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    public Mono<ServerResponse> readGroup(ServerRequest request) {
        String key = requiredQueryParam(request, "key");
        String group = requiredQueryParam(request, "group");
        String consumer = requiredQueryParam(request, "consumer");
        long count = Long.parseLong(request.queryParam("count").orElse("10"));

        return ServerResponse.ok().body(
                streamAsyncService.readGroup(key, group, consumer, count),
                StreamEntryResponse.class
        );
    }

    public Mono<ServerResponse> acknowledge(ServerRequest request) {
        return request.bodyToMono(StreamAckRequest.class)
                .flatMap(streamAsyncService::acknowledge)
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    private String requiredQueryParam(ServerRequest request, String name) {
        return request.queryParam(name)
                .orElseThrow(() -> new IllegalArgumentException("Missing query param: " + name));
    }
}
