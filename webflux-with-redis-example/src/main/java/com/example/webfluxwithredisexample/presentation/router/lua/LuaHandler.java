package com.example.webfluxwithredisexample.presentation.router.lua;

import com.example.webfluxwithredisexample.application.LuaAsyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class LuaHandler {
    private final LuaAsyncService luaAsyncService;

    public Mono<ServerResponse> decreaseStock(ServerRequest request) {
        return request.bodyToMono(LuaStockRequest.class)
                .flatMap(luaAsyncService::decreaseStock)
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    public Mono<ServerResponse> loadScript(ServerRequest request) {
        return ServerResponse.ok().body(luaAsyncService.loadScript(), String.class);
    }

    public Mono<ServerResponse> decreaseStockBySha(ServerRequest request) {
        return request.bodyToMono(LuaEvalShaRequest.class)
                .flatMap(luaAsyncService::decreaseStockBySha)
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }
}
