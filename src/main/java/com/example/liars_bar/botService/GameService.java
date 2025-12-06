package com.example.liars_bar.botService;

import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GameService {

    private final SendService sendService;
    private final GroupService groupService;
    private final List<String> emojis = List.of("", "\uD83D\uDE04", "\uD83E\uDD78", "\uD83D\uDE2D", "\uD83E\uDD2C", "\uD83D\uDE2E\u200D\uD83D\uDCA8");

    public void game(Group group, String texts) {

        Player pTemp = group.getPlayers().get(group.getTurn());

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
                                getResult(group, pTemp) + "\n\n" + texts
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
        Player lastPlayer = group.getPlayers().get(group.getLPI());
        if (lastPlayer.getIsActive() && lastPlayer.getIsAlive()) {
            sendService.send(
                    MessageUtilsService.editCard(lastPlayer, lastPlayer.getEM()),
                    "editMessageText"
            );
        }

        groupService.save(group);
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
