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

        Player pTemp = group.currentPlayer();
        Event event = pTemp.getEvent();
        if (event == null) {
            System.err.println("Player must have Quit event:" + player.getId());
            return;
        }
        pTemp.setEvent(null);
        playerService.save(pTemp);
        eventService.delete(event);

        int index = player.getIndex();
        if (pTemp.getIndex() != index) {
            group.setTurn(index);
            group.setLI(index);
        }
        group.removePlayer(index);
        playerService.reset(player);
        groupService.save(group);

        groupService.updateTurn(group);
        Player p = group.currentPlayer();
        Event newEvent = Event.builder()
                .action(Action.SHUFFLE)
                .endTime(Event.getMin())
                .build();
        p.setEvent(newEvent);
        playerService.save(p);

        boolean isAlone = group.isAlone();

        bar.executeP(player, "guruhni tark etdingiz.");
        card.execute(player, "Yangidan boshlash uchun /start ni bosing.");

        if (isAlone) {
            Event win = Event.builder()
                    .action(WIN)
                    .endTime(Event.getMin())
                    .build();
            p.setEvent(win);
            playerService.save(p);
        }

        bar.execute(group);
        card.executeE(pTemp);
        card.executeB(p);
    }
}
