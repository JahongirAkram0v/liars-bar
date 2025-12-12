package com.example.liars_bar.botService;

import com.example.liars_bar.model.Event;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.service.EventService;
import com.example.liars_bar.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Win {

    private final Bar bar;
    private final Card card;
    private final PlayerService playerService;
    private final EventService eventService;

    public void execute(Player player) {
        Group group = player.getGroup();
        Event event = player.getEvent();
        if (event != null) {
            player.setEvent(null);
            playerService.save(player);
            eventService.delete(event);
        }

        bar.executeAll(group, player.getName());
        card.executeAll(group, "g'olib bo'ldi");
        // nimadir qilaman.
    }
}
