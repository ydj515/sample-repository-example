package com.example.webfluxwithredisexample.presentation.router.usecase.inventory;

public record StockSimulationResponse(
        String runId,
        String mode,
        int initialStock,
        int requests,
        long accepted,
        long rejected,
        long remaining,
        long expectedRemaining,
        boolean consistent) {
}
