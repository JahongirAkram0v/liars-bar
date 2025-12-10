package com.example.liars_bar.command;

import com.example.liars_bar.botService.Utils;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.rabbitmqService.AnswerProducer;
import com.example.liars_bar.service.GroupService;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.example.liars_bar.model.PlayerState.ADD;

@Component
@RequiredArgsConstructor
public class CountCommand {

    private final Dotenv dotenv = Dotenv.load();
    private final String telegramBotUsername = dotenv.get("TELEGRAM_BOT_USERNAME");

    private final AnswerProducer answerProducer;
    private final GroupService groupService;

    public void execute(Player player, int count) {

        Group group = new Group();
        group.setPC(count);

        player.setChances(new Random().nextInt(6) + 1);
        player.setPlayerIndex(0);
        player.setPlayerState(ADD);
        player.setGroup(group);

        groupService.save(group);


        String text = "Send this link to your friends to join the game";
        String keyboardText = "Play";
        List<List<Map<String, Object>>> keyboard = List.of(
                List.of(Map.of(
                        "text", keyboardText,
                        "url", "https://t.me/" + telegramBotUsername + "?start=" + group.getId()))
        );
        answerProducer.response(Utils.text(player.getId(), text, keyboard));
    }
}
