package com.example.liars_bar.command;

import com.example.liars_bar.botService.Utils;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.rabbitmqService.AnswerProducer;
import com.example.liars_bar.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExitCommand {

    private final AnswerProducer answerProducer;
    private final PlayerService playerService;

    public void execute(Player player) {

        Group group = player.getGroup();
        playerService.reset(player.getId());

        for (Player p: group.getPlayers()) {
            String text;
            if (p.equals(player)) {
                text = "You have exited the game.";
            } else {
                text = player.getName() + " has exited the game.";
            }
            answerProducer.response(Utils.text(p.getId(), text));
        }
    }
}
