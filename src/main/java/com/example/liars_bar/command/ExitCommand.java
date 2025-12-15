package com.example.liars_bar.command;

import com.example.liars_bar.botService.Utils;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.rabbitmqService.AnswerProducer;
import com.example.liars_bar.service.GroupService;
import com.example.liars_bar.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.example.liars_bar.model.Which.NOTHING;

@Component
@RequiredArgsConstructor
public class ExitCommand {

    private final AnswerProducer answerProducer;
    private final PlayerService playerService;
    private final GroupService groupService;

    public void execute(Player player) {

        Group group = player.getGroup();

        group.getPlayers().values()
                .forEach(p -> {
                    String text = p.equals(player)
                            ? "Siz guruhni tark etdiniz. /start tugmasini bosing"
                            : player.getName() + " guruhni tark etdi.";

                    answerProducer.response(Utils.text(p.getId(), text, NOTHING));
                }
        );

        group.removePlayer(player.getIndex());
        playerService.reset(player);
        if (group.getPlayers().isEmpty()) {
            groupService.delete(group);
        }

    }
}
