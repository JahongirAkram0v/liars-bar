package com.example.liars_bar;

import com.example.liars_bar.botService.CallbackQueryService;
import com.example.liars_bar.botService.MessageUtilsService;
import com.example.liars_bar.botService.ReferralService;
import com.example.liars_bar.botService.SendService;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.service.PlayerService;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Map;

import static com.example.liars_bar.model.PlayerState.START;

@Component
@RequiredArgsConstructor
public class MyBot extends TelegramWebhookBot {

    private final Dotenv dotenv = Dotenv.load();
    private final String botUsername = dotenv.get("TELEGRAM_BOT_USERNAME");
    private final String botWebhookPath = dotenv.get("TELEGRAM_BOT_WEBHOOK_PATH");

    private final PlayerService playerService;
    private final ReferralService referralService;
    private final SendService sendService;
    private final CallbackQueryService callbackService;

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {

        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            Long currentPlayerId = update.getCallbackQuery().getMessage().getChatId();
            String name = update.getCallbackQuery().getFrom().getFirstName();
            System.out.println(callbackData + " " + currentPlayerId);

            Player player = playerService.findById(currentPlayerId)
                    .orElseGet(() -> {
                        extracted(currentPlayerId);
                        return Player.builder()
                                .id(currentPlayerId)
                                .name(name.substring(0, Math.min(name.length(), 15)))
                                .build();
                    });
            playerService.save(player);

            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            String callbackQueryId = update.getCallbackQuery().getId();
            callbackService.check(callbackData, messageId, player, callbackQueryId);

            return null;
        }

        if (!update.hasMessage() || !update.getMessage().isCommand()) {
            return null;
        }

        Message message = update.getMessage();
        String text = message.getText();
        Long currentPlayerId = message.getChatId();
        String name = message.getFrom().getFirstName();

        Player player = playerService.findById(currentPlayerId)
                .orElseGet(() -> Player.builder()
                        .id(currentPlayerId)
                        .name(name.substring(0, Math.min(name.length(), 15)))
                        .build());
        playerService.save(player);

        if (player.getPlayerState().equals(START) && text.startsWith("/start ")) {

            if (referralService.isReferral(text)) {
                referralService.referral(player);
                System.out.println("Group id : " + player.getGroup().getId());
            } else {
                extracted(currentPlayerId);
            }
            return null;
        }

        if (player.getPlayerState().equals(START) && text.equals("/start")) {
            extracted(currentPlayerId);
            return null;
        }

        return null;
    }

    private void extracted(Long currentPlayerId) {
        System.out.println("Sending player count selection to player: " + currentPlayerId);
        List<List<Map<String, Object>>> keyboards = List.of(
                List.of(
                        Map.of("text", "2", "callback_data", "c 2"),
                        Map.of("text", "3", "callback_data", "c 3"),
                        Map.of("text", "4", "callback_data", "c 4")
                )
        );

        sendService.send(
                MessageUtilsService.sendMessage(
                        currentPlayerId,
                        "O'yinchilar sonini tanlang!",
                        keyboards
                ),
                "sendMessage"
        );
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
