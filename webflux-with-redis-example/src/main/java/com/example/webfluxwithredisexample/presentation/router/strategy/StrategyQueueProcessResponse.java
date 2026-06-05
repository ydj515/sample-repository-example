package com.example.webfluxwithredisexample.presentation.router.strategy;

import com.example.webfluxwithredisexample.domain.StrategyUser;

public record StrategyQueueProcessResponse(
        boolean processed,
        StrategyUser user,
        Long remainingQueueSize,
        String message
) {
}
