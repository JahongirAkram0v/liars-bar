package com.example.liars_bar.botService;

import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.service.GroupService;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

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

            if (group.getThrowCards() == null) {
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
                                    player.getName() + " ishonmadi.\n" + group.getThrowCards().toString() + " ni tashlagan ekan."
                            ),
                            "sendMessage"
                    )
            );

            boolean isLie = group.getThrowCards().stream()
                    .anyMatch(s -> {
                        char card = player.getCards().get(s);
                        return card != group.getCard() && card != 'J';
                    });

            if (isLie) {
                group.getPlayers().forEach(
                        p -> sendService.send(
                                MessageUtilsService.sendMessage(
                                        p.getId(),
                                        "aldadi"
                                ),
                                "sendMessage"
                        )
                );
            } else {
                group.getPlayers().forEach(
                        p -> sendService.send(
                                MessageUtilsService.sendMessage(
                                        p.getId(),
                                        "aldamadi"
                                ),
                                "sendMessage"
                        )
                );
            }
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
                            "Siz yurgan kartalar: " + thrownCards.toString()
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

            //navbatni almashtirib gamega yuboraman.
            int turn = group.getTurn();
            if (player.getIsActive()) {
                turn = (turn + 1) % activePlayerSize;
            }
            group.setTurn(turn);
            groupService.save(group);
            gameService.game(group);
        }
        else {
            Group group = player.getGroup();

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
            System.out.println(message);
            sendService.send(
                    message,
                    "editMessageText"
            );
        }

    }
}
