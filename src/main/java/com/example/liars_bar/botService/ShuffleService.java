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
    private final AnswerProducer answerProducer;


    private final List<Character> cards = Arrays.asList(
            'A', 'A', 'A', 'A', 'A', 'A',
            'K', 'K', 'K', 'K', 'K', 'K',
            'Q', 'Q', 'Q', 'Q', 'Q', 'Q',
            'J','J');

    public void shuffle(Group group) {

        Player player = group.currentPlayer();

        Event event = group.getEvent();
        if (event == null && group.getLI() != -1) {
            System.err.println("Player must have Shuffle event:" + player.getId());
            return;
        }
        if (event != null) {
            groupService.resetEvent(group);
            eventService.delete(event);
            if (player.getSticker() != -1) {
                group.getPlayers().values().stream().filter(p -> p.getSticker() != -1).forEach(
                        p -> answerProducer.response(Utils.delete(p.getId(), p.getSticker()))
                );
            }
        }

        group.setThrowCards(new ArrayList<>());

        Collections.shuffle(cards, new Random());

        List<Player> alivePlayers = group.getPlayers().values().stream()
                .filter(Player::isAlive)
                .toList();

        for (int i = 0; i < alivePlayers.size(); i++) {
            alivePlayers.get(i).setCards(cards.subList(5 * i, 5 * (i + 1)));
            alivePlayers.get(i).setPlayerState(GAME);
        }

        group.setCard(Arrays.asList('A', 'K', 'Q').get(new Random().nextInt(3)));

        //bar
        bar.execute(group);

        //card
        for (Player p: alivePlayers) {
            if (p.equals(player)) {
                card.executeBid(p);
            } else {
                card.executeEmoji(p);
            }
        }
        group.setEvent(new Event());
        playerService.save(player);
        groupService.save(group);
    }
}
