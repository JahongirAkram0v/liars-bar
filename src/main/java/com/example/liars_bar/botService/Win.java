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

@Component
@RequiredArgsConstructor
public class Win {

    private final Bar bar;
    private final Card card;
    private final PlayerService playerService;
    private final EventService eventService;
    private final AnswerProducer answerProducer;
    private final GroupService groupService;

    public void execute(Player player) {
        Group group = player.getGroup();

        bar.executeAll(group, player.getName());
        card.executeAll(group, "g'olib bo'ldi");

        String text = "O'yinni qayta boshlash uchun /start ni bosing!";
        group.getPlayersList().forEach(
                p -> {
                    answerProducer.response(Utils.text(player.getId(), text));
                    playerService.reset(player);
                }
        );
        Event event = player.getEvent();
        eventService.delete(event);
        groupService.delete(group);
    }
}
