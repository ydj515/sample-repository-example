package com.example.webfluxwithredisexample.presentation.router.pubsub;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

@Configuration
public class PubSubRouter {

    @Bean
    public RouterFunction<ServerResponse> pubSubRoute(PubSubHandler handler) {
        return RouterFunctions
                .route(POST("/pubsub/publish"), handler::publish)
                .andRoute(GET("/pubsub/subscribe"), handler::subscribe)
                .andRoute(GET("/pubsub/psubscribe"), handler::patternSubscribe);
    }
}
