package com.example.webfluxwithredisexample.presentation.router.pubsub;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "pub sub publish request")
public record PubSubPublishRequest(
        @Schema(description = "channel")
        @NotBlank
        @NotNull
        String channel,

        @Schema(description = "message")
        @NotBlank
        @NotNull
        String message
) {
}
