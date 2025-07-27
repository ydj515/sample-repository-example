package com.example.webfluxwithmongoexample.presentation.router.user;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

@Configuration
public class UserRouter {

    @Bean
    public RouterFunction<ServerResponse> userRoutes(UserHandler userHandler) {
        return RouterFunctions.route(POST("/users"), userHandler::createUser)
                .andRoute(GET("/users/{id}"), userHandler::getUserById)
                .andRoute(GET("/users"), userHandler::getAllUsers)
                ;
    }
}
