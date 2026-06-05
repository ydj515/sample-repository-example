package com.example.webfluxwithredisexample.presentation.router.list;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

@Configuration
public class ListRouter {

    @Bean
    public RouterFunction<ServerResponse> listRoute(ListHandler handler) {
        return RouterFunctions
                .route(POST("/list-add-left"), handler::addToListLeft)
                .andRoute(POST("/list-add-right"), handler::addToListRight)
                .andRoute(GET("/all"), handler::getAllData)
                .andRoute(GET("/list/size"), handler::getListSize)
                .andRoute(GET("/list/index"), handler::getListIndex)
                .andRoute(POST("/list/left-pop"), handler::leftPop)
                .andRoute(POST("/list/right-pop"), handler::rightPop)
                .andRoute(POST("/list/blocking-left-pop"), handler::blockingLeftPop);
    }
}
