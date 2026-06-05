package com.example.webfluxwithredisexample.presentation.router.stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

@Configuration
public class StreamRouter {

    @Bean
    public RouterFunction<ServerResponse> streamRoute(StreamHandler handler) {
        return RouterFunctions
                .route(POST("/stream/add"), handler::add)
                .andRoute(GET("/stream/length"), handler::size)
                .andRoute(GET("/stream/read"), handler::read)
                .andRoute(POST("/stream/group"), handler::createGroup)
                .andRoute(GET("/stream/group/read"), handler::readGroup)
                .andRoute(POST("/stream/group/ack"), handler::acknowledge);
    }
}
