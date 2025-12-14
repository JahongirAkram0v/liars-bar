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

import java.util.List;
import java.util.Objects;

import static com.example.liars_bar.model.Action.LIE;

@Component
@RequiredArgsConstructor
public class LiarCommand {

    private final GroupService groupService;
    private final Bar bar;
    private final Card card;
    private final PlayerService playerService;
    private final EventService eventService;

    public void execute(Player player) {
        Group group = player.getGroup();

        if (group.getTurn() == player.getIndex()) {
            return;
        }

        Event event = player.getEvent();
        if (event == null) {
            System.err.println("Player must have Liar event:" + player.getId());
            return;
        }
        player.setEvent(null);
        playerService.save(player);
        eventService.delete(event);

        String special = special(group.getThrowCards(), group.getCard());

        boolean isLie = group.getThrowCards().stream().anyMatch(s -> s != group.getCard() && s != 'J');
        group.setTurn(isLie ? group.getLI(): group.getTurn());

        Player p = group.currentPlayer();
        Event newEvent = Event.builder()
                .action(LIE)
                .endTime(Event.getMin())
                .build();
        p.setEvent(newEvent);

        group.getPlayersList().forEach(t -> t.setActive(true));
        groupService.save(group);
        groupService.updateTurn(group);

        String text = "\uD83C\uDCCF : " + group.getCard() + " | " + player.getName() + " ishonmadi";
        bar.executeAll(group, text);
        card.executeAll(group, special);
    }

    private String special(List<Character> throwCards, Character card) {
        StringBuilder cards = new StringBuilder();
        StringBuilder correct = new StringBuilder();
        for (char c : throwCards) {
            if (c == card || c == 'J') {
                correct.append("\uD83D\uDFE9");
            } else correct.append("\uD83D\uDFE5");
            cards.append(" ").append(c).append(" ");
        }
        return cards + "\n" + correct;
    }
}
