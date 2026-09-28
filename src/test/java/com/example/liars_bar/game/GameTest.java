package com.example.liars_bar.game;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameTest {

    @Test
    void seatsSkipEmptySlotsAndKeepOrder() {
        Game game = new Game("g", 4);
        Seat a = new Seat(1, "A", 0, 1);
        Seat c = new Seat(3, "C", 2, 1);
        Seat d = new Seat(4, "D", 3, 1);
        game.put(d);
        game.put(a);
        game.put(c);

        assertThat(game.seatCount()).isEqualTo(3);
        assertThat(game.freeSeat()).isEqualTo(1);
        assertThat(indexes(game)).containsExactly(0, 2, 3);
        assertThat(game.seat(1)).isNull();
        assertThat(game.seat(-1)).isNull();
        assertThat(game.seat(4)).isNull();
        assertThat(game.seatOf(3)).isSameAs(c);

        game.remove(a);
        game.remove(a);
        assertThat(game.seatCount()).isEqualTo(2);
        assertThat(game.firstSeat()).isSameAs(c);
        assertThat(indexes(game)).containsExactly(2, 3);
    }

    @Test
    void nextAfterWrapsAroundAndSkipsIneligible() {
        Game game = new Game("g", 4);
        for (int i = 0; i < 4; i++) {
            game.put(new Seat(i + 1, "P" + i, i, 1));
        }
        game.seat(1).alive = false;
        game.remove(game.seat(2));

        assertThat(game.nextAfter(0, Game::alive)).isEqualTo(3);
        assertThat(game.nextAfter(3, Game::alive)).isEqualTo(0);
        assertThat(game.nextAfter(-1, Game::alive)).isEqualTo(0);

        game.seat(3).alive = false;
        assertThat(game.nextAfter(0, Game::alive)).isEqualTo(0);
        game.seat(0).alive = false;
        assertThat(game.nextAfter(0, Game::alive)).isEqualTo(-1);
    }

    private static List<Integer> indexes(Game game) {
        List<Integer> result = new ArrayList<>();
        game.seats().forEach(s -> result.add(s.index));
        return result;
    }
}
