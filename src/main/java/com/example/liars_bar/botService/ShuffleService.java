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
    private final SendService sendService;

    private final List<Character> cards = Arrays.asList(
            'A', 'A', 'A', 'A', 'A', 'A',
            'K', 'K', 'K', 'K', 'K', 'K',
            'Q', 'Q', 'Q', 'Q', 'Q', 'Q',
            'J','J');

    public void shuffle(Group group, String texts) {
        Collections.shuffle(cards, new Random());

        List<Player> activePlayers = group.getPlayers().stream()
                .filter(player -> player.getIsActive() && player.getIsAlive())
                .toList();

        if (activePlayers.size() == 1) {
            gameService.winner(activePlayers.getFirst());
            groupService.delete(group);
            return;
        }

        for (int i = 0; i < activePlayers.size(); i++) {
            activePlayers.get(i).setCards(cards.subList(5 * i, 5 * (i + 1)));
            activePlayers.get(i).setPlayerState(GAME);
        }

        group.setCard(Arrays.asList('A', 'K', 'Q').get(new Random().nextInt(3)));

        Player pTemp = group.getPlayers().getFirst();

        group.getPlayers().forEach(
                p -> sendService.send(
                        MessageUtilsService.action(p.getId()),
                        "sendChatAction"
                )
        );

        //bar
        group.getPlayers().forEach(
                p -> sendService.send(
                        MessageUtilsService.editMessage(
                                p.getBar(),
                                p.getId(),
                                gameService.getResult(group, pTemp) + "\n\n" + texts
                        ),
                        "editMessageText"
                )
        );

        //current player
        sendService.send(
                MessageUtilsService.editMessage(
                        pTemp.getCardI(),
                        pTemp.getId(),
                        "Sizning yurishingiz",
                        MessageUtilsService.getBid(pTemp.getCards())
                ),
                "editMessageText"
        );

        //last players
        for (Player p: activePlayers) {
            if (!p.equals(pTemp)) {
                sendService.send(
                        MessageUtilsService.editCard(p, p.getEM()),
                        "editMessageText"
                );
            }
        }

        groupService.save(group);
    }
}
