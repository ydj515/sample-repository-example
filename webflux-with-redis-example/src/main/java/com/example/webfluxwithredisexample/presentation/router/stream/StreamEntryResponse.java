package com.example.webfluxwithredisexample.presentation.router.stream;

import java.util.Map;

public record StreamEntryResponse(
        String id,
        Map<String, String> fields
) {
}
