package com.example.jpajavaexample.infrastructure.persistence.schedule;

import com.example.jpajavaexample.domain.Schedule;
import com.example.jpajavaexample.domain.schedule.repository.ScheduleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ScheduleRepositoryImpl implements ScheduleRepository {

    private final JpaScheduleRepository jpaScheduleRepository;

    @Override
    public long count() {
        return jpaScheduleRepository.count();
    }

    @Override
    @Transactional
    public Schedule save(Schedule schedule) {
        return jpaScheduleRepository.save(schedule);
    }

    @Override
    public List<Schedule> findAll() {
        return jpaScheduleRepository.findAll();
    }
}
