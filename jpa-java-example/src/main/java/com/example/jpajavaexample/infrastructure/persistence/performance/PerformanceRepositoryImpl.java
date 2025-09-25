package com.example.jpajavaexample.infrastructure.persistence.performance;

import com.example.jpajavaexample.domain.performance.model.Performance;
import com.example.jpajavaexample.domain.performance.repository.PerformanceRepository;
import com.example.jpajavaexample.domain.performance.service.PerformanceDetailInfo;
import com.example.jpajavaexample.domain.performance.service.PerformanceScheduleInfo;
import com.example.jpajavaexample.domain.performance.service.PerformanceSeatGradeInfo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NoResultException;
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

    @PersistenceContext
    private EntityManager entityManager;

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

    @Override
    public List<Performance> findAll() {
        return jpaPerformanceRepository.findAll();
    }

    @Override
    public Optional<PerformanceDetailInfo> findDetail(Long performanceId) {
        // getResultList()를 사용하여 NoResultException 발생 차단
        List<PerformanceBasicProjection> basics = entityManager.createQuery(
                        "select new com.example.jpajavaexample.infrastructure.persistence.performance.PerformanceBasicProjection(" +
                                " p.id, p.title, p.category, p.posterUrl, p.startDate, p.endDate)" +
                                " from Performance p" +
                                " where p.id = :performanceId",
                        PerformanceBasicProjection.class)
                .setParameter("performanceId", performanceId)
                .getResultList();

        // 결과가 없으면 Optional.empty()를 반환하여 메서드 시그니처의 의도를 명확히 함
        if (basics.isEmpty()) {
            return Optional.empty();
        }

        PerformanceBasicProjection basic = basics.get(0);

        List<PerformanceScheduleProjection> scheduleProjections = entityManager.createQuery(
                "select new com.example.jpajavaexample.infrastructure.persistence.performance.PerformanceScheduleProjection(" +
                    " s.id, s.startsAt)" +
                    " from Schedule s" +
                    " where s.performance.id = :performanceId" +
                    " order by s.startsAt asc",
                PerformanceScheduleProjection.class)
            .setParameter("performanceId", performanceId)
            .getResultList();

        Map<Long, List<PerformanceSeatGradeInfo>> seatGradesBySchedule = entityManager.createQuery(
                "select new com.example.jpajavaexample.infrastructure.persistence.performance.PerformanceSeatGradeProjection(" +
                    " s.id," +
                    " s.startsAt," +
                    " vs.name," +
                    " count(ss.id)," +
                    " sum(case when ss.status = com.example.jpajavaexample.domain.SeatStatus.AVAILABLE then 1 else 0 end)," +
                    " min(ss.price)," +
                    " max(ss.price))" +
                    " from ScheduleSeat ss" +
                    " join ss.schedule s" +
                    " join ss.seat seat" +
                    " join seat.venueSection vs" +
                    " where s.performance.id = :performanceId" +
                    " group by s.id, s.startsAt, vs.name" +
                    " order by s.startsAt asc, vs.name asc",
                PerformanceSeatGradeProjection.class)
            .setParameter("performanceId", performanceId)
            .getResultList()
            .stream()
            .collect(Collectors.groupingBy(
                PerformanceSeatGradeProjection::scheduleId,
                LinkedHashMap::new,
                Collectors.mapping(
                    projection -> new PerformanceSeatGradeInfo(
                        projection.gradeName(),
                        projection.totalSeats(),
                        projection.availableSeats(),
                        projection.minPrice(),
                        projection.maxPrice()
                    ),
                    Collectors.toCollection(ArrayList::new)
                )
            ));

        List<PerformanceScheduleInfo> schedules = new ArrayList<>();
        for (PerformanceScheduleProjection projection : scheduleProjections) {
            List<PerformanceSeatGradeInfo> seatGrades = seatGradesBySchedule.getOrDefault(
                projection.scheduleId(),
                List.of()
            );
            schedules.add(new PerformanceScheduleInfo(
                projection.scheduleId(),
                projection.startsAt(),
                List.copyOf(seatGrades)
            ));
        }

        return Optional.of(new PerformanceDetailInfo(
            basic.performanceId(),
            basic.title(),
            basic.category(),
            basic.posterUrl(),
            basic.startDate(),
            basic.endDate(),
            List.copyOf(schedules)
        ));
    }

    private String like(String keyword) {
        return "%" + keyword.toLowerCase() + "%";
    }
}

record PerformanceBasicProjection(
    Long performanceId,
    String title,
    String category,
    String posterUrl,
    LocalDate startDate,
    LocalDate endDate
) {
}

record PerformanceScheduleProjection(
    Long scheduleId,
    LocalDateTime startsAt
) {
}

record PerformanceSeatGradeProjection(
    Long scheduleId,
    LocalDateTime startsAt,
    String gradeName,
    long totalSeats,
    long availableSeats,
    BigDecimal minPrice,
    BigDecimal maxPrice
) {
}
