package com.example.webfluxwithredisexample.presentation.router.list;

import com.example.webfluxwithredisexample.application.ListAsyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListHandler {
    private final ListAsyncService listAsyncService;

    public Mono<ServerResponse> addToListLeft(ServerRequest request) {
        return request.bodyToMono(ListRequest.class)
                .flatMap(listAsyncService::addToListLeft)
                .flatMap(result -> ServerResponse.ok().body(Mono.just("Added to the left: " + result), String.class))
                .onErrorResume(e -> ServerResponse.badRequest().body(Mono.just("Error adding to list left: " + e.getMessage()), String.class));
    }

    public Mono<ServerResponse> addToListRight(ServerRequest request) {
        return request.bodyToMono(ListRequest.class)
                .flatMap(listAsyncService::addToListRight)
                .flatMap(result -> ServerResponse.ok().body(Mono.just("Added to the right: " + result), String.class));
    }

    public Mono<ServerResponse> getAllData(ServerRequest request) {
        String key = request.queryParam("key").orElse("");
        return listAsyncService.getAllList(key, String.class)
                .collectList()
                .flatMap(data -> ServerResponse.ok().body(Mono.just(data), List.class));
    }

    public Mono<ServerResponse> getListSize(ServerRequest request) {
        String key = request.queryParam("key").orElse("");
        return ServerResponse.ok().body(listAsyncService.size(key), Long.class);
    }

    public Mono<ServerResponse> getListIndex(ServerRequest request) {
        String key = request.queryParam("key").orElse("");
        long index = Long.parseLong(request.queryParam("index").orElse("0"));
        return ServerResponse.ok().body(listAsyncService.index(key, index), String.class);
    }

    public Mono<ServerResponse> leftPop(ServerRequest request) {
        String key = request.queryParam("key").orElse("");
        long count = Long.parseLong(request.queryParam("count").orElse("1"));

        if (count <= 1) {
            return ServerResponse.ok().body(listAsyncService.leftPop(key), String.class);
        }

        return ServerResponse.ok().body(listAsyncService.leftPop(key, count), List.class);
    }

    public Mono<ServerResponse> rightPop(ServerRequest request) {
        String key = request.queryParam("key").orElse("");
        long count = Long.parseLong(request.queryParam("count").orElse("1"));

        if (count <= 1) {
            return ServerResponse.ok().body(listAsyncService.rightPop(key), String.class);
        }

        return ServerResponse.ok().body(listAsyncService.rightPop(key, count), List.class);
    }

    public Mono<ServerResponse> blockingLeftPop(ServerRequest request) {
        String key = request.queryParam("key").orElse("");
        long timeoutSeconds = Long.parseLong(request.queryParam("timeoutSeconds").orElse("5"));
        return ServerResponse.ok().body(listAsyncService.leftPopBlocking(key, timeoutSeconds), String.class);
    }
}
