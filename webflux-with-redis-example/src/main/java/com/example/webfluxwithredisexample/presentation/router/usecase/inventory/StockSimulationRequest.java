package com.example.webfluxwithredisexample.presentation.router.usecase.inventory;

public record StockSimulationRequest(
        String mode, int initialStock, int requests, int concurrency, int quantity) {
}
