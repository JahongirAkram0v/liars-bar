package com.example.liars_bar.botService;

import com.example.liars_bar.model.Event;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.service.EventService;
import com.example.liars_bar.service.GroupService;
import com.example.liars_bar.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.example.liars_bar.model.Action.SHUFFLE;
import static com.example.liars_bar.model.Action.WIN;

@Component
@RequiredArgsConstructor
public class Shoot {

    private final Bar bar;
    private final Card card;
    private final PlayerService playerService;
    private final GroupService groupService;
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

            List<Player> alivePlayers = group.getPlayersList().stream()
                    .filter(Player::isAlive)
                    .toList();
            if (alivePlayers.size() == 1) {
                Player p = alivePlayers.getFirst();
                Event newEvent = Event.builder()
                        .action(WIN)
                        .endTime(Event.getMin())
                        .build();
                p.setEvent(newEvent);
                playerService.save(p);
                return;
            }
        } else {
            player.setAttempt(player.getAttempt() + 1);

            card.executeAll(group, "omadi bor ekan");
        }
        groupService.updateTurn(group);
        Optional<Player> optionalPlayer = group.currentPlayer();
        if (optionalPlayer.isEmpty()) {
            System.err.println("shoot: player not found which lpi");
            return;
        }
        Player p = optionalPlayer.get();

        Event newEvent = Event.builder()
                .action(SHUFFLE)
                .endTime(Event.getMin())
                .build();
        p.setEvent(newEvent);
        playerService.save(p);
    }
}
