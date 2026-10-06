package com.example.webfluxwithredisexample.domain.usecase.inventory;

public record StockResult(boolean accepted, long remaining) {
}
