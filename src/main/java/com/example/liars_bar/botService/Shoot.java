package com.example.liars_bar.botService;

import com.example.liars_bar.model.Event;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.service.EventService;
import com.example.liars_bar.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.example.liars_bar.model.Action.SHUFFLE;
import static com.example.liars_bar.model.Action.WIN;

@Component
@RequiredArgsConstructor
public class Shoot {

    private final Bar bar;
    private final Card card;
    private final GroupService groupService;
    private final EventService eventService;


    public void execute(Group group) {

        Event event = group.getEvent();
        groupService.resetEvent(group);
        eventService.delete(event);
        Player player = group.currentPlayer();

        bar.executeAll(group, player.getName());

        if (player.getAttempt() + 1 == player.getChances()) {
            player.setAlive(false);
            player.setActive(false);

            card.executeAllText(group, "...");
            card.executeSticker(group, "CAACAgIAAxkBAAISJ2k_2HedMrAy56rTSUmmC95548JSAAILhgACTcwAAUpZD2FuI4n8nzYE");

            List<Player> alivePlayers = group.getPlayers().values().stream()
                    .filter(Player::isAlive)
                    .toList();
            if (alivePlayers.size() == 1) {

                group.setTurn(alivePlayers.getFirst().getIndex());
                groupService.updateTurn(group);
                Event newEvent = Event.builder()
                        .action(WIN)
                        .endTime(Event.getMin())
                        .build();
                group.setEvent(newEvent);
                groupService.save(group);
                return;
            }
        } else {
            player.setAttempt(player.getAttempt() + 1);

            card.executeAllText(group, "...");
            card.executeSticker(group, "CAACAgIAAxkBAAISKGk_2I14E9JWUjIAAUKKvNH5LbDIbgAChIoAAkqCAAFKc183VZPcPpA2BA");
        }
        groupService.updateTurn(group);
        Event newEvent = Event.builder()
                .action(SHUFFLE)
                .endTime(Event.getMin())
                .build();
        group.setEvent(newEvent);
        groupService.save(group);
    }
}
