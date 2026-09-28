package com.example.liars_bar.telegram;

import com.example.liars_bar.common.PartitionedExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;
import java.util.concurrent.locks.LockSupport;

/**
 * Xabarlarni chat bo'yicha bo'lingan navbatlar orqali yuboradi:
 * bitta chatga tartib saqlanadi, umumiy tezlik Telegram limitidan oshmaydi,
 * 429/5xx/tarmoq xatolarida qayta uriniladi.
 * <p>
 * Bitta xabarga navbatda kutayotgan tahrir bo'lsa, yangi tahrir uning o'rnini egallaydi:
 * Telegram'ga faqat oxirgi holat yuboriladi. Callback tasdiqlari limitga kirmaydi va
 * navbatni kutmasdan darhol yuboriladi.
 * <p>
 * Telegram 403 qaytarsa (bot bloklangan), {@link ChatBlockedEvent} e'lon qilinadi.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramOutbox implements TelegramSender {

    private static final int MAX_ATTEMPTS = 3;
    private static final int MESSAGES_PER_SECOND = 30;

    private final TelegramApi api;
    private final ApplicationEventPublisher events;
    private final PartitionedExecutor executor = new PartitionedExecutor("tg-out", 8, 10_000);
    private final RateLimiter rateLimiter = new RateLimiter(MESSAGES_PER_SECOND);
    private final ExecutorService callbackExecutor = Executors.newVirtualThreadPerTaskExecutor();
    /** Hali yuborilmagan tahrirlar: (chat, xabar) -> eng oxirgi body. */
    private final Map<EditKey, Map<String, Object>> pendingEdits = new ConcurrentHashMap<>();

    private record EditKey(long chatId, long messageId) {
    }

    @Override
    public CompletableFuture<Long> send(long chatId, String text, Object replyMarkup) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        if (replyMarkup != null) {
            body.put("reply_markup", replyMarkup);
        }
        return submit(chatId, "sendMessage", () -> body, null).thenApply(r -> r.path("message_id").asLong());
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
        EditKey key = new EditKey(chatId, messageId);
        if (pendingEdits.put(key, body) == null) {
            // Navbatda shu xabar uchun vazifa yo'q: yangisini qo'yamiz. Vazifa bajarilganda
            // shu paytgacha kelgan eng oxirgi body'ni oladi.
            submit(chatId, "editMessageText", () -> pendingEdits.remove(key), () -> pendingEdits.remove(key));
        }
    }

    @Override
    public CompletableFuture<Long> sendSticker(long chatId, String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalStateException("sticker is not configured"));
        }
        Map<String, Object> body = Map.of("chat_id", chatId, "sticker", fileId);
        return submit(chatId, "sendSticker", () -> body, null)
                .thenApply(r -> r.path("message_id").asLong());
    }

    @Override
    public void delete(long chatId, long messageId) {
        if (messageId <= 0) {
            return;
        }
        Map<String, Object> body = Map.of("chat_id", chatId, "message_id", messageId);
        submit(chatId, "deleteMessage", () -> body, null);
    }

    @Override
    public void answerCallback(long chatId, String callbackQueryId, String alert) {
        Map<String, Object> body = new HashMap<>();
        body.put("callback_query_id", callbackQueryId);
        if (alert != null) {
            body.put("text", alert);
            body.put("show_alert", true);
        }
        try {
            callbackExecutor.execute(() -> {
                try {
                    api.call("answerCallbackQuery", body);
                } catch (TelegramException e) {
                    log.debug("answerCallbackQuery failed for chat {}: {}", chatId, e.getMessage());
                }
            });
        } catch (RejectedExecutionException e) {
            log.debug("answerCallbackQuery dropped during shutdown");
        }
    }

    /**
     * @param body       vazifa bajarilayotganda olinadi; null bo'lsa so'rov yuborilmaydi
     * @param onRejected navbat to'lib vazifa qabul qilinmasa chaqiriladi
     */
    private CompletableFuture<JsonNode> submit(long chatId, String method,
                                               Supplier<Map<String, Object>> body, Runnable onRejected) {
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        boolean accepted = executor.execute(chatId, () -> {
            Map<String, Object> request = body.get();
            if (request == null) {
                future.complete(null);
                return;
            }
            try {
                future.complete(callWithRetry(method, request));
            } catch (TelegramException e) {
                if (e.isForbidden()) {
                    log.info("Chat {} is unreachable: {}", chatId, e.getMessage());
                    publishBlocked(chatId);
                } else if (!e.isNotModified()) {
                    log.warn("Telegram {} failed for chat {}: {}", method, chatId, e.getMessage());
                }
                future.completeExceptionally(e);
            } catch (RuntimeException e) {
                log.error("Telegram {} failed for chat {}", method, chatId, e);
                future.completeExceptionally(e);
            }
        });
        if (!accepted) {
            if (onRejected != null) {
                onRejected.run();
            }
            future.completeExceptionally(new IllegalStateException("outbox is full"));
        }
        return future;
    }

    private void publishBlocked(long chatId) {
        try {
            events.publishEvent(new ChatBlockedEvent(chatId));
        } catch (RuntimeException e) {
            log.error("ChatBlockedEvent handling failed for chat {}", chatId, e);
        }
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
        callbackExecutor.shutdown();
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
