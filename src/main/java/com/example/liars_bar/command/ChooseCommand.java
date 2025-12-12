package com.example.liars_bar.command;

import com.example.liars_bar.botService.Utils;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.rabbitmqService.AnswerProducer;
import com.example.liars_bar.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChooseCommand {

    private final AnswerProducer answerProducer;
    private final LiarCommand liarCommand;
    private final GroupService groupService;

    public void execute(Player player, int c) {
        Group group = player.getGroup();
        int activePlayersSize = (int) group.getPlayers().stream()
                .filter(p -> p.isActive() && p.isAlive())
                .count();

        if (activePlayersSize == 1) {
            liarCommand.execute(player);
            return;
        }

        List<Integer> temp = player.getTemp();
        if (!temp.remove(Integer.valueOf(c))) {
            temp.add(c);
        }
        player.setTemp(temp);
        groupService.save(group);

        String text = "\uD83D\uDD39\uD83D\uDD39\uD83D\uDD39 Sizning yurishingiz \uD83D\uDD39\uD83D\uDD39\uD83D\uDD39";

        answerProducer.response(
                Utils.editText(
                        player.getId(),
                        text,
                        List.of(
                                Utils.getEditBid(
                                        player.getCards(),
                                        temp
                                ),
                                List.of(
                                        Map.of("text", "Liar", "callback_data", "l"),
                                        Map.of("text", "Throw", "callback_data", "t")
                                )
                        ),
                        player.getCard()
                )
        );
    }
}
