package com.example.webfluxwithredisexample.presentation.router.strategy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.PUT;

@Configuration
public class StrategyRouter {

    @Bean
    public RouterFunction<ServerResponse> strategyRoute(StrategyHandler handler) {
        return RouterFunctions
                .route(GET("/strategy/db/user"), handler::getDbUser)
                .andRoute(PUT("/strategy/db/user"), handler::saveDbUser)
                .andRoute(GET("/strategy/cache-aside/user"), handler::getCacheAsideUser)
                .andRoute(PUT("/strategy/cache-aside/user"), handler::updateCacheAsideUser)
                .andRoute(GET("/strategy/write-through/user"), handler::getWriteThroughUser)
                .andRoute(PUT("/strategy/write-through/user"), handler::updateWriteThroughUser)
                .andRoute(GET("/strategy/read-through/user"), handler::getReadThroughUser)
                .andRoute(PUT("/strategy/write-behind/user"), handler::updateWriteBehindUser)
                .andRoute(POST("/strategy/write-behind/process-next"), handler::processWriteBehindQueue)
                .andRoute(GET("/strategy/per/user"), handler::getPerUser)
                .andRoute(GET("/strategy/lock/user"), handler::getLockUser)
                .andRoute(POST("/strategy/background-refresh"), handler::refreshUsers)
                .andRoute(GET("/strategy/cache/value-with-ttl"), handler::getValueWithTtl);
    }
}
