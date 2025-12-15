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

import static com.example.liars_bar.model.Action.*;

@Service
@RequiredArgsConstructor
public class EventChecker {

    private final EventService eventService;
    private final ThrowCommand throwCommand;
    private final Shoot shoot;
    private final ShuffleService shuffleService;
    private final Win win;

    @Scheduled(fixedRate = 1_000)
    public void checkEvents() {
        List<Event> events = eventService.findExpiredEvents(Instant.now());
        for (Event ev : events) {
            process(ev);
        }
    }

    private void process(Event event) {
        Action action = event.getAction();
        Group group = event.getGroup();
        if (action == THROW) {
            Player player = group.currentPlayer();
            if (player.getTemp().isEmpty()) {
                player.getTemp().add(0);
            }
            throwCommand.execute(player, null);
            return;
        }
        if (action == LIE) {
            shoot.execute(group);
            return;
        }
        if (action == SHUFFLE) {
            shuffleService.shuffle(group);
            return;
        }
        if (action == WIN) {
            win.execute(group);
        }
    }
}
