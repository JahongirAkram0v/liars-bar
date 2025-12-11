package com.example.liars_bar.command;

import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuitCommand {

    private final PlayerService playerService;

    public void execute(Player player) {

        Group group = player.getGroup();
        playerService.reset(player.getId());
        // Additional logic for quitting can be added here
    }
}
