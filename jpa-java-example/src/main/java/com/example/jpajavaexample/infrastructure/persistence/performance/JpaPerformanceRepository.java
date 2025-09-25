package com.example.jpajavaexample.infrastructure.persistence.performance;

import com.example.jpajavaexample.domain.performance.model.Performance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JpaPerformanceRepository extends JpaRepository<Performance, Long>, JpaSpecificationExecutor<Performance> {
}
