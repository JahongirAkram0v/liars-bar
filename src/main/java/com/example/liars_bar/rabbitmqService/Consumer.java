package com.example.liars_bar.rabbitmqService;

import com.example.liars_bar.botService.ReferralService;
import com.example.liars_bar.botService.Utils;
import com.example.liars_bar.command.*;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.model.PlayerState;
import com.example.liars_bar.model.Request;
import com.example.liars_bar.model.RequestCallback;
import com.example.liars_bar.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.example.liars_bar.config.RabbitQueue.*;
import static com.example.liars_bar.model.PlayerState.*;

@Service
@RequiredArgsConstructor
public class Consumer {

    private final PlayerService playerService;
    private final AnswerProducer answerProducer;

    private final ReferralService referralService;
    private final StartCommand startCommand;
    private final ExitCommand exitCommand;
    private final QuitCommand quitCommand;
    private final CountCommand countCommand;
    private final BarCommand barCommand;
    private final CardCommand cardCommand;
    private final LiarCommand liarCommand;
    private final ThrowCommand throwCommand;
    private final ChooseCommand chooseCommand;
    private final EmojiCommand emojiCommand;

    @RabbitListener(queues = REQUEST_MESSAGE_QUEUE)
    public void requestMessage(Request request) {

        Long id = request.id();
        String command = request.command();

        Optional<Player> optionalPlayer = playerService.findById(id);
        if (optionalPlayer.isEmpty()) {
            System.err.println("Player not found: " + id);
            System.out.println(request);
            return;
        }
        Player player = optionalPlayer.get();

        if (command.startsWith("/start")) {
            referralService.isReferral(command)
                    .ifPresentOrElse(
                            group -> referralService.referral(player, group),
                            () -> startCommand.execute(player.getId())
                    );
            return;
        }
        switch (command) {
            case "exit" -> exitCommand.execute(player);
            case "quit" -> quitCommand.execute(player);
        }

    }

    @RabbitListener(queues = REQUEST_CALLBACK_QUEUE)
    public void requestCallback(RequestCallback request) {

        Long id = request.id();
        String command = request.command();
        int messageId = request.messageId();
        String callbackQueryId = request.callbackQueryId();

        Optional<Player> optionalPlayer = playerService.findById(id);
        if (optionalPlayer.isEmpty()) {
            answerProducer.response(Utils.error(callbackQueryId, "Press /start"));
            return;
        }
        Player player = optionalPlayer.get();
        PlayerState state = player.getPlayerState();

        if (state == START) {
            if (command.startsWith("x")) {
                int count = command.charAt(1) - '0';
                countCommand.execute(player, count, messageId);
                return;
            }
        }

        if (state == ADD) {
            if (command.equals("bar")) {
                barCommand.execute(player, messageId);
                return;
            }
            if (command.equals("card")) {
                cardCommand.execute(player, messageId);
                return;
            }
        }

        if (state == GAME) {
            if (command.startsWith("e")) {
                int count = command.charAt(1) - '0';
                emojiCommand.execute(player, count);
                return;
            }
            if (command.equals("l")) {
                liarCommand.execute(player);
                return;
            }
            if (command.equals("t")) {
                throwCommand.execute(player, callbackQueryId);
                return;
            }
            if ("0,1,2,3,4".contains(command)) {
                chooseCommand.execute(player, Integer.parseInt(command));
                return;
            }
        }

        answerProducer.response(Utils.error(callbackQueryId, "SomeThing went wrong!!"));
    }

}
