package com.example.liars_bar.command;

import com.example.liars_bar.botService.Bar;
import com.example.liars_bar.botService.Card;
import com.example.liars_bar.botService.Win;
import com.example.liars_bar.model.Action;
import com.example.liars_bar.model.Event;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.service.EventService;
import com.example.liars_bar.service.GroupService;
import com.example.liars_bar.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class QuitCommand {

    private final PlayerService playerService;
    private final GroupService groupService;
    private final Win win;
    private final Bar bar;
    private final Card card;
    private final EventService eventService;

    public void execute(Player player) {

        Group group = player.getGroup();
        Player pTemp = group.currentPlayer();
        Event event = pTemp.getEvent();
        if (event != null) {
            pTemp.setEvent(null);
            playerService.save(pTemp);
            eventService.delete(event);
        }

        int index = player.getIndex();
        group.setTurn(index);
        group.setLI(index);
        playerService.reset(player);
        group.setTurn(groupService.index(group));
        Player p = group.currentPlayer();
        group.setTurn(p.getIndex());
        group.removePlayer(index);
        groupService.save(group);

        System.out.println(group.getTurn());
        List<Player> alivePlayers = group.getPlayersList().stream()
                .filter(Player::isAlive)
                .toList();
        if (alivePlayers.size() == 1) {
            win.execute(alivePlayers.getFirst());
        }
        Event newEvent = Event.builder()
                .action(Action.SHUFFLE)
                .endTime(Instant.now().plusSeconds(3))
                .build();
        p.setEvent(newEvent);
        bar.execute(group);
        card.executeE(pTemp);
        card.executeB(p);

        bar.executeP(player, "guruhni tark etdingiz.");
        card.execute(player, "Yangidan boshlash uchun /start ni bosing yoki havola orqali o'yinga qo'shiling.");

    }
}
