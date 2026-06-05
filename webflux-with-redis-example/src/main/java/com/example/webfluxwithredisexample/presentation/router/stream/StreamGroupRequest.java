package com.example.webfluxwithredisexample.presentation.router.stream;

import com.example.webfluxwithredisexample.common.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "stream group request")
public record StreamGroupRequest(
        BaseRequest baseRequest,

        @Schema(description = "group name")
        @NotBlank
        @NotNull
        String group,

        @Schema(description = "offset such as 0-0 or $")
        String offset
) {
}
