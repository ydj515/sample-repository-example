package com.example.webfluxwithredisexample.presentation.router.lua;

import com.example.webfluxwithredisexample.common.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "lua evalsha request")
public record LuaEvalShaRequest(
        BaseRequest baseRequest,

        @Schema(description = "script sha")
        @NotBlank
        @NotNull
        String sha,

        @Schema(description = "quantity")
        @NotNull
        Long quantity
) {
}
