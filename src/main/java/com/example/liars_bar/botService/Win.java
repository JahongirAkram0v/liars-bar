package com.example.liars_bar.botService;

import com.example.liars_bar.model.Event;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.rabbitmqService.AnswerProducer;
import com.example.liars_bar.service.EventService;
import com.example.liars_bar.service.GroupService;
import com.example.liars_bar.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.example.liars_bar.model.Which.NOTHING;

@Component
@RequiredArgsConstructor
public class Win {

    private final Bar bar;
    private final Card card;
    private final PlayerService playerService;
    private final EventService eventService;
    private final AnswerProducer answerProducer;
    private final GroupService groupService;

    public void execute(Group group) {
        Event event = group.getEvent();
        groupService.resetEvent(group);
        eventService.delete(event);
        group.getPlayers().values().stream().filter(player -> player.getSticker() != -1).forEach(
                p -> answerProducer.response(Utils.delete(p.getId(), p.getSticker()))
        );

        String name = group.currentPlayer().getName();

        bar.executeAll(group, name);
        card.executeAllText(group, "..");
        card.executeSticker(group, "CAACAgIAAxkBAAISKWk_2KEROgck7th2Q8BMwrNvhvEMAAIsjgACW7z4Sb-IZaGPsSucNgQ", NOTHING);

        String text = "O'yinni qayta boshlash uchun /start ni bosing!";
        List<Player> players = new ArrayList<>(group.getPlayers().values());
        for (Player p : players) {
            answerProducer.response(Utils.text(p.getId(), text, NOTHING));
            group.removePlayer(p.getIndex());
            playerService.reset(p);
        }

        groupService.delete(group);
    }
}
