package com.example.liars_bar.botService;

import com.example.liars_bar.model.Event;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.rabbitmqService.AnswerProducer;
import com.example.liars_bar.service.EventService;
import com.example.liars_bar.service.GroupService;
import com.example.liars_bar.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.example.liars_bar.model.PlayerState.GAME;

@Component
@RequiredArgsConstructor
public class ShuffleService {

    private final GroupService groupService;
    private final PlayerService playerService;
    private final Bar bar;
    private final Card card;
    private final EventService eventService;


    private final List<Character> cards = Arrays.asList(
            'A', 'A', 'A', 'A', 'A', 'A',
            'K', 'K', 'K', 'K', 'K', 'K',
            'Q', 'Q', 'Q', 'Q', 'Q', 'Q',
            'J','J');

    public void shuffle(Group group) {

        Player player = group.getPlayers().get(group.getTurn());
        Event event = player.getEvent();
        if (event != null) {
            player.setEvent(null);
            playerService.save(player);
            eventService.delete(event);
            group.setTurn(groupService.index(group));
        }
        group.setThrowCards(new ArrayList<>());

        Collections.shuffle(cards, new Random());

        List<Player> activePlayers = group.getPlayers().stream()
                .filter(p -> p.isActive() && p.isAlive())
                .toList();

        for (int i = 0; i < activePlayers.size(); i++) {
            activePlayers.get(i).setCards(cards.subList(5 * i, 5 * (i + 1)));
            activePlayers.get(i).setPlayerState(GAME);
        }

        group.setCard(Arrays.asList('A', 'K', 'Q').get(new Random().nextInt(3)));

        Player pTemp = group.getPlayers().get(group.getTurn());
        pTemp.setEvent(new Event());

        //bar
        bar.execute(group);

        //card
        for (Player p: activePlayers) {
            if (p.equals(pTemp)) {
                card.executeB(p);
            } else {
                card.executeE(p);
            }
        }

        groupService.save(group);
    }
}
