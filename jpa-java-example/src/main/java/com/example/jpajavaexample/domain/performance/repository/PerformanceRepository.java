package com.example.jpajavaexample.domain.performance.repository;

import com.example.jpajavaexample.domain.performance.model.Performance;
import com.example.jpajavaexample.domain.performance.service.PerformanceDetailInfo;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PerformanceRepository {

    Page<Performance> search(String title, String category, LocalDate startDate, LocalDate endDate, Pageable pageable);

    Page<Performance> findAll(Pageable pageable);

    Performance save(Performance performance);

    long count();

    List<Performance> findAll();

    Optional<PerformanceDetailInfo> findDetail(Long performanceId);
}
