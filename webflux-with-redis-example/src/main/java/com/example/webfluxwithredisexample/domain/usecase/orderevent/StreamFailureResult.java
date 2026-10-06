package com.example.webfluxwithredisexample.domain.usecase.orderevent;

public record StreamFailureResult(
        String status, long attempts, String dlqRecordId, long acknowledged) {
        }
