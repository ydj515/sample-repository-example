package com.example.webfluxwithredisexample.presentation.router.strategy;

import com.example.webfluxwithredisexample.application.RedisStrategyService;
import com.example.webfluxwithredisexample.domain.StrategyUser;
import com.example.webfluxwithredisexample.domain.ValueWithTTL;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class StrategyHandler {
    private final RedisStrategyService strategyService;

    public Mono<ServerResponse> getDbUser(ServerRequest request) {
        Long userId = requiredUserId(request);
        return ServerResponse.ok().body(strategyService.getDbUser(userId), StrategyUser.class);
    }

    public Mono<ServerResponse> saveDbUser(ServerRequest request) {
        return request.bodyToMono(StrategyUserRequest.class)
                .flatMap(strategyService::saveDbUser)
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    public Mono<ServerResponse> getCacheAsideUser(ServerRequest request) {
        return ServerResponse.ok().body(strategyService.getUserByCacheAside(requiredUserId(request)), StrategyUserResponse.class);
    }

    public Mono<ServerResponse> updateCacheAsideUser(ServerRequest request) {
        return request.bodyToMono(StrategyUserRequest.class)
                .flatMap(strategyService::updateUserByCacheAside)
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    public Mono<ServerResponse> getWriteThroughUser(ServerRequest request) {
        return ServerResponse.ok().body(strategyService.getUserByWriteThrough(requiredUserId(request)), StrategyUserResponse.class);
    }

    public Mono<ServerResponse> updateWriteThroughUser(ServerRequest request) {
        return request.bodyToMono(StrategyUserRequest.class)
                .flatMap(strategyService::updateUserByWriteThrough)
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    public Mono<ServerResponse> getReadThroughUser(ServerRequest request) {
        return ServerResponse.ok().body(strategyService.getUserByReadThrough(requiredUserId(request)), StrategyUserResponse.class);
    }

    public Mono<ServerResponse> updateWriteBehindUser(ServerRequest request) {
        return request.bodyToMono(StrategyUserRequest.class)
                .flatMap(strategyService::updateUserByWriteBehind)
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    public Mono<ServerResponse> processWriteBehindQueue(ServerRequest request) {
        return ServerResponse.ok().body(strategyService.processWriteBehindQueue(), StrategyQueueProcessResponse.class);
    }

    public Mono<ServerResponse> getPerUser(ServerRequest request) {
        return ServerResponse.ok().body(strategyService.getUserByPer(requiredUserId(request)), StrategyUserResponse.class);
    }

    public Mono<ServerResponse> getLockUser(ServerRequest request) {
        return ServerResponse.ok().body(strategyService.getUserByLock(requiredUserId(request)), StrategyUserResponse.class);
    }

    public Mono<ServerResponse> refreshUsers(ServerRequest request) {
        return request.bodyToMono(StrategyRefreshRequest.class)
                .flatMap(strategyService::refreshUsers)
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    public Mono<ServerResponse> getValueWithTtl(ServerRequest request) {
        String key = request.queryParam("key")
                .orElseThrow(() -> new IllegalArgumentException("Missing query param: key"));

        Mono<ValueWithTTL<StrategyUser>> response = strategyService.getValueWithTtl(key, StrategyUser.class);
        return ServerResponse.ok().body(response, ValueWithTTL.class);
    }

    private Long requiredUserId(ServerRequest request) {
        return request.queryParam("userId")
                .map(Long::parseLong)
                .orElseThrow(() -> new IllegalArgumentException("Missing query param: userId"));
    }
}
