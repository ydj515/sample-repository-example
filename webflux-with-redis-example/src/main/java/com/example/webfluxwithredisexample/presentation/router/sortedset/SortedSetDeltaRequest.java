package com.example.webfluxwithredisexample.presentation.router.sortedset;

import com.example.webfluxwithredisexample.common.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "sorted set delta request")
public record SortedSetDeltaRequest(
        BaseRequest baseRequest,

        @Schema(description = "member name")
        @NotBlank
        @NotNull
        String name,

        @Schema(description = "score delta")
        @NotNull
        Double delta
) {
}
