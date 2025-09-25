package com.example.jpajavaexample.domain.scheduleseat.repository;

import com.example.jpajavaexample.domain.ScheduleSeat;
import java.util.List;

public interface ScheduleSeatRepository {

    long count();

    ScheduleSeat save(ScheduleSeat scheduleSeat);

    List<ScheduleSeat> findAll();
}
