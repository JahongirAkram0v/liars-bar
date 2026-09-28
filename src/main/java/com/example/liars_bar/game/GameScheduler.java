package com.example.liars_bar.game;

import java.time.Duration;

public interface GameScheduler {

    Cancellable schedule(Duration delay, Runnable task);

    interface Cancellable {
        void cancel();
    }
}
