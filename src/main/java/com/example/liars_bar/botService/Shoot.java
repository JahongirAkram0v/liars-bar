package com.example.liars_bar.botService;

import com.example.liars_bar.model.Action;
import com.example.liars_bar.model.Event;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.service.EventService;
import com.example.liars_bar.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static com.example.liars_bar.model.Action.SHUFFLE;

@Component
@RequiredArgsConstructor
public class Shoot {

    private final Bar bar;
    private final Card card;
    private final PlayerService playerService;
    private final EventService eventService;


    public void execute(Player player) {
        Group group = player.getGroup();
        Event event = player.getEvent();
        player.setEvent(null);
        playerService.save(player);
        eventService.delete(event);

        bar.executeAll(group, player.getName());

        if (player.getAttempt() + 1 == player.getChances()) {
            player.setAlive(false);
            player.setActive(false);

            card.executeAll(group, "omadi yoq ekan");
        } else {
            player.setAttempt(player.getAttempt() + 1);

            card.executeAll(group, "omadi bor ekan");
        }
        Event newEvent = Event.builder()
                .action(SHUFFLE)
                .endTime(Instant.now().plusSeconds(3))
                .build();
        player.setEvent(newEvent);
        playerService.save(player);
    }
}
