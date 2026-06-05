package com.example.webfluxwithredisexample.presentation.router.lua;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

@Configuration
public class LuaRouter {

    @Bean
    public RouterFunction<ServerResponse> luaRoute(LuaHandler handler) {
        return RouterFunctions
                .route(POST("/lua/stock/decrease"), handler::decreaseStock)
                .andRoute(GET("/lua/stock/script-sha"), handler::loadScript)
                .andRoute(POST("/lua/stock/decrease-by-sha"), handler::decreaseStockBySha);
    }
}
