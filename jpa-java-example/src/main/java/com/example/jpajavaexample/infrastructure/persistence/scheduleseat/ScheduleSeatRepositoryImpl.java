package com.example.jpajavaexample.infrastructure.persistence.scheduleseat;

import com.example.jpajavaexample.domain.ScheduleSeat;
import com.example.jpajavaexample.domain.scheduleseat.repository.ScheduleSeatRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ScheduleSeatRepositoryImpl implements ScheduleSeatRepository {

    private final JpaScheduleSeatRepository jpaScheduleSeatRepository;

    @Override
    public long count() {
        return jpaScheduleSeatRepository.count();
    }

    @Override
    @Transactional
    public ScheduleSeat save(ScheduleSeat scheduleSeat) {
        return jpaScheduleSeatRepository.save(scheduleSeat);
    }

    @Override
    public List<ScheduleSeat> findAll() {
        return jpaScheduleSeatRepository.findAll();
    }
}
