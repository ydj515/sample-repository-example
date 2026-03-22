package com.example.springaisample.model;

import java.util.List;

public record Contents(
        String summary,
        List<Shop> items
) {
}
