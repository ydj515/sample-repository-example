package com.example.jpajavaexample.infrastructure.persistence.schedule;

import com.example.jpajavaexample.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaScheduleRepository extends JpaRepository<Schedule, Long> {
}
