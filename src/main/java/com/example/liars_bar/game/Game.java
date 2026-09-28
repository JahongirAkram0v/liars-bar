package com.example.liars_bar.game;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

/**
 * Bitta o'yin holati. Barcha o'qish/yozish {@link #lock} ostida bajariladi.
 */
final class Game {

    final String id;
    final int capacity;
    final ReentrantLock lock = new ReentrantLock();

    /** O'rin raqami -> o'yinchi. Bo'sh o'rin null. */
    private final Seat[] seats;
    private int seatCount;

    Phase phase = Phase.LOBBY;
    int turn;
    char tableCard;
    List<Character> pile = new ArrayList<>();
    int lastThrower = -1;
    String lastThrowerName;

    boolean finished;
    long timerToken;
    GameScheduler.Cancellable timer;
    long startToken;
    boolean tableRefreshScheduled;

    Game(String id, int capacity) {
        this.id = id;
        this.capacity = capacity;
        this.seats = new Seat[capacity];
    }

    /** Band o'rinlar, o'rin raqami tartibida. */
    Iterable<Seat> seats() {
        return () -> new Iterator<>() {
            private int next = skipEmpty(0);

            @Override
            public boolean hasNext() {
                return next < seats.length;
            }

            @Override
            public Seat next() {
                if (next >= seats.length) {
                    throw new NoSuchElementException();
                }
                Seat seat = seats[next];
                next = skipEmpty(next + 1);
                return seat;
            }
        };
    }

    private int skipEmpty(int from) {
        while (from < seats.length && seats[from] == null) {
            from++;
        }
        return from;
    }

    /** @return o'rindagi o'yinchi; o'rin bo'sh yoki mavjud bo'lmasa null */
    Seat seat(int index) {
        return index >= 0 && index < seats.length ? seats[index] : null;
    }

    void put(Seat seat) {
        if (seats[seat.index] == null) {
            seatCount++;
        }
        seats[seat.index] = seat;
    }

    void remove(Seat seat) {
        if (seats[seat.index] == seat) {
            seats[seat.index] = null;
            seatCount--;
        }
    }

    int seatCount() {
        return seatCount;
    }

    boolean isEmpty() {
        return seatCount == 0;
    }

    Seat current() {
        return seat(turn);
    }

    Seat seatOf(long userId) {
        for (Seat seat : seats) {
            if (seat != null && seat.userId == userId) {
                return seat;
            }
        }
        return null;
    }

    int freeSeat() {
        for (int i = 0; i < seats.length; i++) {
            if (seats[i] == null) {
                return i;
            }
        }
        return -1;
    }

    Seat firstSeat() {
        int index = skipEmpty(0);
        return index < seats.length ? seats[index] : null;
    }

    long aliveCount() {
        long count = 0;
        for (Seat seat : seats) {
            if (seat != null && seat.alive) {
                count++;
            }
        }
        return count;
    }

    boolean isActiveAlone() {
        int count = 0;
        for (Seat seat : seats) {
            if (seat != null && aliveAndActive(seat)) {
                count++;
            }
        }
        return count == 1;
    }

    Seat firstAlive() {
        for (Seat seat : seats) {
            if (seat != null && seat.alive) {
                return seat;
            }
        }
        return null;
    }

    /**
     * {@code from} dan keyingi, shartga mos o'rin (aylanma). Hech kim bo'lmasa -1.
     */
    int nextAfter(int from, Predicate<Seat> eligible) {
        int n = seats.length;
        int start = Math.floorMod(from, n);
        for (int step = 1; step <= n; step++) {
            Seat seat = seats[(start + step) % n];
            if (seat != null && eligible.test(seat)) {
                return seat.index;
            }
        }
        return -1;
    }

    static boolean aliveAndActive(Seat seat) {
        return seat.alive && seat.active;
    }

    static boolean alive(Seat seat) {
        return seat.alive;
    }
}
