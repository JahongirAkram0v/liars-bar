package com.example.liars_bar.botService;

import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GameService {

    private final SendService sendService;
    private final List<String> emojis = List.of("", "\uD83D\uDE04", "\uD83E\uDD78", "\uD83D\uDE2D", "\uD83E\uDD2C", "\uD83D\uDE2E\u200D\uD83D\uDCA8");

    public void game(Group group) {

        List<Player> activePlayers = group.getPlayers().stream()
                .filter(player -> player.getIsActive() && player.getIsAlive())
                .toList();

        Player pTemp = group.getPlayers().get(group.getTurn());
        //bar
        Result result = getResult(group, pTemp);

        //bar
        group.getPlayers().forEach(
                p -> sendService.send(
                        MessageUtilsService.sendMessage(
                                p.getId(),
                                result.text(),
                                result.keyboard()
                        ),
                        "sendMessage"
                )
        );

        //current player
        sendService.send(
                MessageUtilsService.sendMessage(
                        pTemp.getId(),
                        "Sizning yurishingiz",
                        MessageUtilsService.getBid(pTemp.getCards())
                ),
                "sendMessage"
        );

        //other players
        for (Player p: activePlayers) {
            if (!p.equals(pTemp)) {
                sendService.send(
                        MessageUtilsService.sendMessage(
                                p.getId(),
                                p.getCards().toString()
                        ),
                        "sendMessage"
                );
            }
        }
    }

    public Result getResult(Group group, Player pTemp) {
        List<List<Map<String, Object>>> keyboard = List.of(
                List.of(
                        Map.of("text", emojis.get(1), "callback_data", "e"+1),
                        Map.of("text", emojis.get(2), "callback_data", "e"+2),
                        Map.of("text", emojis.get(3), "callback_data", "e"+3),
                        Map.of("text", emojis.get(4), "callback_data", "e"+4),
                        Map.of("text", emojis.get(5), "callback_data", "e"+5)
                )
        );

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
        return new Result(keyboard, text.toString());
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
