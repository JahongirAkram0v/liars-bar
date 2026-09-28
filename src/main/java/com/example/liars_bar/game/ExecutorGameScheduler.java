package com.example.liars_bar.game;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class ExecutorGameScheduler implements GameScheduler {

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2, r -> {
        Thread t = new Thread(r, "game-timer");
        t.setDaemon(true);
        return t;
    });

    public ExecutorGameScheduler() {
        executor.setRemoveOnCancelPolicy(true);
    }

    @Override
    public Cancellable schedule(Duration delay, Runnable task) {
        ScheduledFuture<?> future = executor.schedule(task, delay.toMillis(), TimeUnit.MILLISECONDS);
        return () -> future.cancel(false);
    }

    @PreDestroy
    public void close() {
        executor.shutdownNow();
    }
}
