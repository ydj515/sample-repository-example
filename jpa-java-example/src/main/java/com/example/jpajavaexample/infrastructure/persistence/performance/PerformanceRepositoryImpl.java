package com.example.jpajavaexample.infrastructure.persistence.performance;

import com.example.jpajavaexample.domain.performance.model.Performance;
import com.example.jpajavaexample.domain.performance.repository.PerformanceRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PerformanceRepositoryImpl implements PerformanceRepository {

    private final JpaPerformanceRepository jpaPerformanceRepository;

    @Override
    public Page<Performance> search(String title, String category, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Specification<Performance> spec = Specification.where(null);

        if (StringUtils.hasText(title)) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("title")), like(title)));
        }

        if (StringUtils.hasText(category)) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
        }

        if (startDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), startDate));
        }

        if (endDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("endDate"), endDate));
        }

        return jpaPerformanceRepository.findAll(spec, pageable);
    }

    @Override
    public Page<Performance> findAll(Pageable pageable) {
        return jpaPerformanceRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public Performance save(Performance performance) {
        return jpaPerformanceRepository.save(performance);
    }

    @Override
    public long count() {
        return jpaPerformanceRepository.count();
    }

    private String like(String keyword) {
        return "%" + keyword.toLowerCase() + "%";
    }
}
