package com.example.liars_bar.service;

import com.example.liars_bar.model.Event;
import com.example.liars_bar.repo.EventRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepo eventRepo;


    public List<Event> findExpiredEvents(Instant now) {
        return eventRepo.findExpiredEvents(now);
    }

    public void delete(Event event) {
        eventRepo.delete(event);
    }
}
