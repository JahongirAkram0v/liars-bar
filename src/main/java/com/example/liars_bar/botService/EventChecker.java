package com.example.liars_bar.botService;

import com.example.liars_bar.command.ThrowCommand;
import com.example.liars_bar.model.Action;
import com.example.liars_bar.model.Event;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

import static com.example.liars_bar.model.Action.LIE;
import static com.example.liars_bar.model.Action.THROW;

@Service
@RequiredArgsConstructor
public class EventChecker {

    private final EventService eventService;
    private final ThrowCommand throwCommand;
    private final Shoot shoot;

    @Scheduled(fixedRate = 1_000)
    public void checkEvents() {
        List<Event> events = eventService.findExpiredEvents(Instant.now());
        for (Event ev : events) {
            process(ev);
        }
    }

    private void process(Event event) {
        Action action = event.getAction();
        Player player = event.getPlayer();
        Group group = player.getGroup();
        if (action == THROW) {
            if (player.getTemp().isEmpty()) {
                group.getThrowCards().add(player.getCards().removeFirst());
            }
            throwCommand.execute(player);
        }
        if (action == LIE) {
            shoot.execute(player);
        }
        eventService.delete(event);
    }
}
