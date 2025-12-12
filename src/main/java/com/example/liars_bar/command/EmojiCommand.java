package com.example.liars_bar.command;

import com.example.liars_bar.botService.Bar;
import com.example.liars_bar.botService.Card;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmojiCommand {

    private final PlayerService playerService;
    private final Bar bar;
    private final Card card;

    public void execute(Player player, int index) {
        Group group = player.getGroup();
        if (group.getTurn() == player.getPlayerIndex()) {
            return;
        }
        player.setEM(index);
        playerService.save(player);
        bar.execute(group, null);
        card.executeE(player);
    }
}
