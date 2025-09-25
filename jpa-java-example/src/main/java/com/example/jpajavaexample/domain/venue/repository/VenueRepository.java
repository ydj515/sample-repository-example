package com.example.jpajavaexample.domain.venue.repository;

import com.example.jpajavaexample.domain.Venue;
import java.util.List;

public interface VenueRepository {

    long count();

    Venue save(Venue venue);

    List<Venue> findAll();
}
