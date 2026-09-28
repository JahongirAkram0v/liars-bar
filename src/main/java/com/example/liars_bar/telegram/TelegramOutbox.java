package com.example.liars_bar.telegram;

import com.example.liars_bar.common.PartitionedExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.LockSupport;

/**
 * Xabarlarni chat bo'yicha bo'lingan navbatlar orqali yuboradi:
 * bitta chatga tartib saqlanadi, umumiy tezlik Telegram limitidan oshmaydi,
 * 429/5xx/tarmoq xatolarida qayta uriniladi.
 */
@Component
public class TelegramOutbox implements TelegramSender {

    private static final Logger log = LoggerFactory.getLogger(TelegramOutbox.class);

    private static final int MAX_ATTEMPTS = 3;
    private static final int MESSAGES_PER_SECOND = 30;

    private final TelegramApi api;
    private final PartitionedExecutor executor = new PartitionedExecutor("tg-out", 8, 10_000);
    private final RateLimiter rateLimiter = new RateLimiter(MESSAGES_PER_SECOND);

    public TelegramOutbox(TelegramApi api) {
        this.api = api;
    }

    @Override
    public CompletableFuture<Long> send(long chatId, String text, Object replyMarkup) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        if (replyMarkup != null) {
            body.put("reply_markup", replyMarkup);
        }
        return submit(chatId, "sendMessage", body).thenApply(r -> r.path("message_id").asLong());
    }

    @Override
    public void edit(long chatId, long messageId, String text, Object replyMarkup) {
        if (messageId <= 0) {
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("message_id", messageId);
        body.put("text", text);
        if (replyMarkup != null) {
            body.put("reply_markup", replyMarkup);
        }
        submit(chatId, "editMessageText", body);
    }

    @Override
    public CompletableFuture<Long> sendSticker(long chatId, String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalStateException("sticker is not configured"));
        }
        return submit(chatId, "sendSticker", Map.of("chat_id", chatId, "sticker", fileId))
                .thenApply(r -> r.path("message_id").asLong());
    }

    @Override
    public void delete(long chatId, long messageId) {
        if (messageId <= 0) {
            return;
        }
        submit(chatId, "deleteMessage", Map.of("chat_id", chatId, "message_id", messageId));
    }

    @Override
    public void answerCallback(long chatId, String callbackQueryId, String alert) {
        Map<String, Object> body = new HashMap<>();
        body.put("callback_query_id", callbackQueryId);
        if (alert != null) {
            body.put("text", alert);
            body.put("show_alert", true);
        }
        submit(chatId, "answerCallbackQuery", body);
    }

    private CompletableFuture<JsonNode> submit(long chatId, String method, Map<String, Object> body) {
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        boolean accepted = executor.execute(chatId, () -> {
            try {
                future.complete(callWithRetry(method, body));
            } catch (TelegramException e) {
                if (!e.isNotModified()) {
                    log.warn("Telegram {} failed for chat {}: {}", method, chatId, e.getMessage());
                }
                future.completeExceptionally(e);
            } catch (RuntimeException e) {
                log.error("Telegram {} failed for chat {}", method, chatId, e);
                future.completeExceptionally(e);
            }
        });
        if (!accepted) {
            future.completeExceptionally(new IllegalStateException("outbox is full"));
        }
        return future;
    }

    private JsonNode callWithRetry(String method, Map<String, Object> body) {
        for (int attempt = 1; ; attempt++) {
            rateLimiter.acquire();
            try {
                return api.call(method, body);
            } catch (TelegramException e) {
                if (!e.isRetryable() || attempt >= MAX_ATTEMPTS) {
                    throw e;
                }
                long waitMillis = e.code() == 429
                        ? Math.max(1, e.retryAfterSeconds()) * 1000L
                        : 500L * attempt;
                LockSupport.parkNanos(waitMillis * 1_000_000L);
            }
        }
    }

    @PreDestroy
    public void close() {
        executor.close();
    }

    /** Chaqiruvlar orasida teng masofa saqlaydigan oddiy limitlovchi. */
    static final class RateLimiter {

        private final long intervalNanos;
        private long next = System.nanoTime();

        RateLimiter(int perSecond) {
            this.intervalNanos = 1_000_000_000L / perSecond;
        }

        void acquire() {
            long wait;
            synchronized (this) {
                long now = System.nanoTime();
                long at = Math.max(now, next);
                next = at + intervalNanos;
                wait = at - now;
            }
            if (wait > 0) {
                LockSupport.parkNanos(wait);
            }
        }
    }
}
