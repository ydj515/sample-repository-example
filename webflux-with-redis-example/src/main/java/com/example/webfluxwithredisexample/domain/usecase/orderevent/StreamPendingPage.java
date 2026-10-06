package com.example.webfluxwithredisexample.domain.usecase.orderevent;

import java.util.List;

public record StreamPendingPage(String nextCursor, List<StreamPendingEntry> entries) {
}
