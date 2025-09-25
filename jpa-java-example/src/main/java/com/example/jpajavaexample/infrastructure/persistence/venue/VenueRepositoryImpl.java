package com.example.jpajavaexample.infrastructure.persistence.venue;

import com.example.jpajavaexample.domain.Venue;
import com.example.jpajavaexample.domain.venue.repository.VenueRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VenueRepositoryImpl implements VenueRepository {

    private final JpaVenueRepository jpaVenueRepository;

    @Override
    public long count() {
        return jpaVenueRepository.count();
    }

    @Override
    @Transactional
    public Venue save(Venue venue) {
        return jpaVenueRepository.save(venue);
    }

    @Override
    public List<Venue> findAll() {
        return jpaVenueRepository.findAll();
    }
}
