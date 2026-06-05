package com.example.webfluxwithredisexample.presentation.router.pubsub;

public record PubSubMessageResponse(
        String channel,
        String message,
        String pattern
) {
}
