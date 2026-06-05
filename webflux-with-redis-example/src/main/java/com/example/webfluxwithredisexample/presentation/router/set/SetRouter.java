package com.example.webfluxwithredisexample.presentation.router.set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

@Configuration
public class SetRouter {

    @Bean
    public RouterFunction<ServerResponse> setRoute(SetHandler handler) {
        return RouterFunctions
                .route(POST("/set/add"), handler::addMembers)
                .andRoute(POST("/set/remove"), handler::removeMembers)
                .andRoute(GET("/set/members"), handler::getMembers)
                .andRoute(GET("/set/is-member"), handler::isMember)
                .andRoute(GET("/set/size"), handler::size)
                .andRoute(POST("/set/pop"), handler::popMembers)
                .andRoute(GET("/set/random-members"), handler::randomMembers)
                .andRoute(GET("/set/intersection"), handler::intersect)
                .andRoute(GET("/set/union"), handler::union)
                .andRoute(GET("/set/difference"), handler::difference);
    }
}
