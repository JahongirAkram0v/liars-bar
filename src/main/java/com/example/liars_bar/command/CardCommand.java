package com.example.liars_bar.command;

import com.example.liars_bar.botService.ShuffleService;
import com.example.liars_bar.botService.Utils;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.rabbitmqService.AnswerProducer;
import com.example.liars_bar.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardCommand {

    private final AnswerProducer answerProducer;
    private final PlayerService playerService;
    private final ShuffleService shuffleService;

    public void execute(Player player, int messageId) {
        player.setCard(messageId);
        playerService.save(player);
        Group group = player.getGroup();

        int playerCount = (int) group.getPlayers().stream()
                .filter(p -> p.getCard() != -1)
                .count();

        if (playerCount == group.getPC()) {
            shuffleService.shuffle(group);
        } else {
            String text = "Iltimos kuting";
            answerProducer.response(Utils.editText(player.getId(), text, messageId));
        }
    }
}
