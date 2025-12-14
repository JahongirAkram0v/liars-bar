package com.example.liars_bar.botService;

import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.rabbitmqService.AnswerProducer;
import com.example.liars_bar.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.example.liars_bar.model.PlayerState.ADD;

@Component
@RequiredArgsConstructor
public class ReferralService {

    private final AnswerProducer answerProducer;
    private final GroupService groupService;

    public Optional<Group> isReferral(String text) {
        if (text == null || text.length() <= 7) {
            return Optional.empty();
        }

        return groupService.findById(text.substring(7));
    }

    public void referral(Player player, Group group) {

        int pS = group.playerCount();

        if (pS < group.getPC() && group.getLI() == -1) {
            player.setChances(new Random().nextInt(6) + 1);
            player.setPlayerState(ADD);
            player.setGroup(group);
            group.addPlayer(player);
            groupService.save(group);

            group.getPlayersList().forEach(
                    p -> {
                        String text = player.getName() + " qo'shildi. (" + (pS + 1) + "/" + group.getPC() + ")";
                        answerProducer.response(Utils.text(p.getId(), text));
                    }
            );

            if (pS + 1 == group.getPC()) {
                groupService.updateTurn(group);
                group.getPlayersList().forEach(
                        p -> {
                            String t = "Tugmani bosing!";
                            answerProducer.response(
                                    Utils.text(
                                            p.getId(),
                                            t,
                                            List.of(List.of(Map.of("text", "⚡️⚡️⚡️", "callback_data", "bar")))
                                    )
                            );
                        }
                );
            }
        }
    }
}
