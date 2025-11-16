package com.example.liars_bar.botService;

import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.service.GroupService;
import com.example.liars_bar.service.PlayerService;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CallbackQueryService {

    private final Dotenv dotenv = Dotenv.load();
    private final String botUsername = dotenv.get("TELEGRAM_BOT_USERNAME");

    private final GroupService groupService;
    private final PlayerService playerService;
    private final SendService sendService;
    private final ShuffleService shuffleService;
    private final GameService gameService;

    public void check(String callbackData, Integer messageId, Player player, String callbackQueryId) {

        if (callbackData.startsWith("c ")) {
            int count = callbackData.charAt(2) - '0';
            Group group = new Group();
            group.setPlayerCount(count);
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
        else if (callbackData.equals("l")) {

            Group group = player.getGroup();

            if (group.getThrowCards().isEmpty()) {
                sendService.send(
                        MessageUtilsService.errorMessage(callbackQueryId),
                        "answerCallbackQuery"
                );
                return;
            }

            sendService.send(
                    MessageUtilsService.editMessage(
                            messageId,
                            player.getId(),
                            "Siz ishonmadingiz"
                    ),
                    "editMessageText"
            );

            for (Player p: group.getPlayers()) {
                if (!p.equals(player)) {
                    sendService.send(
                            MessageUtilsService.sendMessage(
                                    p.getId(),
                                    player.getName() + " ishonmadi.\n" + group.getThrowCards().toString() + " tashlagan ekan."
                            ),
                            "sendMessage"
                    );
                }
            }

            ///

            if (group.getIsLie()) {

                Player lastPlayer = playerService.findById(group.getLastPlayerId()).orElseThrow();
                if (lastPlayer.getAttempt() + 1 == lastPlayer.getChances()) {
                    lastPlayer.setIsAlive(false);
                    lastPlayer.setIsActive(false);

                    sendService.send(
                            MessageUtilsService.sendMessage(
                                    lastPlayer.getId(),
                                    "Siz o'ldingiz."
                            ),
                            "sendMessage"
                    );

                    for (Player p : group.getPlayers()) {
                        if (!p.equals(lastPlayer)) sendService.send(
                                MessageUtilsService.sendMessage(
                                        p.getId(),
                                        lastPlayer.getName() + " o'ldi."
                                ),
                                "sendMessage"
                        );
                    }

                } else {
                    lastPlayer.setAttempt(lastPlayer.getAttempt() + 1);

                    sendService.send(
                            MessageUtilsService.sendMessage(
                                    lastPlayer.getId(),
                                    "Omadingiz bor ekan."
                            ),
                            "sendMessage"
                    );

                    for (Player p : group.getPlayers()) {
                        if (!p.equals(lastPlayer)) sendService.send(
                                MessageUtilsService.sendMessage(
                                        p.getId(),
                                        lastPlayer.getName() + " omadi bor ekan."
                                ),
                                "sendMessage"
                        );
                    }
                }
                playerService.save(lastPlayer);

            } else {

                if (player.getAttempt() + 1 == player.getChances()) {
                    player.setIsAlive(false);
                    player.setIsActive(false);

                    sendService.send(
                            MessageUtilsService.sendMessage(
                                    player.getId(),
                                    "Siz o'ldingiz."
                            ),
                            "sendMessage"
                    );

                    for (Player p : group.getPlayers()) {
                        if (!p.equals(player)) sendService.send(
                                MessageUtilsService.sendMessage(
                                        p.getId(),
                                        player.getName() + " o'ldi."
                                ),
                                "sendMessage"
                        );
                    }

                } else {
                    player.setAttempt(player.getAttempt() + 1);

                    sendService.send(
                            MessageUtilsService.sendMessage(
                                    player.getId(),
                                    "Omadingiz bor ekan."
                            ),
                            "sendMessage"
                    );

                    for (Player p : group.getPlayers()) {
                        if (!p.equals(player)) sendService.send(
                                MessageUtilsService.sendMessage(
                                        p.getId(),
                                        player.getName() + " omadi bor ekan."
                                ),
                                "sendMessage"
                        );
                    }
                }
                playerService.save(player);

                List<Player> activePlayers = group.getPlayers().stream()
                        .filter(p -> p.getIsActive() && p.getIsAlive())
                        .toList();

                int activePlayerSize = activePlayers.size();

                int turn = group.getTurn();

                if (activePlayerSize != 1) {

                    if (player.getIsActive()) {
                        turn = (turn + 1) % activePlayerSize;
                    } else {
                        turn = turn % (activePlayerSize - 1);
                    }
                    group.setTurn(turn);

                }


            }

            group.setThrowCards(new ArrayList<>());
            groupService.save(group);

            List<Player> alivePlayers = group.getPlayers().stream()
                    .filter(Player::getIsAlive)
                    .toList();

            if (alivePlayers.size() == 1) {
                Player winner = alivePlayers.getFirst();

                sendService.send(
                        MessageUtilsService.sendMessage(
                                winner.getId(),
                                "Siz g'olib bo'ldingiz! Tabriklaymiz!\nQayta boshlash uchun /start"
                        ),
                        "sendMessage"
                );

                for (Player p : group.getPlayers()) {
                    if (!p.equals(winner)) {
                        sendService.send(
                                MessageUtilsService.sendMessage(
                                        p.getId(),
                                        "O'yin tugadi. G'olib: " + winner.getName() + "\nQayta boshlash uchun /start"
                                ),
                                "sendMessage"
                        );
                    }
                }

                return;
            }

            shuffleService.shuffle(group);
        }
        else if (callbackData.equals("t")) {

            Group group = player.getGroup();

            if (player.getTemp() == null || player.getTemp().isEmpty()) {
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
            List<Player> activePlayers = group.getPlayers().stream()
                    .filter(p -> p.getIsActive() && p.getIsAlive())
                    .toList();

            int activePlayerSize = activePlayers.size();
            if (playerCards.isEmpty()) {
                if (activePlayerSize > 1) {
                    player.setIsActive(false);
                }
            }

            player.setCards(playerCards);
            group.setThrowCards(thrownCards);

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
                                    player.getName() + " " + player.getTemp().size() + " ta karta tashladi."
                            ),
                            "sendMessage"
                    );
                }
            }

            player.setTemp(new ArrayList<>());

            int turn = group.getTurn();
            if (player.getIsActive()) {
                turn = (turn + 1) % activePlayerSize;
            } else {
                turn = turn % (activePlayerSize - 1);
            }
            group.setTurn(turn);

            boolean isLie = thrownCards.stream()
                    .anyMatch(s -> s != group.getCard() && s != 'J');
            group.setIsLie(isLie);
            group.setLastPlayerId(player.getId());

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
}
