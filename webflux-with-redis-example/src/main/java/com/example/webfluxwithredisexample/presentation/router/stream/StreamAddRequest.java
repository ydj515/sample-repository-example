package com.example.webfluxwithredisexample.presentation.router.stream;

import com.example.webfluxwithredisexample.common.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "stream add request")
public record StreamAddRequest(
        BaseRequest baseRequest,

        @Schema(description = "stream fields")
        @NotNull
        @NotEmpty
        Map<String, String> fields
) {
}
