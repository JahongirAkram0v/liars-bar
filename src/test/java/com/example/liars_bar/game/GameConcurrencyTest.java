package com.example.liars_bar.game;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ko'p o'yinchi bir vaqtda tugma bosganda va taymerlar parallel ishlaganda
 * o'yin holati buzilmasligini tekshiradi.
 */
class GameConcurrencyTest {

    private final ExecutorGameScheduler scheduler = new ExecutorGameScheduler();

    @AfterEach
    void tearDown() {
        scheduler.close();
    }

    @Test
    void parallelActionsAndTimersNeverBreakGames() throws Exception {
        Fakes.Sender tg = new Fakes.Sender();
        Fakes.Store store = new Fakes.Store();
        GameService service = new GameService(tg, scheduler, store, new SecureRandom(), new GameService.Settings(
                "liars_bar_bot", "death", "survive", "win",
                Duration.ofMillis(15), Duration.ofMillis(2), Duration.ofSeconds(5), Duration.ofMinutes(5),
                Duration.ofMillis(1)));

        int gamesCount = 25;
        List<Long> users = new ArrayList<>();
        for (int g = 0; g < gamesCount; g++) {
            long base = 1_000L * (g + 1);
            service.onCount(base, "P" + base, 4, 1);
            String id = service.gameOf(base).id;
            users.add(base);
            for (int i = 1; i < 4; i++) {
                users.add(base + i);
                service.onStart(base + i, "P" + (base + i), id);
            }
        }

        ExecutorService pool = Executors.newFixedThreadPool(16);
        for (long user : users) {
            pool.submit(() -> {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                long deadline = System.currentTimeMillis() + 20_000;
                while (System.currentTimeMillis() < deadline) {
                    Game game = service.gameOf(user);
                    if (game == null) {
                        return;
                    }
                    Seat seat = game.seatOf(user);
                    long messageId = seat == null ? 0 : seat.cardMessageId;
                    switch (rnd.nextInt(10)) {
                        case 0, 1, 2 -> service.onCard(user, rnd.nextInt(5), messageId);
                        case 3, 4 -> service.onThrow(user, messageId);
                        case 5 -> service.onLiar(user, messageId);
                        case 6 -> service.onEmoji(user, rnd.nextInt(6), messageId);
                        default -> Thread.onSpinWait();
                    }
                    try {
                        Thread.sleep(rnd.nextInt(3));
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            });
        }
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        long deadline = System.currentTimeMillis() + 5_000;
        while (store.winners.size() < gamesCount && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertThat(tg.anyText(Texts.GAME_ERROR)).isFalse();
        assertThat(service.activeGames()).isZero();
        assertThat(store.winners).hasSize(gamesCount);
    }
}
