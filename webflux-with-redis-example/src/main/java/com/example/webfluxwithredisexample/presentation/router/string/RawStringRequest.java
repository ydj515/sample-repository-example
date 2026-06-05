package com.example.webfluxwithredisexample.presentation.router.string;

import com.example.webfluxwithredisexample.common.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "raw string request")
public record RawStringRequest(
        BaseRequest baseRequest,

        @Schema(description = "value")
        @NotBlank
        @NotNull
        String value,

        @Schema(description = "ttl seconds")
        Long ttlSeconds
) {
}
