package com.example.liars_bar.botService;

import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.example.liars_bar.model.PlayerState.GAME;

@Component
@RequiredArgsConstructor
public class ShuffleService {

    private final GroupService groupService;
    private final GameService gameService;

    private final List<String> cards = Arrays.asList(
            "A", "A", "A", "A", "A", "A",
            "K", "K", "K", "K", "K", "K",
            "Q", "Q", "Q", "Q", "Q", "Q",
            "J","J");

    public void shuffle(Group group) {
        Collections.shuffle(cards, new Random());

        List<Player> players = group.getPlayers();

        for (int i = 0; i < players.size(); i++) {
            players.get(i).setCards(cards.subList(5 * i, 5 * (i + 1)));
            players.get(i).setPlayerState(GAME);
        }

        group.setCard(Arrays.asList('A', 'K', 'Q').get(new Random().nextInt(3)));
        group.setPlayers(players);
        groupService.save(group);

        gameService.game(group);
    }
}
