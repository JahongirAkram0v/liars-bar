package com.example.liars_bar.bot;

import com.example.liars_bar.config.TelegramProperties;
import com.example.liars_bar.telegram.TelegramApi;
import com.example.liars_bar.telegram.TelegramException;
import com.example.liars_bar.telegram.Update;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Telegram'dan update'larni long polling ({@code getUpdates}) orqali oladi.
 * <p>
 * Bitta virtual thread Telegram'ga so'rov yuboradi va javobni {@value #POLL_TIMEOUT_SECONDS}
 * soniyagacha kutadi: yangi update kelishi bilan javob darhol qaytadi. Keyingi so'rovdagi
 * {@code offset} oldingi update'larni tasdiqlaydi, shuning uchun har bir update bir marta olinadi.
 * Tashqaridan kiruvchi ulanish yo'q: ochiq port, domen va sertifikat kerak emas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpdatePoller implements SmartLifecycle {

    static final int POLL_TIMEOUT_SECONDS = 50;
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(POLL_TIMEOUT_SECONDS + 15);
    private static final List<String> ALLOWED_UPDATES = List.of("message", "callback_query");
    private static final long MIN_BACKOFF_MILLIS = 1_000;
    private static final long MAX_BACKOFF_MILLIS = 30_000;

    private final TelegramApi api;
    private final UpdateRouter router;
    private final ObjectMapper mapper;
    private final TelegramProperties properties;

    private volatile boolean running;
    private Thread thread;
    private long offset;

    @Override
    public void start() {
        if (!properties.pollingEnabled()) {
            log.info("Telegram polling is disabled");
            return;
        }
        running = true;
        thread = Thread.ofVirtual().name("tg-poll").start(this::loop);
    }

    @Override
    public void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(Duration.ofSeconds(5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void loop() {
        log.info("Telegram polling started");
        deleteWebhook();
        long backoff = MIN_BACKOFF_MILLIS;
        while (running) {
            try {
                pollOnce();
                backoff = MIN_BACKOFF_MILLIS;
            } catch (TelegramException e) {
                if (!running) {
                    break;
                }
                long wait = backoff;
                switch (e.code()) {
                    case 409 -> {
                        // Webhook o'rnatilgan yoki boshqa nusxa ham polling qilyapti
                        log.error("getUpdates conflict: {}. Is another bot instance running?", e.getMessage());
                        deleteWebhook();
                    }
                    case 401, 404 -> log.error("Telegram rejected the bot token");
                    case 429 -> wait = Math.max(1, e.retryAfterSeconds()) * 1000L;
                    default -> log.warn("getUpdates failed: {}", e.getMessage());
                }
                sleep(wait);
                backoff = Math.min(backoff * 2, MAX_BACKOFF_MILLIS);
            } catch (RuntimeException e) {
                log.error("Polling failed", e);
                sleep(backoff);
                backoff = Math.min(backoff * 2, MAX_BACKOFF_MILLIS);
            }
        }
        log.info("Telegram polling stopped");
    }

    /** @return olingan update'lar soni */
    int pollOnce() {
        JsonNode result = api.call("getUpdates", Map.of(
                "offset", offset,
                "timeout", POLL_TIMEOUT_SECONDS,
                "allowed_updates", ALLOWED_UPDATES
        ), HTTP_TIMEOUT);

        int count = 0;
        for (JsonNode node : result) {
            long updateId = node.path("update_id").asLong();
            offset = Math.max(offset, updateId + 1);
            try {
                router.submit(mapper.treeToValue(node, Update.class));
                count++;
            } catch (JsonProcessingException | IllegalArgumentException e) {
                log.warn("Skipping malformed update {}", updateId);
            }
        }
        return count;
    }

    private void deleteWebhook() {
        try {
            api.call("deleteWebhook", Map.of("drop_pending_updates", false));
        } catch (TelegramException e) {
            log.warn("deleteWebhook failed: {}", e.getMessage());
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
