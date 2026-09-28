package com.example.liars_bar.bot;

import com.example.liars_bar.common.PartitionedExecutor;
import com.example.liars_bar.game.GameService;
import com.example.liars_bar.telegram.TelegramSender;
import com.example.liars_bar.telegram.Update;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Update'larni tekshiradi va o'yin buyruqlariga aylantiradi.
 * Bitta foydalanuvchining update'lari ketma-ket, turli foydalanuvchilarniki parallel ishlanadi.
 */
@Component
@RequiredArgsConstructor
public class UpdateRouter {

    private static final Pattern COUNT = Pattern.compile("x[2-4]");
    private static final Pattern CARD = Pattern.compile("[0-4]");
    private static final Pattern EMOJI = Pattern.compile("e[0-5]");
    private static final int MAX_NAME_CODE_POINTS = 15;
    private static final int MAX_PAYLOAD_LENGTH = 64;

    private final GameService games;
    private final TelegramSender tg;
    private final UserThrottle throttle = new UserThrottle(8, 4);
    private final PartitionedExecutor executor = new PartitionedExecutor("update", 4, 5_000);

    public void submit(Update update) {
        Update.User user = sender(update);
        if (user == null || user.isBot()) {
            return;
        }
        executor.execute(user.id(), () -> handle(update));
    }

    void handle(Update update) {
        if (update.callbackQuery() != null) {
            handleCallback(update.callbackQuery());
        } else if (update.message() != null) {
            handleMessage(update.message());
        }
    }

    private void handleMessage(Update.Message message) {
        String text = message.text();
        if (message.chat() == null || !message.chat().isPrivate() || text == null || !text.startsWith("/")) {
            return;
        }
        long userId = message.from().id();
        if (message.chat().id() != userId || !throttle.tryAcquire(userId)) {
            return;
        }

        String[] parts = text.trim().split("\\s+", 2);
        String command = parts[0];
        int at = command.indexOf('@');
        if (at > 0) {
            command = command.substring(0, at);
        }
        String name = displayName(message.from());

        switch (command) {
            case "/start" -> {
                String payload = parts.length > 1 && parts[1].length() <= MAX_PAYLOAD_LENGTH ? parts[1] : null;
                games.onStart(userId, name, payload);
            }
            case "/quit" -> games.onQuit(userId);
            default -> {
                // boshqa buyruqlar e'tiborsiz qoldiriladi
            }
        }
    }

    private void handleCallback(Update.CallbackQuery query) {
        long userId = query.from().id();
        Update.Message message = query.message();
        String data = query.data();
        if (message == null || message.chat() == null || message.chat().id() != userId || data == null) {
            tg.answerCallback(userId, query.id(), null);
            return;
        }
        if (!throttle.tryAcquire(userId)) {
            tg.answerCallback(userId, query.id(), null);
            return;
        }

        long messageId = message.messageId();
        String alert;
        if (COUNT.matcher(data).matches()) {
            alert = games.onCount(userId, displayName(query.from()), data.charAt(1) - '0', messageId);
        } else if (CARD.matcher(data).matches()) {
            alert = games.onCard(userId, data.charAt(0) - '0', messageId);
        } else if (EMOJI.matcher(data).matches()) {
            alert = games.onEmoji(userId, data.charAt(1) - '0', messageId);
        } else if (data.equals("l")) {
            alert = games.onLiar(userId, messageId);
        } else if (data.equals("t")) {
            alert = games.onThrow(userId, messageId);
        } else {
            alert = "SomeThing went wrong!!";
        }
        tg.answerCallback(userId, query.id(), alert);
    }

    private static Update.User sender(Update update) {
        if (update.callbackQuery() != null) {
            return update.callbackQuery().from();
        }
        if (update.message() != null) {
            return update.message().from();
        }
        return null;
    }

    /** Ism: boshqaruv belgilarisiz, ko'pi bilan 15 ta belgi. */
    static String displayName(Update.User user) {
        String raw = user.firstName() == null ? "" : user.firstName();
        StringBuilder name = new StringBuilder();
        raw.codePoints()
                .filter(cp -> !Character.isISOControl(cp))
                .limit(MAX_NAME_CODE_POINTS)
                .forEach(name::appendCodePoint);
        String result = name.toString().trim();
        return result.isEmpty() ? "Player" : result;
    }

    @PreDestroy
    public void close() {
        executor.close();
    }
}
