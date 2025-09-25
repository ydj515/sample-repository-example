package com.example.jpajavaexample.presentation.performance;

import com.example.jpajavaexample.application.performance.PerformanceQueryService;
import com.example.jpajavaexample.presentation.performance.dto.PerformanceDetailResponse;
import com.example.jpajavaexample.presentation.performance.dto.PerformanceSearchRequest;
import com.example.jpajavaexample.presentation.performance.dto.PerformanceSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/performances")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceQueryService performanceQueryService;

    @GetMapping
    public ResponseEntity<Page<PerformanceSummaryResponse>> getPerformances(
        @ModelAttribute PerformanceSearchRequest searchRequest,
        @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<PerformanceSummaryResponse> performances = performanceQueryService.getPerformances(
            searchRequest.title(),
            searchRequest.category(),
            searchRequest.startDate(),
            searchRequest.endDate(),
            pageable
        );
        return ResponseEntity.ok(performances);
    }

    @GetMapping("/{performanceId}")
    public ResponseEntity<PerformanceDetailResponse> getPerformanceDetail(@PathVariable Long performanceId) {
        PerformanceDetailResponse detail = performanceQueryService.getPerformanceDetail(performanceId);
        return ResponseEntity.ok(detail);
    }
}
