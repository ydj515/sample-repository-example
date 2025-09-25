package com.example.jpajavaexample.domain.performance.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PerformanceNotFoundException extends RuntimeException {

    public PerformanceNotFoundException(Long performanceId) {
        super("Performance not found. id=" + performanceId);
    }
}
