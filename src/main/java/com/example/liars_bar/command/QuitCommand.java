package com.example.liars_bar.command;

import com.example.liars_bar.botService.Bar;
import com.example.liars_bar.botService.Card;
import com.example.liars_bar.model.Action;
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

import static com.example.liars_bar.model.Action.WIN;

@Component
@RequiredArgsConstructor
public class QuitCommand {

    private final PlayerService playerService;
    private final GroupService groupService;
    private final Bar bar;
    private final Card card;
    private final EventService eventService;

    public void execute(Player player) {

        Group group = player.getGroup();
        Optional<Player> optionalPlayer = group.currentPlayer();
        if (optionalPlayer.isEmpty()) {
            System.err.println("quit: player not found which lpi");
            return;
        }
        Player pTemp = optionalPlayer.get();
        Event event = pTemp.getEvent();
        if (event != null) {
            pTemp.setEvent(null);
            playerService.save(pTemp);
            eventService.delete(event);
        }

        Long index = player.getId();
        if (!pTemp.getId().equals(index)) {
            group.setTurn(index);
            group.setLI(index);
        }

        group.removePlayer(index);
        playerService.reset(player);
        groupService.save(group);

        groupService.updateTurn(group);
        Optional<Player> optionalPlayerT = group.currentPlayer();
        if (optionalPlayerT.isEmpty()) {
            System.err.println("quit: player not found");
            return;
        }
        Player p = optionalPlayerT.get();
        Event newEvent = Event.builder()
                .action(Action.SHUFFLE)
                .endTime(Event.getMin())
                .build();
        p.setEvent(newEvent);
        playerService.save(p);

        System.out.println(group.getTurn());
        List<Player> alivePlayers = group.getPlayersList().stream()
                .filter(Player::isAlive)
                .toList();

        bar.executeP(player, "guruhni tark etdingiz.");
        card.execute(player, "Yangidan boshlash uchun /start ni bosing.");

        if (alivePlayers.size() == 1) {
            Player pT = alivePlayers.getFirst();
            Event win = Event.builder()
                    .action(WIN)
                    .endTime(Event.getMin())
                    .build();
            pT.setEvent(win);
            playerService.save(pT);
        }

        bar.execute(group);
        card.executeE(pTemp);
        card.executeB(p);
    }
}
