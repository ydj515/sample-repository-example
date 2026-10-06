package com.example.webfluxwithredisexample.presentation.router.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

import java.util.Map;

public final class UsecaseResponses {
    // Redis의 다형성 직렬화 설정을 HTTP 응답에 적용하지 않는다.
    private static final ObjectMapper HTTP_MAPPER = new ObjectMapper();

    private UsecaseResponses() {
    }

    public static Mono<ServerResponse> ok(Mono<?> value) {
        return value.flatMap(
                        body ->
                                Mono.fromCallable(() -> HTTP_MAPPER.writeValueAsString(body))
                                        .flatMap(
                                                json ->
                                                        ServerResponse.ok()
                                                                .contentType(
                                                                        MediaType.APPLICATION_JSON)
                                                                .bodyValue(json)))
                .switchIfEmpty(ServerResponse.noContent().build());
    }

    public static Mono<ServerResponse> badRequest(Throwable error, ServerRequest request) {
        return ServerResponse.status(HttpStatus.BAD_REQUEST)
                .bodyValue(Map.of("error", "Invalid request parameters or body"));
    }
}
