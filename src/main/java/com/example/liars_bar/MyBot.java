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
            Long currentPlayerId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            String callbackQueryId = update.getCallbackQuery().getId();

            Optional<Player> optionalPlayer = playerService.findById(currentPlayerId);
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
        Long currentPlayerId = message.getChatId();
        String name = message.getFrom().getFirstName();

        Player player = playerService.findById(currentPlayerId)
                .orElseGet(() -> Player.builder()
                        .id(currentPlayerId)
                        .name(firstNCodePoints(name))
                        .build());
        playerService.save(player);

        if (player.getPlayerState().equals(START) && text.startsWith("/start ")) {

            Optional<Group> optionalGroup = referralService.isReferral(text);
            if (optionalGroup.isPresent()) {
                referralService.referral(player, optionalGroup.get());
            } else {
                extracted(currentPlayerId);
            }
            return null;
        }

        if (player.getPlayerState().equals(START) && text.equals("/start")) {
            extracted(currentPlayerId);
            return null;
        }

        if (text.equals("/quit")) {

            Group group = player.getGroup();
            if (group == null) {
                sendService.send(
                        MessageUtilsService.sendMessage(
                                player.getId(),
                                "Siz hech qanday guruhda emassiz"
                        ),
                        "sendMessage"
                );
                return null;
            }

            PlayerState state = player.getPlayerState();

            String textP = "Siz guruhni tark etdingiz";
            String textG = player.getName() + " guruhni tark etdi";

            sendService.send(
                    MessageUtilsService.sendMessage(
                            player.getId(),
                            textP
                    ),
                    "sendMessage"
            );

            if (state == ADD) {
                for (Player p: group.getPlayers()) {
                    if (!p.equals(player)) {
                        sendService.send(
                                MessageUtilsService.sendMessage(
                                        p.getId(),
                                        textG
                                ),
                                "sendMessage"
                        );
                    }
                }
            }

            group.getPlayers().remove(player);
            playerService.delete(player);
            if (group.getPlayers().isEmpty()) {
                groupService.delete(group);
            }
            if (state.equals(GAME)) {
                shuffleService.shuffle(group, new String[]{"", "", textG});
            }

            return null;
        }

        return null;
    }

    private void extracted(Long currentPlayerId) {
        sendService.send(
                MessageUtilsService.start(currentPlayerId),
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
