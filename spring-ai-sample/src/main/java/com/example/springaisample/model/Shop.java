package com.example.springaisample.model;

import java.util.List;

public record Shop(
        String name,
        String description,
        String address,
        Double lat,
        Double lng,
        List<String> menu
) {
}
