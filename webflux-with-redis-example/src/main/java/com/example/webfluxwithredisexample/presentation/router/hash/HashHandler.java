package com.example.webfluxwithredisexample.presentation.router.hash;

import com.example.webfluxwithredisexample.application.HashAsyncService;
import com.example.webfluxwithredisexample.domain.HashModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HashHandler {
    private final HashAsyncService hashAsyncService;

    public Mono<ServerResponse> putHashes(ServerRequest request) {
        return request.bodyToMono(HashReqeust.class)
                .flatMap(hashAsyncService::putInHash)
                .flatMap(result -> ServerResponse.ok().body(Mono.just("Hash value saved: " + result), String.class));
    }

    public Mono<ServerResponse> getHashes(ServerRequest request) {
        String key = request.queryParam("key").orElse("");
        String field = request.queryParam("field").orElse("");
        return hashAsyncService.getFromHash(key, field, HashModel.class)
                .flatMap(data -> ServerResponse.ok().body(Mono.just(data), HashModel.class));
    }

    public Mono<ServerResponse> getAllHashes(ServerRequest request) {
        String key = request.queryParam("key").orElse("");
        return ServerResponse.ok().body(hashAsyncService.getEntries(key), java.util.Map.class);
    }

    public Mono<ServerResponse> multiGetHashes(ServerRequest request) {
        String key = request.queryParam("key").orElse("");
        List<String> fields = request.queryParam("fields")
                .map(value -> Arrays.stream(value.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toList())
                .orElse(List.of());

        return ServerResponse.ok().body(hashAsyncService.multiGet(key, fields), List.class);
    }

    public Mono<ServerResponse> existsHashField(ServerRequest request) {
        String key = request.queryParam("key").orElse("");
        String field = request.queryParam("field").orElse("");
        return ServerResponse.ok().body(hashAsyncService.exists(key, field), Boolean.class);
    }

    public Mono<ServerResponse> getHashKeys(ServerRequest request) {
        String key = request.queryParam("key").orElse("");
        return ServerResponse.ok().body(hashAsyncService.getKeys(key), List.class);
    }

    public Mono<ServerResponse> getHashValues(ServerRequest request) {
        String key = request.queryParam("key").orElse("");
        return ServerResponse.ok().body(hashAsyncService.getValues(key), List.class);
    }
}
