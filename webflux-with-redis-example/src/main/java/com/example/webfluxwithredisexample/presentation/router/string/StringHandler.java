package com.example.webfluxwithredisexample.presentation.router.string;

import com.example.webfluxwithredisexample.application.StringAsyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StringHandler {
    private final StringAsyncService stringService;

    public Mono<ServerResponse> setString(ServerRequest request) {
        return request.bodyToMono(StringRequest.class)
                .flatMap(stringService::set)
                .then(ServerResponse.ok().build());
    }

    public Mono<ServerResponse> getString(ServerRequest request) {
        String key = request.queryParam("key")
                .orElseThrow(() -> new IllegalArgumentException("Missing query param: key"));
        Mono<StringResponse> response = stringService.get(key);
        return ServerResponse.ok().body(response, StringResponse.class);
    }

    public Mono<ServerResponse> multiSetString(ServerRequest request) {
        return request.bodyToMono(MultiStringRequest.class)
                .flatMap(stringService::multiSet)
                .then(ServerResponse.ok().build());
    }

    public Mono<ServerResponse> setRawString(ServerRequest request) {
        return request.bodyToMono(RawStringRequest.class)
                .flatMap(stringService::setRaw)
                .then(ServerResponse.ok().build());
    }

    public Mono<ServerResponse> getRawString(ServerRequest request) {
        String key = request.queryParam("key")
                .orElseThrow(() -> new IllegalArgumentException("Missing query param: key"));
        return ServerResponse.ok().body(stringService.getRaw(key), String.class);
    }

    public Mono<ServerResponse> multiGetStrings(ServerRequest request) {
        List<String> keys = request.queryParam("keys")
                .map(value -> Arrays.stream(value.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toList())
                .orElseThrow(() -> new IllegalArgumentException("Missing query param: keys"));

        return ServerResponse.ok().body(stringService.multiGet(keys), List.class);
    }

    public Mono<ServerResponse> increment(ServerRequest request) {
        return request.bodyToMono(StringDeltaRequest.class)
                .flatMap(stringService::increment)
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    public Mono<ServerResponse> decrement(ServerRequest request) {
        return request.bodyToMono(StringDeltaRequest.class)
                .flatMap(stringService::decrement)
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    public Mono<ServerResponse> getTtl(ServerRequest request) {
        String key = request.queryParam("key")
                .orElseThrow(() -> new IllegalArgumentException("Missing query param: key"));
        return ServerResponse.ok().body(stringService.ttl(key), java.time.Duration.class);
    }

    public Mono<ServerResponse> setIfAbsent(ServerRequest request) {
        return request.bodyToMono(RawStringRequest.class)
                .flatMap(stringService::setIfAbsent)
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }
}
