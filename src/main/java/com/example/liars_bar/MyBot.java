package com.example.liars_bar;

import com.example.liars_bar.botService.*;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.model.PlayerState;
import com.example.liars_bar.service.GroupService;
import com.example.liars_bar.service.PlayerService;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

import static com.example.liars_bar.model.PlayerState.*;

@Component
@RequiredArgsConstructor
public class MyBot extends TelegramWebhookBot {

    private final Dotenv dotenv = Dotenv.load();
    private final String botUsername = dotenv.get("TELEGRAM_BOT_USERNAME");
    private final String botWebhookPath = dotenv.get("TELEGRAM_BOT_WEBHOOK_PATH");

    private final PlayerService playerService;
    private final GroupService groupService;
    private final ReferralService referralService;
    private final SendService sendService;
    private final CallbackQueryService callbackService;
    private final ShuffleService shuffleService;

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {

        if (update.hasCallbackQuery()) {

            String callbackData = update.getCallbackQuery().getData();
            Long id = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            String callbackQueryId = update.getCallbackQuery().getId();

            Optional<Player> optionalPlayer = playerService.findById(id);
            if (optionalPlayer.isEmpty()) {
                sendService.send(
                        MessageUtilsService.errorMessage(callbackQueryId),
                        "answerCallbackQuery"
                );
                return null;
            }
            Player player = optionalPlayer.get();

            callbackService.check(callbackData, messageId, player, callbackQueryId);

            return null;
        }

        if (!update.hasMessage() || !update.getMessage().isCommand()) {
            return null;
        }

        Message message = update.getMessage();
        String text = message.getText();
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

        if (player.getPlayerState().equals(START) && text.startsWith("/start")) {
            referralService.isReferral(text)
                    .ifPresentOrElse(
                            group -> referralService.referral(player, group),
                            () -> extracted(id)
                    );
            return null;
        }

        if (text.equals("/quit")) {

            Group group = player.getGroup();
            if (group == null) return null;
            extracted(id);

            PlayerState state = player.getPlayerState();

            if (state == ADD) {
                for (Player p: group.getPlayers()) {
                    sendService.send(
                            MessageUtilsService.sendMessage(
                                    p.getId(),
                                    player.getName() + " guruhni tark etdi"
                            ),
                            "sendMessage"
                    );
                }
            }

            group.getPlayers().remove(player);
            playerService.delete(player);
            if (group.getPlayers().isEmpty()) {
                groupService.delete(group);
            }
            if (state.equals(GAME)) {
                shuffleService.shuffle(group, new String[]{"", ""});
            }

            return null;
        }

        return null;
    }

    private void extracted(Long id) {
        sendService.send(
                MessageUtilsService.start(id),
                "sendMessage"
        );
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
