package com.example.liars_bar.botService;

import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.model.Which;
import com.example.liars_bar.rabbitmqService.AnswerProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class Card {

    private final AnswerProducer answerProducer;

    public void executeBid(Player player) {
        String text = "\uD83D\uDD39\uD83D\uDD39\uD83D\uDD39 Sizning yurishingiz \uD83D\uDD39\uD83D\uDD39\uD83D\uDD39";
        answerProducer.response(
                Utils.editText(
                        player.getId(),
                        text,
                        Utils.getBid(player.getCards()),
                        player.getCard()
                )
        );
    }
    public void executeEmoji(Player player) {
        answerProducer.response(
                Utils.editText(
                        player.getId(),
                        listCard(player.getCards()),
                        Utils.editCard(player.getEM()),
                        player.getCard()
                )
        );
    }

    public void executeText(Player player, String text) {
        answerProducer.response(
                Utils.editText(
                        player.getId(),
                        text,
                        player.getCard()
                )
        );
    }

    public void executeAllText(Group group, String text) {
        group.getPlayers().values().forEach(
                p -> answerProducer.response(
                        Utils.editText(
                                p.getId(),
                                text,
                                p.getCard()
                        )
                )
        );
    }

    public void executeSticker(Group group, String fileId, Which which) {
        group.getPlayers().values().forEach(
                p -> answerProducer.response(
                        Utils.sticker(
                                p.getId(),
                                fileId,
                                which
                        )
                )
        );
    }

    private static String listCard(List<Character> cards) {
        StringBuilder text = new StringBuilder("\uD83D\uDD39\uD83D\uDD38\uD83D\uDD39 ");
        for (char c : cards) {
            text.append(" ").append(c).append(" ");
        }
        return text + " \uD83D\uDD39\uD83D\uDD38\uD83D\uDD39";
    }


}
