package com.example.webfluxwithredisexample.presentation.router.lua;

import com.example.webfluxwithredisexample.common.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "lua stock request")
public record LuaStockRequest(
        BaseRequest baseRequest,

        @Schema(description = "quantity")
        @NotNull
        Long quantity
) {
}
