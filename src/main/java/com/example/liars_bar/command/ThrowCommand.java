package com.example.liars_bar.command;

import com.example.liars_bar.botService.Bar;
import com.example.liars_bar.botService.Card;
import com.example.liars_bar.model.Event;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.service.EventService;
import com.example.liars_bar.service.GroupService;
import com.example.liars_bar.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ThrowCommand {

    private final GroupService groupService;
    private final PlayerService playerService;
    private final Bar bar;
    private final Card card;
    private final EventService eventService;
    private final LiarCommand liarCommand;


    public void execute(Player player) {

        if (player.getTemp().isEmpty()) {
            liarCommand.execute(player);
            return;
        }

        Group group = player.getGroup();
        Event event = player.getEvent();
        if (event == null) {
            return;
        }
        player.setEvent(null);
        playerService.save(player);
        eventService.delete(event);

        List<Character> thrownCards = player.getTemp().stream()
                .map(i -> player.getCards().get(i))
                .toList();

        List<Character> playerCards = new ArrayList<>();
        for (int i = 0; i < player.getCards().size(); i++) {
            if (!player.getTemp().contains(i)) {
                playerCards.add(player.getCards().get(i));
            }
        }
        player.setCards(playerCards);
        player.setTemp(new ArrayList<>());

        group.setThrowCards(thrownCards);
        group.setLI(player.getIndex());
        group.setTurn(groupService.index(group));
        Player p = group.currentPlayer();
        p.setEvent(new Event());

        bar.execute(group);

        if (player.getCards().isEmpty()) {
            player.setActive(false);
            String text = "Sizda karta qolmadi, o'yinni kuzating.";
            card.execute(player, text);
        } else {
            card.executeE(player);
        }

        card.executeB(p);

        groupService.save(group);
    }
}
