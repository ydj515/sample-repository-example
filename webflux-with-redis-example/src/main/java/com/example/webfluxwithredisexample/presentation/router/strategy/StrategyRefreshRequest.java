package com.example.webfluxwithredisexample.presentation.router.strategy;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "strategy refresh request")
public record StrategyRefreshRequest(
        @Schema(description = "user ids to refresh. empty means refresh all fake DB users")
        List<Long> userIds
) {
}
