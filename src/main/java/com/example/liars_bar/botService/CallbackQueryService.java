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
                            "Siz ishonmadingiz.\n" + group.getThrowCards().toString() + " tashlagan ekan."
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
            Player p = (group.getIsLie()) ? group.getPlayers().get(group.getLPI()) : player;
            group.setTurn(group.getIsLie() ? group.getLPI(): group.getTurn());
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

            group.setTurn(playerService.index(group));
            group.setThrowCards(new ArrayList<>());

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

                for (Player  t: group.getPlayers()) {
                    if (!t.equals(winner)) {
                        sendService.send(
                                MessageUtilsService.sendMessage(
                                        t.getId(),
                                        "O'yin tugadi. G'olib: " + winner.getName() + "\nQayta boshlash uchun /start"
                                ),
                                "sendMessage"
                        );
                    }
                }
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
                                    player.getName() + " " + player.getTemp().size() + " ta karta tashladi."
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
            System.out.println("Turn " + group.getTurn());

            group.setThrowCards(thrownCards);
            group.setTurn(playerService.index(group));
            boolean isLie = thrownCards.stream()
                    .anyMatch(s -> s != group.getCard() && s != 'J');
            group.setIsLie(isLie);
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
}
