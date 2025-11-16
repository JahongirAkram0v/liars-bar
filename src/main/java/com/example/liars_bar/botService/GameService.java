package com.example.liars_bar.botService;

import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
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

        Player pTemp = group.getPlayers().get(group.getTurn());

        //ozgartirishim kerak
        //buni ham yaxshilashim kerak

        //bar
        List<List<Map<String,Object>>> keyboard = List.of(
                List.of(
                        Map.of("text", emojis.get(1), "callback_data", "e 1"),
                        Map.of("text", emojis.get(2), "callback_data", "e 2"),
                        Map.of("text", emojis.get(3), "callback_data", "e 3"),
                        Map.of("text", emojis.get(4), "callback_data", "e 4"),
                        Map.of("text", emojis.get(5), "callback_data", "e 5")
                )
        );

        StringBuilder text = new StringBuilder("\uD83C\uDCCF : " + group.getCard() + "\n" );
        for (Player p: group.getPlayers()) {
            text.append(getANC(p))
                    .append(" - (" )
                    .append(p.getAttempt())
                    .append("/6) " )
                    .append(getA(p, pTemp))
                    .append(emojis.get(p.getEN()))
                    .append("\n");
        }

        group.getPlayers().forEach(
                p -> sendService.send(
                        MessageUtilsService.sendMessage(
                                p.getId(),
                                text.toString(),
                                keyboard
                        ),
                        "sendMessage"
                )
        );

        sendService.send(
                MessageUtilsService.sendMessage(
                        pTemp.getId(),
                        "Kartangiz",
                        MessageUtilsService.getBid(pTemp.getCards())
                ),
                "sendMessage"
        );

        group.getPlayers().forEach(
                p -> sendService.send(
                        MessageUtilsService.sendMessage(
                                p.getId(),
                                p.getCards().toString()
                        ),
                        "sendMessage"
                )
        );
    }

    private String getA(Player p, Player playerTemp) {
        return p.equals(playerTemp) ? "\uD83D\uDC7E | " : " | ";
    }

    private String getANC(Player p) {
        return (p.getIsAlive() ? "\uD83D\uDC64 : " : "💀 : ") + p.getName();
    }
}
