package com.example.jpajavaexample.application.performance;

import com.example.jpajavaexample.domain.performance.repository.PerformanceRepository;
import com.example.jpajavaexample.presentation.performance.dto.PerformanceSummaryResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PerformanceQueryService {

    private final PerformanceRepository performanceRepository;

    @Transactional(readOnly = true)
    public Page<PerformanceSummaryResponse> getPerformances(
        String title,
        String category,
        LocalDate startDate,
        LocalDate endDate,
        Pageable pageable
    ) {
        return performanceRepository.search(title, category, startDate, endDate, pageable)
            .map(PerformanceSummaryResponse::from);
    }
}
