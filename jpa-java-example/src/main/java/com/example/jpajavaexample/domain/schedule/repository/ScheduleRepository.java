package com.example.jpajavaexample.domain.schedule.repository;

import com.example.jpajavaexample.domain.Schedule;
import java.util.List;

public interface ScheduleRepository {

    long count();

    Schedule save(Schedule schedule);

    List<Schedule> findAll();
}
