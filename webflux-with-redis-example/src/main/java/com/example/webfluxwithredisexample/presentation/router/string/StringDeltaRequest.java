package com.example.webfluxwithredisexample.presentation.router.string;

import com.example.webfluxwithredisexample.common.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "string delta request")
public record StringDeltaRequest(
        BaseRequest baseRequest,

        @Schema(description = "delta")
        @NotNull
        Long delta
) {
}
