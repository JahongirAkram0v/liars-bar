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
    private final ShuffleService shuffleService;

    public Optional<Group> isReferral(String text) {
        if (text == null || text.length() <= 7) {
            return Optional.empty();
        }

        return groupService.findById(text.substring(7));
    }

    public void referral(Player player, Group group) {

        System.out.println("salom");

        int playersSize = group.getPlayers() == null ? 0 : group.getPlayers().size();
        System.out.println(playersSize);

        if (playersSize < group.getPlayerCount()) {
            player.setChances(new Random().nextInt(6) + 1);
            player.setPlayerState(ADD);
            player.setGroup(group);
            group.getPlayers().add(player);
            groupService.save(group);

            group.getPlayers().forEach(
                    p -> sendService.send(
                            MessageUtilsService.sendMessage(
                                    p.getId(),
                                    player.getName() + " guruhga qo'shildi."
                            ),
                            "sendMessage"
                    )
            );

            if (playersSize + 1 == group.getPlayerCount()) {
                shuffleService.shuffle(group);
            }
        }
        else {
            sendService.send(
                    MessageUtilsService.sendMessage(
                            player.getId(),
                            "Guruh to'lgan. Yangi guruh yaratish uchun /start buyrug'ini bosing"
                    ),
                    "sendMessage"
            );
        }
    }
}
