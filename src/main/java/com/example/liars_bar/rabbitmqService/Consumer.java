package com.example.liars_bar.rabbitmqService;

import com.example.liars_bar.botService.ReferralService;
import com.example.liars_bar.command.*;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.model.Request;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import static com.example.liars_bar.config.RabbitQueue.REQUEST_QUEUE;

@Service
@RequiredArgsConstructor
public class Consumer {

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

    @RabbitListener(queues = REQUEST_QUEUE)
    public void request(Request request) {

        Player player = request.player();
        String command = request.command();

        if (command.startsWith("/start")) {
            referralService.isReferral(command)
                    .ifPresentOrElse(
                            group -> referralService.referral(player, group),
                            () -> startCommand.execute(player.getId())
                    );
            return;
        }
        if (command.startsWith("c")) {
            int count = command.charAt(1) - '0';
            countCommand.execute(player, count);
            return;
        }
        switch (command) {
            case "exit" -> exitCommand.execute(player);
            case "quit" -> quitCommand.execute(player);
            case "bar" -> barCommand.execute(player, request.messageId());
            case "card" -> cardCommand.execute(player, request.messageId());
            case "l" -> liarCommand.execute(player);
            case "t" -> throwCommand.execute(player);
            default -> chooseCommand.execute(player, Integer.parseInt(command));
        }


    }

}
