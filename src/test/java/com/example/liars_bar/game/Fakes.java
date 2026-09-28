package com.example.liars_bar.game;

import com.example.liars_bar.telegram.TelegramSender;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

final class Fakes {

    private Fakes() {
    }

    record Out(long chatId, String method, long messageId, String text, Object markup) {
    }

    /** Barcha chiquvchi xabarlarni yozib oladi, message_id larni ketma-ket beradi. */
    static final class Sender implements TelegramSender {

        final List<Out> log = new CopyOnWriteArrayList<>();
        /** chat -> message_id -> oxirgi matn */
        final Map<Long, Map<Long, Out>> screen = new ConcurrentHashMap<>();
        private final AtomicLong ids = new AtomicLong(100);

        @Override
        public CompletableFuture<Long> send(long chatId, String text, Object replyMarkup) {
            long id = ids.incrementAndGet();
            record(new Out(chatId, "send", id, text, replyMarkup));
            return CompletableFuture.completedFuture(id);
        }

        @Override
        public void edit(long chatId, long messageId, String text, Object replyMarkup) {
            record(new Out(chatId, "edit", messageId, text, replyMarkup));
        }

        @Override
        public CompletableFuture<Long> sendSticker(long chatId, String fileId) {
            long id = ids.incrementAndGet();
            record(new Out(chatId, "sticker", id, fileId, null));
            return CompletableFuture.completedFuture(id);
        }

        @Override
        public void delete(long chatId, long messageId) {
            record(new Out(chatId, "delete", messageId, null, null));
        }

        @Override
        public void answerCallback(long chatId, String callbackQueryId, String alert) {
        }

        private void record(Out out) {
            log.add(out);
            screen.computeIfAbsent(out.chatId(), k -> new ConcurrentHashMap<>()).put(out.messageId(), out);
        }

        List<String> textsSentTo(long chatId) {
            return log.stream().filter(o -> o.chatId() == chatId && o.method().equals("send")).map(Out::text).toList();
        }

        boolean anyText(String text) {
            return log.stream().anyMatch(o -> text.equals(o.text()));
        }

        long winStickers() {
            return log.stream().filter(o -> o.method().equals("sticker") && "win".equals(o.text())).count();
        }

        String screenText(long chatId, long messageId) {
            Out out = screen.getOrDefault(chatId, Map.of()).get(messageId);
            return out == null ? null : out.text();
        }
    }

    /** Faza taymerlarini qo'lda ishga tushiradi; 1 soniyadan qisqa kechikishlar darhol bajariladi. */
    static final class ManualScheduler implements GameScheduler {

        final class Task implements Cancellable {
            final Duration delay;
            final Runnable runnable;
            boolean cancelled;

            Task(Duration delay, Runnable runnable) {
                this.delay = delay;
                this.runnable = runnable;
            }

            @Override
            public void cancel() {
                cancelled = true;
            }
        }

        final List<Task> tasks = new ArrayList<>();

        @Override
        public Cancellable schedule(Duration delay, Runnable task) {
            if (delay.compareTo(Duration.ofSeconds(1)) < 0) {
                task.run();
                return () -> {
                };
            }
            synchronized (this) {
                return enqueue(delay, task);
            }
        }

        private Cancellable enqueue(Duration delay, Runnable task) {
            Task t = new Task(delay, task);
            tasks.add(t);
            return t;
        }

        /** Eng oxirgi bekor qilinmagan taymerni bajaradi. */
        Duration runNext() {
            Task next;
            synchronized (this) {
                tasks.removeIf(t -> t.cancelled);
                if (tasks.isEmpty()) {
                    return null;
                }
                next = tasks.removeLast();
                tasks.clear();
            }
            next.runnable.run();
            return next.delay;
        }
    }
}
