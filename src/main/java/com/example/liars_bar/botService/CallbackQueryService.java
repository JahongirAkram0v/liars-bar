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
import java.util.Random;

import static com.example.liars_bar.model.PlayerState.ADD;

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

        if (player.getPlayerState().equals(ADD) && !callbackData.equals("⚡️")) {
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
        else if (callbackData.equals("⚡️")) {
            player.setBar(messageId);
            playerService.save(player);
            Group group = player.getGroup();

            if (group == null) {
                sendService.send(
                        MessageUtilsService.errorMessage(callbackQueryId),
                        "answerCallbackQuery"
                );
                return;
            }

            int playerCount = (int) group.getPlayers().stream()
                    .filter(p -> p.getBar() != -1)
                    .count();

            if (playerCount == group.getPlayerCount()) {
                shuffleService.shuffle(group, new String[]{"", ""});
            } else {
                sendService.send(
                        MessageUtilsService.editMessage(
                                player.getBar(),
                                player.getId(),
                                "Kutib turing"
                        ),
                        "editMessageText"
                );
            }
        }
        else if (callbackData.equals("eye")) {
            player.setCardI(messageId);
            player.setCard(true);
            playerService.save(player);
            Group group = player.getGroup();

            if (group == null) {
                sendService.send(
                        MessageUtilsService.errorMessage(callbackQueryId),
                        "answerCallbackQuery"
                );
                return;
            }

            int playerCount = (int) group.getPlayers().stream()
                    .filter(p -> p.getCardI() != -1)
                    .count();

            if (playerCount == group.getPlayerCount()) {
                gameService.game(group, new String[]{"", "", ""});
            } else {
                sendService.send(
                        MessageUtilsService.editMessage(
                                player.getCardI(),
                                player.getId(),
                                "Kutib turing"
                        ),
                        "editMessageText"
                );
            }
        }
        else if (callbackData.startsWith("e")) {
            Group group = player.getGroup();
            int index = callbackData.charAt(1) - '0';
            player.setEM(index);
            playerService.save(player);
            String result = gameService.getResult(group, player);
            group.getPlayers().forEach(
                    p -> sendService.send(
                            MessageUtilsService.editMessage(
                                    p.getBar(),
                                    p.getId(),
                                    result
                            ),
                            "editMessageText"
                    )
            );
            sendService.send(
                    MessageUtilsService.editCard(player, index),
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

            String special = special(group.getThrowCards(), group.getCard());
            ///
            boolean isLie = group.getThrowCards().stream()
                    .anyMatch(s -> s != group.getCard() && s != 'J');
            group.setTurn(isLie ? group.getLPI(): group.getTurn());
            Player p = group.getPlayers().get(group.getTurn());

            String textG;

            if (p.getAttempt() + 1 == p.getChances()) {
                p.setIsAlive(false);
                p.setIsActive(false);

                sendService.send(
                        MessageUtilsService.editMessage(
                                p.getCardI(),
                                p.getId(),
                                "Siz yutqazdingiz."
                        ),
                        "editMessageText"
                );

                textG = p.getName() + " mag'lub bo'ldi.";
            } else {
                p.setAttempt(p.getAttempt() + 1);

                textG = p.getName() + "ning omadi bor ekan.";
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
            shuffleService.shuffle(group, new String[]{special, textG});
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
            player.setCard(true);
            player.setCards(playerCards);
            player.setTemp(new ArrayList<>());
            if (player.getCards().isEmpty()) {
                player.setIsActive(false);
            }

            group.setThrowCards(thrownCards);
            int index = groupService.index(group);
            group.setTurn(index);
            Player p = group.getPlayers().get(index);
            p.setCard(true);
            group.setLPI(player.getPlayerIndex());
            group.setBar(true);
            groupService.save(group);

            gameService.game(group, new String[]{"", "", ""});
        }
        else {
            Group group = player.getGroup();

            if (group == null) {
                sendService.send(
                        MessageUtilsService.errorMessage(callbackQueryId),
                        "answerCallbackQuery"
                );
                return;
            }

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
            player.setCard(true);
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
                correct.append("\uD83D\uDFE9");
            } else correct.append("\uD83D\uDFE5");
            cards.append(" ").append(c).append(" ");
        }
        return cards + "\n" + correct;
    }
}
