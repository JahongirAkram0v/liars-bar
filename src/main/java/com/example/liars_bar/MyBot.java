package com.example.liars_bar;

import com.example.liars_bar.model.*;
import com.example.liars_bar.rabbitmqService.Producer;
import com.example.liars_bar.service.PlayerService;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.example.liars_bar.model.PlayerState.*;

@Component
@RequiredArgsConstructor
public class MyBot extends TelegramWebhookBot {

    private final Dotenv dotenv = Dotenv.load();
    private final String botUsername = dotenv.get("TELEGRAM_BOT_USERNAME");
    private final String botWebhookPath = dotenv.get("TELEGRAM_BOT_WEBHOOK_PATH");

    private final PlayerService playerService;

    private final Producer producer;

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {

        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();

            Long id = callbackQuery.getMessage().getChatId();
            String command = callbackQuery.getData();
            Integer messageId = callbackQuery.getMessage().getMessageId();
            String callbackQueryId = callbackQuery.getId();

            RequestCallback requestCallback = new RequestCallback(id, command, messageId, callbackQueryId);
            producer.requestCallback(requestCallback);

        }

        if (update.hasMessage() && update.getMessage().isCommand()) {
            Message message = update.getMessage();
            String command = message.getText();
            Long id = message.getChatId();

            String name = message.getFrom().getFirstName();

            Player player = playerService.findById(id)
                    .orElseGet(() -> {
                        Player p = Player.builder()
                                .id(id)
                                .name(firstNCodePoints(name))
                                .build();
                        playerService.save(p);
                        return p;
                    });

            if (!player.getName().equals(name)) {
                player.setName(firstNCodePoints(name));
                playerService.save(player);
            }

            PlayerState state = player.getPlayerState();

            if (state == START && command.startsWith("/start")) {
                producer.requestMessage(new Request(id, command));
                return null;
            }
            if (state == ADD && command.equals("/quit")) {
                producer.requestMessage(new Request(id, "exit"));
                return null;
            }
            if (state == GAME && command.equals("/quit")) {
                producer.requestMessage(new Request(id, "quit"));
                return null;
            }
            return null;
        }

        return null;
    }

    private static String firstNCodePoints(String s) {
        int codePointCount = s.codePointCount(0, s.length());
        int end = s.offsetByCodePoints(0, Math.min(codePointCount, 15));
        return s.substring(0, end);
    }

    @Override
    public String getBotPath() {
        return botWebhookPath;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }
}
