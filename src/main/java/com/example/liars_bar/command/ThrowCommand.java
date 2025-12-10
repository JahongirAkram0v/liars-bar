package com.example.liars_bar.command;

import com.example.liars_bar.botService.Card;
import com.example.liars_bar.model.Event;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ThrowCommand {

    private final GroupService groupService;
    private final Card card;


    public void execute(Player player) {
        Group group = player.getGroup();

        List<Character> thrownCards = player.getTemp().stream()
                .map(i -> player.getCards().get(i))
                .toList();

        thrownCards.forEach(c -> player.getCards().remove(c));
        player.setTemp(new ArrayList<>());

        if (player.getCards().isEmpty()) {
            player.setIsActive(false);
            String text = "Sizda karta qolmadi, o'yinni kuzating.";
            card.execute(player, text);
        }

        group.setThrowCards(thrownCards);
        group.setTurn(groupService.index(group));
        Player p = group.getPlayers().get(group.getTurn());
        p.setEvent(new Event());
        group.setLPI(player.getPlayerIndex());
        groupService.save(group);
    }
}
