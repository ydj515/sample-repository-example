package com.example.webfluxwithredisexample.presentation.router.strategy;

import com.example.webfluxwithredisexample.domain.StrategyUser;

public record StrategyUserResponse(
        String strategy,
        String source,
        StrategyUser user,
        Long remainingTtlSeconds,
        Double recomputeProbability,
        boolean refreshed,
        boolean queued,
        Long queueSize,
        String message
) {
}
