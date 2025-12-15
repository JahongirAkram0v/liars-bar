package com.example.liars_bar.command;

import com.example.liars_bar.botService.Bar;
import com.example.liars_bar.botService.Card;
import com.example.liars_bar.model.Action;
import com.example.liars_bar.model.Event;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.service.EventService;
import com.example.liars_bar.service.GroupService;
import com.example.liars_bar.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.example.liars_bar.model.Action.WIN;

@Component
@RequiredArgsConstructor
public class QuitCommand {

    private final PlayerService playerService;
    private final GroupService groupService;
    private final Bar bar;
    private final Card card;
    private final EventService eventService;

    public void execute(Player player) {

        Group group = player.getGroup();

        Event event = group.getEvent();
        if (event == null) {
            System.err.println("Player must have Quit event:" + player.getId());
            return;
        }
        Player pTemp = group.currentPlayer();
        groupService.resetEvent(group);
        eventService.delete(event);

        int index = player.getIndex();
        group.setTurn(index);
        group.setLI(index);
        group.removePlayer(index);
        playerService.reset(player);
        groupService.save(group);

        groupService.updateTurn(group);
        Player p = group.currentPlayer();
        Event newEvent = Event.builder()
                .action(Action.SHUFFLE)
                .endTime(Event.getMin())
                .build();
        group.setEvent(newEvent);
        groupService.save(group);

        boolean isAlone = group.isAlone();

        bar.executeP(player, "guruhni tark etdingiz.");
        card.executeText(player, "Yangidan boshlash uchun /start ni bosing.");

        if (isAlone) {
            Event win = Event.builder()
                    .action(WIN)
                    .endTime(Event.getMin())
                    .build();
            group.setEvent(win);
            groupService.save(group);
        }

        bar.execute(group);
        card.executeEmoji(pTemp);
        card.executeBid(p);
    }
}
