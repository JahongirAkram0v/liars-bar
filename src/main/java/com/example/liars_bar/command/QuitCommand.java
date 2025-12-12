package com.example.liars_bar.command;

import com.example.liars_bar.botService.Bar;
import com.example.liars_bar.botService.Card;
import com.example.liars_bar.botService.Win;
import com.example.liars_bar.model.Action;
import com.example.liars_bar.model.Event;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
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

    public void execute(Player player) {

        Group group = player.getGroup();
        group.getPlayers().remove(player);
        group.setTurn(player.getIndex());
        group.setLPI(player.getIndex());
        bar.executeP(player, "guruhni tark etdingiz.");
        card.execute(player, "Yangidan boshlash uchun /start ni bosing yoki havola orqali o'yinga qo'shiling.");
        playerService.reset(player.getId());

        bar.execute(group);

        List<Player> alivePlayers = group.getPlayers().stream()
                .filter(Player::isAlive)
                .toList();
        if (alivePlayers.size() == 1) {
            win.execute(alivePlayers.getFirst());
        }
        group.setTurn(groupService.index(group));
        Player p = group.getPlayers().get(group.getTurn());
        Event event = Event.builder()
                .action(Action.SHUFFLE)
                .endTime(Instant.now().plusSeconds(3))
                .build();
        p.setEvent(event);
    }
}
