package com.example.webfluxwithredisexample.presentation.router.sortedset;

import com.example.webfluxwithredisexample.common.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "sorted set conditional add request")
public record SortedSetConditionalAddRequest(
        BaseRequest baseRequest,

        @Schema(description = "member name")
        @NotBlank
        @NotNull
        String name,

        @Schema(description = "score")
        @NotNull
        Double score,

        @Schema(description = "option (NX, XX, GT, LT, CH)")
        String option
) {
}
