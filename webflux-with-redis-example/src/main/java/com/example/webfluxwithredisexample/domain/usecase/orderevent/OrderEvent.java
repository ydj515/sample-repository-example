package com.example.webfluxwithredisexample.domain.usecase.orderevent;

public record OrderEvent(String orderId, String productId, int quantity) {
}
