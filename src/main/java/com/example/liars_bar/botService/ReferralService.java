package com.example.liars_bar.botService;

import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static com.example.liars_bar.model.PlayerState.ADD;

@Component
@RequiredArgsConstructor
public class ReferralService {

    private final GroupService groupService;
    private final SendService sendService;

    public Optional<Group> isReferral(String text) {
        if (text == null || text.length() <= 7) {
            return Optional.empty();
        }

        return groupService.findById(text.substring(7));
    }

    public void referral(Player player, Group group) {

        int playersSize = group.getPlayers().size();

        if (playersSize < group.getPlayerCount()) {
            player.setChances(new Random().nextInt(6) + 1);
            player.setPlayerIndex(group.getTurn());
            player.setPlayerState(ADD);
            player.setGroup(group);
            group.getPlayers().add(player);
            group.setTurn(group.getTurn() + 1);
            groupService.save(group);

            for (Player p: group.getPlayers()) {
                sendService.send(
                        MessageUtilsService.sendMessage(
                                p.getId(),
                                player.getName() + " guruhga qo'shildi."
                        ),
                        "sendMessage"
                );
            }

            if (playersSize + 1 == group.getPlayerCount()) {
                group.setTurn(group.getPlayers().stream()
                        .mapToInt(Player::getPlayerIndex)
                        .min()
                        .orElse(0)
                );
                groupService.save(group);
                group.getPlayers().forEach(
                        p -> sendService.send(
                                MessageUtilsService.sendMessage(
                                    p.getId(),
                                        "Boshlash",
                                        List.of(List.of( Map.of("text", "⚡️⚡️⚡️", "callback_data", "⚡️")))
                                ),
                                "sendMessage"
                        )
                );
            }
        }
    }
}
