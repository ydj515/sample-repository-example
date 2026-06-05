package com.example.webfluxwithredisexample.presentation.router.set;

import com.example.webfluxwithredisexample.application.SetAsyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SetHandler {
    private final SetAsyncService setAsyncService;

    public Mono<ServerResponse> addMembers(ServerRequest request) {
        return request.bodyToMono(SetMembersRequest.class)
                .flatMap(setAsyncService::add)
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    public Mono<ServerResponse> removeMembers(ServerRequest request) {
        return request.bodyToMono(SetMembersRequest.class)
                .flatMap(setAsyncService::remove)
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    public Mono<ServerResponse> getMembers(ServerRequest request) {
        String key = requiredQueryParam(request, "key");
        return ServerResponse.ok().body(setAsyncService.members(key), List.class);
    }

    public Mono<ServerResponse> isMember(ServerRequest request) {
        String key = requiredQueryParam(request, "key");
        String member = requiredQueryParam(request, "member");
        return ServerResponse.ok().body(setAsyncService.isMember(key, member), Boolean.class);
    }

    public Mono<ServerResponse> size(ServerRequest request) {
        String key = requiredQueryParam(request, "key");
        return ServerResponse.ok().body(setAsyncService.size(key), Long.class);
    }

    public Mono<ServerResponse> popMembers(ServerRequest request) {
        String key = requiredQueryParam(request, "key");
        long count = Long.parseLong(request.queryParam("count").orElse("1"));
        return ServerResponse.ok().body(setAsyncService.pop(key, count), List.class);
    }

    public Mono<ServerResponse> randomMembers(ServerRequest request) {
        String key = requiredQueryParam(request, "key");
        long count = Long.parseLong(request.queryParam("count").orElse("1"));
        boolean distinct = Boolean.parseBoolean(request.queryParam("distinct").orElse("true"));
        return ServerResponse.ok().body(setAsyncService.randomMembers(key, count, distinct), List.class);
    }

    public Mono<ServerResponse> intersect(ServerRequest request) {
        return ServerResponse.ok().body(setAsyncService.intersect(parseKeys(request)), List.class);
    }

    public Mono<ServerResponse> union(ServerRequest request) {
        return ServerResponse.ok().body(setAsyncService.union(parseKeys(request)), List.class);
    }

    public Mono<ServerResponse> difference(ServerRequest request) {
        return ServerResponse.ok().body(setAsyncService.difference(parseKeys(request)), List.class);
    }

    private List<String> parseKeys(ServerRequest request) {
        return request.queryParam("keys")
                .map(value -> Arrays.stream(value.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toList())
                .orElseThrow(() -> new IllegalArgumentException("Missing query param: keys"));
    }

    private String requiredQueryParam(ServerRequest request, String name) {
        return request.queryParam(name)
                .orElseThrow(() -> new IllegalArgumentException("Missing query param: " + name));
    }
}
