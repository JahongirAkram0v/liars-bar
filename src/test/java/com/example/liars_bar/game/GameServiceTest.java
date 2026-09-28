package com.example.liars_bar.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class GameServiceTest {

    private static final Duration TURN = Duration.ofSeconds(35);
    private static final Duration SHORT = Duration.ofSeconds(5);

    private Fakes.Sender tg;
    private Fakes.ManualScheduler timers;
    private Fakes.Store store;
    private GameService service;

    @BeforeEach
    void setUp() {
        tg = new Fakes.Sender();
        timers = new Fakes.ManualScheduler();
        store = new Fakes.Store();
        service = new GameService(tg, timers, store, new Random(42), new GameService.Settings(
                "liars_bar_bot", "death", "survive", "win",
                TURN, SHORT, Duration.ofSeconds(60), Duration.ofHours(1), Duration.ofMillis(300)));
    }

    /** O'yin yaratadi, qolganlarni qo'shadi va birinchi raundni tarqatadi. */
    private Game startGame(long... users) {
        service.onStart(users[0], "P" + users[0], null);
        assertThat(service.onCount(users[0], "P" + users[0], users.length, 1)).isNull();
        Game game = service.gameOf(users[0]);
        for (int i = 1; i < users.length; i++) {
            service.onStart(users[i], "P" + users[i], game.id);
        }
        // Xabarlar darhol tayyor kontent bilan yuboriladi: message_id lar kelishi bilan yurish boshlanadi
        assertThat(game.phase).isEqualTo(Phase.TURN);
        return game;
    }

    private static Seat seat(Game game, long userId) {
        return game.seatOf(userId);
    }

    private static Seat withChances(Game game, long userId, int chances) {
        Seat old = seat(game, userId);
        Seat seat = new Seat(old.userId, old.name, old.index, chances);
        seat.cards = old.cards;
        seat.barMessageId = old.barMessageId;
        seat.cardMessageId = old.cardMessageId;
        game.seats.put(seat.index, seat);
        return seat;
    }

    @Test
    void roundDealsFiveCardsFromTheDeck() {
        Game game = startGame(1, 2, 3, 4);

        List<Character> all = new ArrayList<>();
        for (Seat s : game.seats()) {
            assertThat(s.cards).hasSize(5);
            assertThat(s.barMessageId).isPositive();
            assertThat(s.cardMessageId).isPositive();
            all.addAll(s.cards);
        }
        assertThat(all).containsExactlyInAnyOrderElementsOf(GameService.newDeck());
        assertThat(game.turn).isZero();
        assertThat("AKQ").contains(String.valueOf(game.tableCard));

        Seat first = game.current();
        assertThat(tg.screenText(first.userId, first.cardMessageId)).isEqualTo(Texts.YOUR_TURN);
        Seat other = seat(game, 2);
        assertThat(tg.screenText(2, other.cardMessageId)).isEqualTo(Texts.hand(other.cards));
        assertThat(tg.screenText(2, other.barMessageId)).isEqualTo(Texts.table(game));
        // BAR va CARD xabarlari tayyor holatda yuboriladi: start paytida tahrir yo'q
        assertThat(tg.log).noneMatch(o -> o.method().equals("edit") && o.messageId() > 1);
    }

    @Test
    void handsAreIndependentBetweenGames() {
        Game a = startGame(1, 2);
        Game b = startGame(3, 4);
        List<Character> before = new ArrayList<>(seat(a, 1).cards);

        timers.tasks.clear();
        // b da yangi raund a ning kartalariga ta'sir qilmasligi kerak
        service.onCard(3, 0, seat(b, 3).cardMessageId);
        service.onThrow(3, seat(b, 3).cardMessageId);

        assertThat(seat(a, 1).cards).isEqualTo(before);
    }

    @Test
    void throwMovesPileAndTurn() {
        Game game = startGame(1, 2, 3);
        Seat p1 = seat(game, 1);

        assertThat(service.onThrow(1, p1.cardMessageId)).isEqualTo(Texts.ALERT_PRESS_CARD);
        service.onCard(1, 0, p1.cardMessageId);
        service.onCard(1, 2, p1.cardMessageId);
        service.onCard(1, 2, p1.cardMessageId); // tanlov bekor qilindi
        service.onCard(1, 4, p1.cardMessageId);
        List<Character> expected = List.of(p1.cards.get(0), p1.cards.get(4));

        service.onThrow(1, p1.cardMessageId);

        assertThat(game.pile).isEqualTo(expected);
        assertThat(p1.cards).hasSize(3);
        assertThat(game.lastThrower).isEqualTo(p1.index);
        assertThat(game.turn).isEqualTo(seat(game, 2).index);
        assertThat(Texts.table(game)).contains("❗️P1 🃏x2");
    }

    @Test
    void outOfTurnAndStaleButtonsAreIgnored() {
        Game game = startGame(1, 2);
        Seat p1 = seat(game, 1);
        Seat p2 = seat(game, 2);

        assertThat(service.onCard(2, 0, p2.cardMessageId)).isNull();
        assertThat(p2.selected).isEmpty();
        service.onCard(1, 0, 999); // eski xabar tugmasi
        assertThat(p1.selected).isEmpty();
        service.onThrow(1, 999);
        assertThat(game.pile).isEmpty();
        assertThat(service.onLiar(1, p1.cardMessageId)).isEqualTo(Texts.ALERT_PRESS_CARD);
    }

    @Test
    void liarPunishesTheThrowerWhenHeLied() {
        Game game = startGame(1, 2);
        Seat p1 = seat(game, 1);
        game.tableCard = 'A';
        p1.cards = new ArrayList<>(List.of('K', 'A', 'A', 'A', 'A'));

        service.onCard(1, 0, p1.cardMessageId);
        service.onThrow(1, p1.cardMessageId);
        service.onLiar(2, seat(game, 2).cardMessageId);

        assertThat(game.phase).isEqualTo(Phase.REVEAL);
        assertThat(game.turn).isEqualTo(p1.index);
        assertThat(tg.screenText(2, seat(game, 2).cardMessageId)).isEqualTo(" K \n🟥");
        assertThat(tg.screenText(2, seat(game, 2).barMessageId)).isEqualTo("🃏 : A | P2 ishonmadi");
    }

    @Test
    void liarPunishesTheCallerWhenThrowerWasHonest() {
        Game game = startGame(1, 2);
        Seat p1 = seat(game, 1);
        Seat p2 = seat(game, 2);
        game.tableCard = 'Q';
        p1.cards = new ArrayList<>(List.of('Q', 'J', 'A', 'A', 'A'));

        service.onCard(1, 0, p1.cardMessageId);
        service.onCard(1, 1, p1.cardMessageId);
        service.onThrow(1, p1.cardMessageId);
        service.onLiar(2, p2.cardMessageId);

        assertThat(game.turn).isEqualTo(p2.index);
        int attemptsBefore = p2.attempt;
        assertThat(timers.runNext()).isEqualTo(SHORT); // o'q
        assertThat(p2.attempt + (p2.alive ? 0 : 1)).isEqualTo(attemptsBefore + 1);
    }

    @Test
    void shootKillsOnTheLastChamberAndLastAliveWins() {
        Game game = startGame(1, 2);
        Seat p1 = seat(game, 1);
        game.tableCard = 'A';
        p1.cards = new ArrayList<>(List.of('K', 'A', 'A', 'A', 'A'));
        p1.attempt = p1.chances - 1;

        service.onCard(1, 0, p1.cardMessageId);
        service.onThrow(1, p1.cardMessageId);
        service.onLiar(2, seat(game, 2).cardMessageId);
        timers.runNext(); // o'q

        assertThat(p1.alive).isFalse();
        assertThat(game.phase).isEqualTo(Phase.FINISHING);
        assertThat(tg.log).anyMatch(o -> o.method().equals("sticker") && o.text().equals("death"));

        timers.runNext(); // g'olib
        assertThat(game.finished).isTrue();
        assertThat(service.isPlaying(1)).isFalse();
        assertThat(service.isPlaying(2)).isFalse();
        assertThat(tg.textsSentTo(1)).contains(Texts.RESTART);
        assertThat(tg.screenText(1, p1.barMessageId)).isEqualTo("P2");
        awaitStats();
        assertThat(store.winners).containsExactly(2L);
    }

    @Test
    void survivingShotStartsNewRoundFromNextPlayer() {
        Game game = startGame(1, 2, 3);
        Seat p1 = withChances(game, 1, 6); // birinchi o'qda tirik qoladi
        game.tableCard = 'A';
        p1.cards = new ArrayList<>(List.of('K', 'A', 'A', 'A', 'A'));

        service.onCard(1, 0, p1.cardMessageId);
        service.onThrow(1, p1.cardMessageId);
        service.onLiar(2, seat(game, 2).cardMessageId);
        timers.runNext(); // o'q

        assertThat(p1.alive).isTrue();
        assertThat(p1.attempt).isEqualTo(1);
        assertThat(game.phase).isEqualTo(Phase.ROUND_OVER);
        assertThat(game.turn).isEqualTo(seat(game, 2).index);

        timers.runNext(); // yangi raund
        assertThat(game.phase).isEqualTo(Phase.TURN);
        assertThat(game.pile).isEmpty();
        game.seats().forEach(s -> assertThat(s.cards).hasSize(5));
        assertThat(tg.log).anyMatch(o -> o.method().equals("delete"));
    }

    @Test
    void turnTimeoutThrowsFirstCard() {
        Game game = startGame(1, 2);
        Seat p1 = seat(game, 1);
        char first = p1.cards.getFirst();

        assertThat(timers.runNext()).isEqualTo(TURN);

        assertThat(game.pile).containsExactly(first);
        assertThat(p1.cards).hasSize(4);
        assertThat(game.turn).isEqualTo(seat(game, 2).index);
    }

    @Test
    void onlyActivePlayerLeftMustCallLiar() {
        Game game = startGame(1, 2);
        Seat p1 = seat(game, 1);
        Seat p2 = seat(game, 2);
        game.tableCard = 'A';
        p1.cards = new ArrayList<>(List.of('A', 'A', 'A', 'A', 'A'));
        for (int i = 0; i < 5; i++) {
            service.onCard(1, i, p1.cardMessageId);
        }
        service.onThrow(1, p1.cardMessageId);
        assertThat(p1.active).isFalse();
        assertThat(tg.screenText(1, p1.cardMessageId)).isEqualTo(Texts.NO_CARDS_LEFT);

        // P2 yolg'iz faol: karta tanlash ham "Liar" hisoblanadi
        service.onCard(2, 0, p2.cardMessageId);

        assertThat(game.phase).isEqualTo(Phase.REVEAL);
        assertThat(game.turn).isEqualTo(p2.index);
    }

    @Test
    void unchangedMessagesAreNotSentAgain() {
        Game game = startGame(1, 2);
        long before = tg.log.stream().filter(o -> o.method().equals("edit")).count();

        game.lock.lock();
        try {
            service.updateTable(game);
            service.updateTable(game);
        } finally {
            game.lock.unlock();
        }
        service.onCard(1, 0, seat(game, 1).cardMessageId);
        service.onCard(1, 0, seat(game, 1).cardMessageId); // tanlov bekor: klaviatura o'zgaradi

        long after = tg.log.stream().filter(o -> o.method().equals("edit")).count();
        assertThat(after - before).isEqualTo(2);
    }

    @Test
    void emojiIsIgnoredForCurrentPlayer() {
        Game game = startGame(1, 2);
        Seat p1 = seat(game, 1);
        Seat p2 = seat(game, 2);

        service.onEmoji(1, 3, p1.cardMessageId);
        assertThat(p1.emoji).isZero();
        assertThat(tg.screenText(1, p1.cardMessageId)).isEqualTo(Texts.YOUR_TURN);

        service.onEmoji(2, 3, p2.cardMessageId);
        assertThat(p2.emoji).isEqualTo(3);
        assertThat(tg.screenText(1, p1.barMessageId)).contains("P2 - (0/6)  | 🃏x5 /" + Texts.EMOJIS.get(3));
    }

    @Test
    void quitWithTwoPlayersMakesTheOtherWin() {
        Game game = startGame(1, 2);

        service.onQuit(1);

        assertThat(service.isPlaying(1)).isFalse();
        assertThat(game.phase).isEqualTo(Phase.FINISHING);
        timers.runNext();
        assertThat(game.finished).isTrue();
        awaitStats();
        assertThat(store.winners).containsExactly(2L);
    }

    @Test
    void quitRestartsRoundAndDeadSpectatorQuitDoesNot() {
        Game game = startGame(1, 2, 3);
        seat(game, 3).alive = false;
        seat(game, 3).active = false;

        service.onQuit(3);
        assertThat(game.phase).isEqualTo(Phase.TURN);
        assertThat(game.seats).hasSize(2);

        service.onQuit(1);
        assertThat(game.phase).isEqualTo(Phase.FINISHING);
    }

    @Test
    void quitDuringRoundWithThreeAlivePlayersReshuffles() {
        Game game = startGame(1, 2, 3);

        service.onQuit(1);

        assertThat(game.phase).isEqualTo(Phase.ROUND_OVER);
        assertThat(game.turn).isEqualTo(seat(game, 2).index);
        timers.runNext();
        assertThat(game.phase).isEqualTo(Phase.TURN);
        game.seats().forEach(s -> assertThat(s.cards).hasSize(5));
    }

    @Test
    void lobbyRules() {
        service.onCount(1, "P1", 2, 1);
        Game game = service.gameOf(1);

        assertThat(service.onCount(1, "P1", 3, 1)).isEqualTo(Texts.ALERT_ERROR);
        service.onStart(1, "P1", game.id); // o'z guruhiga qayta qo'shilmaydi
        assertThat(game.seats).hasSize(1);

        service.onQuit(1);
        assertThat(service.activeGames()).isZero();

        service.onStart(2, "P2", game.id); // o'chirilgan o'yin: yangi o'yin menyusi
        assertThat(tg.textsSentTo(2)).containsExactly(Texts.CHOOSE_COUNT);
    }

    @Test
    void cannotJoinStartedGame() {
        Game game = startGame(1, 2);

        service.onStart(3, "P3", game.id);

        assertThat(service.isPlaying(3)).isFalse();
        assertThat(tg.textsSentTo(3)).containsExactly(Texts.CANNOT_JOIN);
    }

    @Test
    void leavingWhileStartingReturnsToLobby() {
        service.onCount(1, "P1", 2, 1);
        Game game = service.gameOf(1);
        timers.tasks.clear();
        // STARTING paytida chiqish (xabarlar hali yaratilmoqda deb faraz qilamiz)
        game.lock.lock();
        try {
            game.phase = Phase.STARTING;
            game.seats.put(1, new Seat(2, "P2", 1, 3));
        } finally {
            game.lock.unlock();
        }
        service.onQuit(1);

        assertThat(game.phase).isEqualTo(Phase.LOBBY);
        assertThat(game.seats).hasSize(1);
        assertThat(timers.runNext()).isEqualTo(Duration.ofHours(1));
        assertThat(game.finished).isTrue();
    }

    @Test
    void fullGameAlwaysEndsWithOneWinner() {
        for (int round = 0; round < 30; round++) {
            setUp();
            Game game = startGame(1, 2, 3, 4);
            int guard = 0;
            while (!game.finished && guard++ < 2_000) {
                if (game.phase == Phase.TURN && !game.pile.isEmpty() && guard % 3 == 0) {
                    Seat current = game.current();
                    service.onLiar(current.userId, current.cardMessageId);
                } else {
                    timers.runNext();
                }
            }
            assertThat(game.finished).isTrue();
            assertThat(tg.anyText(Texts.GAME_ERROR)).isFalse();
            awaitStats();
            assertThat(store.winners).hasSize(1);
        }
    }

    private void awaitStats() {
        long deadline = System.currentTimeMillis() + 2_000;
        while (store.winners.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.onSpinWait();
        }
    }
}
