package com.example.liars_bar.game;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

/**
 * Bitta o'yin holati. Barcha o'qish/yozish {@link #lock} ostida bajariladi.
 */
@RequiredArgsConstructor
final class Game {

    final String id;
    final int capacity;
    final ReentrantLock lock = new ReentrantLock();

    /** O'rin raqami -> o'yinchi (o'rin raqami bo'yicha tartiblangan). */
    final TreeMap<Integer, Seat> seats = new TreeMap<>();

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

    Collection<Seat> seats() {
        return seats.values();
    }

    Seat current() {
        return seats.get(turn);
    }

    Seat seatOf(long userId) {
        for (Seat seat : seats.values()) {
            if (seat.userId == userId) {
                return seat;
            }
        }
        return null;
    }

    int freeSeat() {
        for (int i = 0; i < capacity; i++) {
            if (!seats.containsKey(i)) {
                return i;
            }
        }
        return -1;
    }

    long aliveCount() {
        return seats.values().stream().filter(s -> s.alive).count();
    }

    boolean isActiveAlone() {
        return seats.values().stream().filter(s -> s.alive && s.active).count() == 1;
    }

    Seat firstAlive() {
        return seats.values().stream().filter(s -> s.alive).findFirst().orElse(null);
    }

    /**
     * {@code from} dan keyingi, shartga mos o'rin (aylanma). Hech kim bo'lmasa -1.
     */
    int nextAfter(int from, Predicate<Seat> eligible) {
        Integer first = null;
        for (Seat seat : seats.values()) {
            if (!eligible.test(seat)) {
                continue;
            }
            if (seat.index > from) {
                return seat.index;
            }
            if (first == null) {
                first = seat.index;
            }
        }
        return first == null ? -1 : first;
    }

    static boolean aliveAndActive(Seat seat) {
        return seat.alive && seat.active;
    }

    static boolean alive(Seat seat) {
        return seat.alive;
    }
}
