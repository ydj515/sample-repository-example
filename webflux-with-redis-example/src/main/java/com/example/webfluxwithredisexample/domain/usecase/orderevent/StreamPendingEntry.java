package com.example.webfluxwithredisexample.domain.usecase.orderevent;

public record StreamPendingEntry(
        String recordId, String consumer, long idleMillis, long deliveries) {
        }
