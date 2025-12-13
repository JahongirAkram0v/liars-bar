package com.example.liars_bar.repo;

import com.example.liars_bar.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface EventRepo extends JpaRepository<Event, Integer> {

    @Query("SELECT e FROM Event e WHERE e.endTime <= :now")
    List<Event> findExpiredEvents(@Param("now") Instant now);

}
