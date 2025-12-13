package com.example.liars_bar.command;

import com.example.liars_bar.botService.Utils;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.rabbitmqService.AnswerProducer;
import com.example.liars_bar.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BarCommand {

    private final AnswerProducer answerProducer;
    private final PlayerService playerService;


    public void execute(Player player, int messageId) {
        player.setBar(messageId);
        playerService.save(player);
        Group group = player.getGroup();

        int pC = (int) group.getPlayersList().stream()
                .filter(p -> p.getBar() != -1)
                .count();

        String text = "⌛️⏳⌛️⏳";
        answerProducer.response(Utils.editText(player.getId(), text, messageId));

        if (pC == group.getPC()) {
            group.getPlayersList().forEach(
                    p -> {
                        String t = "Bu tugmani ham bosing!";
                        answerProducer.response(
                                Utils.text(
                                        p.getId(),
                                        t,
                                        List.of(List.of(Map.of("text", "⚡️⚡️⚡️", "callback_data", "card")))
                                )
                        );
                    }
            );
        }
    }
}
