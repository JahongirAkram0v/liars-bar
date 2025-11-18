package com.example.liars_bar.botService;

import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.model.Result;
import com.example.liars_bar.service.GroupService;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.example.liars_bar.model.PlayerState.ADD;

@Component
@RequiredArgsConstructor
public class CallbackQueryService {

    private final Dotenv dotenv = Dotenv.load();
    private final String botUsername = dotenv.get("TELEGRAM_BOT_USERNAME");

    private final GroupService groupService;
    private final SendService sendService;
    private final ShuffleService shuffleService;
    private final GameService gameService;

    public void check(String callbackData, Integer messageId, Player player, String callbackQueryId) {

        if (player.getPlayerState().equals(ADD)) {
            sendService.send(
                    MessageUtilsService.errorMessage(callbackQueryId),
                    "answerCallbackQuery"
            );
            return;
        }

        if (callbackData.startsWith("c ")) {
            int count = callbackData.charAt(2) - '0';
            Group group = new Group();
            group.setPlayerCount(count);
            player.setChances(new Random().nextInt(6) + 1);
            player.setPlayerState(ADD);
            player.setGroup(group);

            sendService.send(
                    MessageUtilsService.sendMessage(
                            player.getId(),
                            "Guruh yaratdingiz va qoshildingiz."
                    ),
                    "sendMessage"
            );

            groupService.save(group);

            sendService.send(
                    MessageUtilsService.editMessage(
                            messageId,
                            player.getId(),
                            "O'yinni boshlash uchun referral!\n" +
                            "https://t.me/" + botUsername + "?start=" + group.getId()
                    ),
                    "editMessageText"
            );
        }
        else if (callbackData.startsWith("e")) {
            Group group = player.getGroup();
            int index = callbackData.charAt(1) - '0';
            player.setEM(index);
            Result result = gameService.getResult(group, player);
            group.getPlayers().forEach(
                    p -> sendService.send(
                            MessageUtilsService.editMessage(
                                    messageId,
                                    p.getId(),
                                    result.text(),
                                    result.keyboard()
                            ),
                            "editMessageText"
                    )
            );
        }
        else if (callbackData.equals("l")) {

            Group group = player.getGroup();

            if (group.getThrowCards().isEmpty()) {
                sendService.send(
                        MessageUtilsService.errorMessage(callbackQueryId),
                        "answerCallbackQuery"
                );
                return;
            }

            group.getPlayers().forEach(
                    p -> sendService.send(
                            MessageUtilsService.sendMessage(
                                    p.getId(),
                                    special(group.getThrowCards(), group.getCard())
                            ),
                            "sendMessage"
                    )
            );

            ///
            boolean isLie = group.getThrowCards().stream()
                    .anyMatch(s -> s != group.getCard() && s != 'J');
            group.setTurn(isLie ? group.getLPI(): group.getTurn());
            Player p =group.getPlayers().get(group.getTurn());
            if (p.getAttempt() + 1 == p.getChances()) {
                p.setIsAlive(false);
                p.setIsActive(false);

                sendService.send(
                        MessageUtilsService.sendMessage(
                                p.getId(),
                                "Siz o'ldingiz."
                        ),
                        "sendMessage"
                );

                for (Player t : group.getPlayers()) {
                    if (!t.equals(p)) sendService.send(
                            MessageUtilsService.sendMessage(
                                    t.getId(),
                                    p.getName() + " o'ldi."
                            ),
                            "sendMessage"
                    );
                }
            } else {
                p.setAttempt(p.getAttempt() + 1);

                sendService.send(
                        MessageUtilsService.sendMessage(
                                p.getId(),
                                "Omadingiz bor ekan."
                        ),
                        "sendMessage"
                );

                for (Player t : group.getPlayers()) {
                    if (!t.equals(p)) sendService.send(
                            MessageUtilsService.sendMessage(
                                    t.getId(),
                                    p.getName() + " omadi bor ekan."
                            ),
                            "sendMessage"
                    );
                }
            }

            List<Player> activePlayers = new ArrayList<>();
            for (Player t: group.getPlayers()) {
                t.setIsActive(true);
                activePlayers.add(t);
            }
            group.setPlayers(activePlayers);

            group.setTurn(groupService.index(group));
            group.setThrowCards(new ArrayList<>());

            List<Player> alivePlayers = group.getPlayers().stream()
                    .filter(Player::getIsAlive)
                    .toList();

            if (alivePlayers.size() == 1) {
                gameService.winner(alivePlayers.getFirst());
                groupService.delete(group);
                return;
            }

            groupService.save(group);
            shuffleService.shuffle(group);
        }
        else if (callbackData.equals("t")) {

            Group group = player.getGroup();

            if (player.getTemp().isEmpty()) {
                sendService.send(
                        MessageUtilsService.errorMessage(callbackQueryId),
                        "answerCallbackQuery"
                );
                return;
            }



            List<Character> thrownCards = player.getTemp().stream()
                    .map(i -> player.getCards().get(i))
                    .toList();

            List<Character> playerCards = new ArrayList<>();
            for (int i = 0; i < player.getCards().size(); i++) {
                if (!player.getTemp().contains(i)) {
                    playerCards.add(player.getCards().get(i));
                }
            }

            sendService.send(
                    MessageUtilsService.editMessage(
                            messageId,
                            player.getId(),
                            "Siz yurgan kartalar: " + thrownCards
                    ),
                    "editMessageText"
            );

            for (Player p: group.getPlayers()) {
                if (!p.equals(player)) {
                    sendService.send(
                            MessageUtilsService.sendMessage(
                                    p.getId(),
                                    "♠️x" + player.getTemp().size() + " " + group.getCard()
                            ),
                            "sendMessage"
                    );
                }
            }
            player.setCards(playerCards);
            player.setTemp(new ArrayList<>());
            if (player.getCards().isEmpty()) {
                player.setIsActive(false);
            }

            group.setThrowCards(thrownCards);
            group.setTurn(groupService.index(group));
            group.setLPI(player.getPlayerIndex());
            groupService.save(group);

            gameService.game(group);
        }
        else {
            Group group = player.getGroup();

            List<Player> activePlayers = group.getPlayers().stream()
                    .filter(p -> p.getIsActive() && p.getIsAlive())
                    .toList();

            if (activePlayers.size() == 1) {
                sendService.send(
                        MessageUtilsService.errorMessage(callbackQueryId),
                        "answerCallbackQuery"
                );
                return;
            }

            List<Integer> temp = player.getTemp();
            int c = Integer.parseInt(callbackData);
            if (!temp.remove(Integer.valueOf(c))) {
                temp.add(c);
            }
            player.setTemp(temp);

            groupService.save(group);

            Map<String, Object> message = MessageUtilsService.editMessage(
                    messageId,
                    player.getId(),
                    "Sizning yurishingiz",
                    List.of(
                            MessageUtilsService.getEditBid(
                                    player.getCards(),
                                    temp
                            ),
                            List.of(
                                    Map.of("text", "Liar", "callback_data", "l"),
                                    Map.of("text", "Throw", "callback_data", "t")
                            )
                    )

            );
            sendService.send(
                    message,
                    "editMessageText"
            );
        }
    }

    private String special(List<Character> throwCards, Character card) {
        StringBuilder cards = new StringBuilder();
        StringBuilder correct = new StringBuilder();
        for (char c : throwCards) {
            if (c == card || c == 'J') {
                correct.append("\uD83D\uDFE9 ");
            } else correct.append("\uD83D\uDFE5 ");
            cards.append(c).append("    ");
        }
        return cards.toString().trim() + "\n" + correct.toString().trim();
    }
}
