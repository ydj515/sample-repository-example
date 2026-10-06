package com.example.webfluxwithredisexample.domain.usecase.orderevent;

import java.util.List;
import java.util.Map;

public record StreamRecoveryResult(
        String consumer, String nextCursor, int scanned, List<Map<String, Object>> processed) {
        }
