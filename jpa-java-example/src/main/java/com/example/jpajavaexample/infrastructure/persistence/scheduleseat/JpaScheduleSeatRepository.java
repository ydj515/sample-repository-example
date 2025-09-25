package com.example.jpajavaexample.infrastructure.persistence.scheduleseat;

import com.example.jpajavaexample.domain.ScheduleSeat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaScheduleSeatRepository extends JpaRepository<ScheduleSeat, Long> {
}
