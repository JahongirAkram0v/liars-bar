package com.example.liars_bar.botService;

import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.service.GroupService;
import com.example.liars_bar.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GameService {

    private final SendService sendService;
    private final GroupService groupService;
    private final PlayerService playerService;
    private final List<String> emojis = List.of("", "\uD83D\uDE04", "\uD83E\uDD78", "\uD83D\uDE2D", "\uD83E\uDD2C", "\uD83D\uDE2E\u200D\uD83D\uDCA8");

    public void game(Group group, String[] texts) {

        List<Player> activePlayers = group.getPlayers().stream()
                .filter(player -> player.getIsActive() && player.getIsAlive())
                .toList();

        Player pTemp = group.getPlayers().get(group.getTurn());
        //bar
        String result = getResult(group, pTemp);
        String extra = texts[0] + "\n" +texts[2];

        if (group.isBar()) {
            group.getPlayers().forEach(
                    p -> sendService.send(
                            MessageUtilsService.editMessage(
                                    p.getBar(),
                                    p.getId(),
                                    result + "\n\n" + (extra.trim())
                            ),
                            "editMessageText"
                    )
            );
        }

        for (Player p: group.getPlayers()) {
            if (p.getCardI() == -1 && p.isCard()) {
                sendService.send(
                        MessageUtilsService.sendMessage(
                                p.getId(),
                                "Iltimos bosing.\nQo'lni ko'rish",
                                List.of(List.of( Map.of("text", "\uD83D\uDC41", "callback_data", "eye")))
                        ),
                        "sendMessage"
                );
                p.setCard(false);
                playerService.save(p);
            }
        }

        //current player
        if (pTemp.isCard()) {
            sendService.send(
                    MessageUtilsService.editMessage(
                            pTemp.getCardI(),
                            pTemp.getId(),
                            "Sizning yurishingiz",
                            MessageUtilsService.getBid(pTemp.getCards())
                    ),
                    "editMessageText"
            );
        }


        //other players
        List<List<Map<String, Object>>> keyboard = List.of(
                List.of(
                        Map.of("text", emojis.get(1), "callback_data", "e"+1),
                        Map.of("text", emojis.get(2), "callback_data", "e"+2),
                        Map.of("text", emojis.get(3), "callback_data", "e"+3),
                        Map.of("text", emojis.get(4), "callback_data", "e"+4),
                        Map.of("text", emojis.get(5), "callback_data", "e"+5)
                )
        );
        for (Player p: activePlayers) {
            if (!p.equals(pTemp) && p.isCard()) {
                sendService.send(
                        MessageUtilsService.editMessage(
                                p.getCardI(),
                                p.getId(),
                                listCard(p.getCards()),
                                keyboard
                        ),
                        "editMessageText"
                );
            }
        }

        group.setBar(false);
        List<Player> players = new ArrayList<>();
        for (Player p: group.getPlayers()) {
            p.setCard(false);
            players.add(p);
        }
        group.setPlayers(players);
        groupService.save(group);
    }

    private String listCard(List<Character> cards) {
        StringBuilder text = new StringBuilder("▪️ ");
        for (char c : cards) {
            text.append(" ").append(c).append(" ");
        }
        return text + " ▪️";
    }

    public String getResult(Group group, Player pTemp) {

        StringBuilder text = new StringBuilder("\uD83C\uDCCF : " + group.getCard() + "\n" );
        for (Player p: group.getPlayers()) {
            text.append(getANC(p))
                    .append(" - (" )
                    .append(p.getAttempt())
                    .append("/6) " )
                    .append(getA(p, pTemp))
                    .append(" | ♠️x")
                    .append(p.getCards().size())
                    .append(getE(p))
                    .append("\n");
        }
        if (!group.getThrowCards().isEmpty()) {
            text.append("\n")
                    .append("♠️x")
                    .append(group.getThrowCards().size())
                    .append(" ")
                    .append(group.getCard());
        }
        return text.toString().trim();
    }

    private String getE(Player p) {
        if (p.getEM() == 0) return emojis.getFirst();
        return " /" + emojis.get(p.getEM());
    }

    public void winner(Player player) {

        Group group = player.getGroup();

        sendService.send(
                MessageUtilsService.sendMessage(
                        player.getId(),
                        "Siz g'olib bo'ldingiz! Tabriklaymiz!\nQayta boshlash uchun /start"
                ),
                "sendMessage"
        );

        for (Player  t: group.getPlayers()) {
            if (!t.equals(player)) {
                sendService.send(
                        MessageUtilsService.sendMessage(
                                t.getId(),
                                "O'yin tugadi. G'olib: " + player.getName() + "\nQayta boshlash uchun /start"
                        ),
                        "sendMessage"
                );
            }
        }
    }

    private String getA(Player p, Player playerTemp) {
        return p.equals(playerTemp) ? "\uD83D\uDC7E" : "";
    }

    private String getANC(Player p) {
        return (p.getIsAlive() ? "\uD83D\uDC64 : " : "💀 : ") + p.getName();
    }
}
