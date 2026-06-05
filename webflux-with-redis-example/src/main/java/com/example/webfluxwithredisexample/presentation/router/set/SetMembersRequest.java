package com.example.webfluxwithredisexample.presentation.router.set;

import com.example.webfluxwithredisexample.common.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "redis set members request")
public record SetMembersRequest(
        BaseRequest baseRequest,

        @Schema(description = "members")
        @NotNull
        @NotEmpty
        List<String> members
) {
}
