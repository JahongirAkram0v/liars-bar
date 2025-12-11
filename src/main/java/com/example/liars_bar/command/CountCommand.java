package com.example.liars_bar.command;

import com.example.liars_bar.botService.Utils;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.rabbitmqService.AnswerProducer;
import com.example.liars_bar.service.GroupService;
import com.example.liars_bar.service.PlayerService;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.example.liars_bar.model.PlayerState.ADD;
import static com.example.liars_bar.model.PlayerState.START;

@Component
@RequiredArgsConstructor
public class CountCommand {

    private final Dotenv dotenv = Dotenv.load();
    private final String telegramBotUsername = dotenv.get("TELEGRAM_BOT_USERNAME");

    private final AnswerProducer answerProducer;
    private final GroupService groupService;
    private final PlayerService playerService;

    public void execute(Player player, int count, int messageId) {

        Group group = new Group();
        group.setPC(count);
        groupService.save(group);

        player.setChances(new Random().nextInt(6) + 1);
        player.setPlayerIndex(0);
        player.setPlayerState(ADD);
        player.setGroup(group);
        playerService.save(player);


        String text = "O'yinni boshlash uchun dostlaringizga yuboring";
        List<List<Map<String, Object>>> keyboard = List.of(List.of(
                Map.of(
                        "text", "Join",
                        "url", "https://t.me/" + telegramBotUsername + "?start=" + group.getId()
                )
        ));
        answerProducer.response(Utils.editText(player.getId(), text, keyboard, messageId));

    }
}
