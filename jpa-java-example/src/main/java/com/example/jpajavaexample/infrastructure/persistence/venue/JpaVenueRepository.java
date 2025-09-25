package com.example.jpajavaexample.infrastructure.persistence.venue;

import com.example.jpajavaexample.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaVenueRepository extends JpaRepository<Venue, Long> {
}
