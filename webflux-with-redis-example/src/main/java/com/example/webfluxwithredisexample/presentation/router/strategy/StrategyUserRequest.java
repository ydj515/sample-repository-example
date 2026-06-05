package com.example.webfluxwithredisexample.presentation.router.strategy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "strategy user request")
public record StrategyUserRequest(
        @Schema(description = "user id")
        @NotNull
        Long userId,

        @Schema(description = "user name")
        @NotBlank
        @NotNull
        String name,

        @Schema(description = "user email")
        @NotBlank
        @NotNull
        String email,

        @Schema(description = "user age")
        @NotNull
        Integer age
) {
}
