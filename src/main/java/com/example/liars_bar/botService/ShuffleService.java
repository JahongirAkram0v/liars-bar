package com.example.liars_bar.botService;

import com.example.liars_bar.model.Event;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
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

        Optional<Player> optionalPlayer = group.currentPlayer();
        if (optionalPlayer.isEmpty()) {
            System.err.println("shuffle: player not found which lpi");
            return;
        }
        Player player = optionalPlayer.get();

        Event event = player.getEvent();
        if (event != null) {
            player.setEvent(null);
            playerService.save(player);
            eventService.delete(event);
        }
        group.setThrowCards(new ArrayList<>());

        Collections.shuffle(cards, new Random());

        List<Player> alivePlayers = group.getPlayersList().stream()
                .filter(Player::isAlive)
                .toList();

        for (int i = 0; i < alivePlayers.size(); i++) {
            alivePlayers.get(i).setCards(cards.subList(5 * i, 5 * (i + 1)));
            alivePlayers.get(i).setPlayerState(GAME);
        }

        group.setCard(Arrays.asList('A', 'K', 'Q').get(new Random().nextInt(3)));

        Optional<Player> optionalPlayerTemp = group.currentPlayer();
        if (optionalPlayerTemp.isEmpty()) {
            System.err.println("shuffle: player not found");
            return;
        }
        Player pTemp = optionalPlayerTemp.get();

        pTemp.setEvent(new Event());

        //bar
        bar.execute(group);

        //card
        for (Player p: alivePlayers) {
            if (p.equals(pTemp)) {
                card.executeB(p);
            } else {
                card.executeE(p);
            }
        }

        groupService.save(group);
    }
}
