package com.example.liars_bar.botService;

import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GameService {

    private final SendService sendService;
    private final List<String> emojis = List.of("", "\uD83D\uDE04", "\uD83E\uDD78", "\uD83D\uDE2D", "\uD83E\uDD2C", "\uD83D\uDE2E\u200D\uD83D\uDCA8");

    public void game(Group group) {

        List<Player> activePlayers = group.getPlayers().stream()
                .filter(player -> player.getIsActive() && player.getIsAlive())
                .toList();

        Player pTemp = activePlayers.get(group.getTurn());
        //bar
        StringBuilder text = new StringBuilder("\uD83C\uDCCF : " + group.getCard() + "\n" );
        for (Player p: group.getPlayers()) {
            text.append(getANC(p))
                    .append(" - (" )
                    .append(p.getAttempt())
                    .append("/6) " )
                    .append(getA(p, pTemp))
                    .append(" | ♠️x")
                    .append(p.getCards().size())
                    .append("\n");
        }

        group.getPlayers().forEach(
                p -> sendService.send(
                        MessageUtilsService.sendMessage(
                                p.getId(),
                                text.toString()
                        ),
                        "sendMessage"
                )
        );

        sendService.send(
                MessageUtilsService.sendMessage(
                        pTemp.getId(),
                        "Sizning yurishingiz",
                        MessageUtilsService.getBid(pTemp.getCards())
                ),
                "sendMessage"
        );

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

    private String getA(Player p, Player playerTemp) {
        return p.equals(playerTemp) ? "\uD83D\uDC7E" : "";
    }

    private String getANC(Player p) {
        return (p.getIsAlive() ? "\uD83D\uDC64 : " : "💀 : ") + p.getName();
    }
}
