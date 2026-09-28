package com.example.liars_bar.game;

import com.example.liars_bar.config.TelegramProperties;
import com.example.liars_bar.telegram.ChatBlockedEvent;
import com.example.liars_bar.telegram.TelegramSender;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * O'yin mantig'i.
 * <p>
 * Faol o'yinlar xotirada saqlanadi. Bitta o'yinga tegishli har qanday o'zgarish
 * (o'yinchi harakati yoki taymer) shu o'yinning qulfi ostida bajariladi, shuning uchun
 * bir vaqtdagi harakatlar bir-birining natijasini buzmaydi. Taymerlar "token" bilan
 * himoyalangan: faza o'zgargach eski taymer hech narsa qilmaydi.
 */
@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class GameService {

    private static final Pattern GAME_ID = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private static final int CARDS_PER_PLAYER = 5;
    private static final int MAX_GAMES = 2_000;

    public record Settings(
            String botUsername,
            String deathSticker,
            String surviveSticker,
            String winSticker,
            Duration turnTime,
            Duration shortDelay,
            Duration startTimeout,
            Duration lobbyTtl,
            Duration tableRefreshDelay
    ) {
    }

    private final Map<String, Game> games = new ConcurrentHashMap<>();
    private final Map<Long, Game> gameByPlayer = new ConcurrentHashMap<>();

    private final TelegramSender tg;
    private final GameScheduler scheduler;
    private final Random random;
    private final Settings settings;

    @Autowired
    public GameService(TelegramSender tg, GameScheduler scheduler, Random random, TelegramProperties properties) {
        this(tg, scheduler, random, new Settings(
                properties.botUsername(),
                properties.stickers().death(),
                properties.stickers().survive(),
                properties.stickers().win(),
                Duration.ofSeconds(35),
                Duration.ofSeconds(5),
                Duration.ofSeconds(60),
                Duration.ofHours(1),
                Duration.ofMillis(300)
        ));
    }

    public boolean isPlaying(long userId) {
        return gameByPlayer.containsKey(userId);
    }

    // ------------------------------------------------------------------ commands

    /** /start yoki /start &lt;gameId&gt;. O'yindagi o'yinchi uchun e'tiborsiz qoldiriladi. */
    public void onStart(long userId, String name, String payload) {
        if (isPlaying(userId)) {
            return;
        }
        if (payload != null && GAME_ID.matcher(payload).matches()) {
            Game game = games.get(payload);
            if (game != null) {
                if (!join(game, userId, name)) {
                    tg.send(userId, Texts.CANNOT_JOIN, null);
                }
                return;
            }
        }
        tg.send(userId, Texts.CHOOSE_COUNT, Texts.countKeyboard());
    }

    /** /quit: lobbida guruhdan chiqish, o'yinda o'yinni tark etish. */
    public void onQuit(long userId) {
        withGame(userId, (game, seat) -> {
            if (game.phase == Phase.LOBBY || game.phase == Phase.STARTING) {
                leaveLobby(game, seat);
            } else {
                leaveGame(game, seat);
            }
            return null;
        });
    }

    /**
     * Botni bloklagan o'yinchi xabarlarni ko'rmaydi va yurish qila olmaydi:
     * uni kutmasdan /quit bilan bir xil tarzda chiqariladi.
     */
    @EventListener
    public void onChatBlocked(ChatBlockedEvent event) {
        if (isPlaying(event.chatId())) {
            log.info("User {} blocked the bot, removing from game", event.chatId());
            onQuit(event.chatId());
        }
    }

    // ------------------------------------------------------------------ callbacks (return: alert text or null)

    public String onCount(long userId, String name, int count, long messageId) {
        if (count < 2 || count > 4 || isPlaying(userId)) {
            return Texts.ALERT_ERROR;
        }
        if (games.size() >= MAX_GAMES) {
            return Texts.ALERT_BUSY;
        }
        Game game = new Game(UUID.randomUUID().toString(), count);
        if (gameByPlayer.putIfAbsent(userId, game) != null) {
            return Texts.ALERT_ERROR;
        }
        game.lock.lock();
        try {
            game.put(new Seat(userId, name, 0, rollChances()));
            games.put(game.id, game);
            setTimer(game, settings.lobbyTtl(), this::expireLobby);
        } finally {
            game.lock.unlock();
        }
        String url = "https://t.me/" + settings.botUsername() + "?start=" + game.id;
        tg.edit(userId, messageId, Texts.INVITE, Texts.inviteKeyboard(url));
        return null;
    }

    public String onCard(long userId, int index, long messageId) {
        return withGame(userId, (game, seat) -> {
            if (!isTurnOf(game, seat, messageId) || index < 0 || index >= seat.cards.size()) {
                return null;
            }
            if (game.isActiveAlone()) {
                return callLiar(game, seat);
            }
            if (!seat.selected.remove(Integer.valueOf(index))) {
                seat.selected.add(index);
            }
            editCard(seat, Texts.YOUR_TURN, Texts.bidKeyboard(seat.cards, seat.selected));
            return null;
        });
    }

    public String onThrow(long userId, long messageId) {
        return withGame(userId, (game, seat) -> {
            if (!isTurnOf(game, seat, messageId)) {
                return null;
            }
            if (game.isActiveAlone() && seat.selected.isEmpty()) {
                return callLiar(game, seat);
            }
            if (seat.selected.isEmpty()) {
                return Texts.ALERT_PRESS_CARD;
            }
            throwSelected(game, seat);
            return null;
        });
    }

    public String onLiar(long userId, long messageId) {
        return withGame(userId, (game, seat) -> {
            if (!isTurnOf(game, seat, messageId)) {
                return null;
            }
            return callLiar(game, seat);
        });
    }

    public String onEmoji(long userId, int emoji, long messageId) {
        return withGame(userId, (game, seat) -> {
            if (game.phase != Phase.TURN || game.turn == seat.index || !seat.alive || seat.cards.isEmpty()
                    || seat.cardMessageId != messageId || emoji < 0 || emoji >= Texts.EMOJIS.size()
                    || seat.emoji == emoji) {
                return null;
            }
            seat.emoji = emoji;
            scheduleTableRefresh(game);
            editCard(seat, Texts.hand(seat.cards), Texts.emojiKeyboard(seat.emoji));
            return null;
        });
    }

    // ------------------------------------------------------------------ lobby

    private boolean join(Game game, long userId, String name) {
        if (gameByPlayer.putIfAbsent(userId, game) != null) {
            return false;
        }
        boolean joined = false;
        game.lock.lock();
        try {
            if (!game.finished && game.phase == Phase.LOBBY && game.seatCount() < game.capacity) {
                int index = game.freeSeat();
                game.put(new Seat(userId, name, index, rollChances()));
                joined = true;

                int count = game.seatCount();
                String text = Texts.joined(name, count, game.capacity);
                game.seats().forEach(s -> tg.send(s.userId, text, null));
                if (count == game.capacity) {
                    beginStart(game);
                }
            }
        } catch (RuntimeException e) {
            crash(game, e);
        } finally {
            if (!joined) {
                gameByPlayer.remove(userId, game);
            }
            game.lock.unlock();
        }
        return joined;
    }

    /**
     * Kartalarni tarqatadi va har bir o'yinchiga BAR va CARD xabarlarini darhol tayyor holatda yuboradi:
     * "yuklanmoqda" xabari va keyingi tahrir kerak emas. Hamma message_id olingach yurish boshlanadi.
     */
    private void beginStart(Game game) {
        game.phase = Phase.STARTING;
        long token = ++game.startToken;
        setTimer(game, settings.startTimeout(), g -> abort(g, Texts.START_FAILED));

        game.turn = game.firstSeat().index;
        deal(game);
        String table = Texts.table(game);

        List<CompletableFuture<Void>> all = new ArrayList<>();
        for (Seat seat : game.seats()) {
            boolean turn = seat.index == game.turn;
            seat.barText = table;
            seat.cardText = turn ? Texts.YOUR_TURN : Texts.hand(seat.cards);
            seat.cardMarkup = turn ? Texts.bidKeyboard(seat.cards, seat.selected) : Texts.emojiKeyboard(seat.emoji);
            all.add(tg.send(seat.userId, seat.barText, null).thenAccept(id -> seat.barMessageId = id));
            all.add(tg.send(seat.userId, seat.cardText, seat.cardMarkup).thenAccept(id -> seat.cardMessageId = id));
        }
        CompletableFuture.allOf(all.toArray(CompletableFuture[]::new)).whenComplete((ignored, error) ->
                locked(game, () -> {
                    if (game.phase != Phase.STARTING || game.startToken != token) {
                        return;
                    }
                    if (error != null) {
                        abort(game, Texts.START_FAILED);
                        return;
                    }
                    game.phase = Phase.TURN;
                    setTimer(game, settings.turnTime(), this::turnTimeout);
                }));
    }

    private void leaveLobby(Game game, Seat seat) {
        game.seats().forEach(s -> tg.send(s.userId,
                s == seat ? Texts.YOU_LEFT_LOBBY : Texts.leftLobby(seat.name), null));
        game.remove(seat);
        gameByPlayer.remove(seat.userId, game);

        if (game.isEmpty()) {
            end(game);
            return;
        }
        if (game.phase == Phase.STARTING) {
            game.phase = Phase.LOBBY;
            game.startToken++;
        }
        setTimer(game, settings.lobbyTtl(), this::expireLobby);
    }

    private void expireLobby(Game game) {
        if (game.phase == Phase.LOBBY) {
            abort(game, Texts.LOBBY_EXPIRED);
        }
    }

    // ------------------------------------------------------------------ round

    private void shuffle(Game game) {
        deleteStickers(game);
        deal(game);
        game.phase = Phase.TURN;

        updateTable(game);
        for (Seat seat : game.seats()) {
            if (!seat.alive) {
                continue;
            }
            if (seat.index == game.turn) {
                editCard(seat, Texts.YOUR_TURN, Texts.bidKeyboard(seat.cards, seat.selected));
            } else {
                editCard(seat, Texts.hand(seat.cards), Texts.emojiKeyboard(seat.emoji));
            }
        }
        setTimer(game, settings.turnTime(), this::turnTimeout);
    }

    /** Yangi raund holati: kartalar tarqatiladi, stol kartasi tanlanadi, yurish tirik o'yinchiga o'tadi. */
    private void deal(Game game) {
        game.pile = new ArrayList<>();
        game.lastThrower = -1;
        game.lastThrowerName = null;

        List<Character> deck = newDeck();
        Collections.shuffle(deck, random);
        int dealt = 0;
        for (Seat seat : game.seats()) {
            seat.selected.clear();
            if (seat.alive) {
                seat.cards = new ArrayList<>(deck.subList(CARDS_PER_PLAYER * dealt, CARDS_PER_PLAYER * (dealt + 1)));
                seat.active = true;
                dealt++;
            } else {
                seat.cards = new ArrayList<>();
                seat.active = false;
            }
        }
        game.tableCard = "AKQ".charAt(random.nextInt(3));

        Seat current = game.current();
        if (current == null || !current.alive) {
            game.turn = game.nextAfter(game.turn, Game::alive);
        }
    }

    /** Vaqt tugadi: yolg'iz faol o'yinchi "Liar" deydi, aks holda tanlangan (yoki birinchi) karta tashlanadi. */
    private void turnTimeout(Game game) {
        Seat seat = game.current();
        if (seat.selected.isEmpty() && !game.pile.isEmpty()
                && (game.isActiveAlone() || seat.cards.isEmpty())) {
            callLiar(game, seat);
            return;
        }
        if (seat.selected.isEmpty()) {
            seat.selected.add(0);
        }
        throwSelected(game, seat);
    }

    private void throwSelected(Game game, Seat seat) {
        List<Character> keep = new ArrayList<>();
        List<Character> thrown = new ArrayList<>();
        for (int i = 0; i < seat.cards.size(); i++) {
            (seat.selected.contains(i) ? thrown : keep).add(seat.cards.get(i));
        }
        seat.cards = keep;
        seat.selected.clear();
        game.pile = thrown;
        game.lastThrower = seat.index;
        game.lastThrowerName = seat.name;
        game.turn = game.nextAfter(seat.index, Game::aliveAndActive);
        if (keep.isEmpty()) {
            seat.active = false;
        }

        updateTable(game);
        Seat next = game.current();
        editCard(next, Texts.YOUR_TURN, Texts.bidKeyboard(next.cards, next.selected));
        if (keep.isEmpty()) {
            editCard(seat, Texts.NO_CARDS_LEFT, null);
        } else {
            editCard(seat, Texts.hand(seat.cards), Texts.emojiKeyboard(seat.emoji));
        }
        setTimer(game, settings.turnTime(), this::turnTimeout);
    }

    private String callLiar(Game game, Seat caller) {
        if (game.pile.isEmpty()) {
            return Texts.ALERT_PRESS_CARD;
        }
        char table = game.tableCard;
        boolean lie = game.pile.stream().anyMatch(c -> c != table && c != 'J');
        game.turn = lie && game.seat(game.lastThrower) != null ? game.lastThrower : caller.index;
        game.seats().forEach(s -> s.active = s.alive);
        game.phase = Phase.REVEAL;

        String tableText = Texts.liarCalled(table, caller.name);
        String reveal = Texts.reveal(game.pile, table);
        for (Seat seat : game.seats()) {
            editBar(seat, tableText);
            editCard(seat, reveal, null);
        }
        setTimer(game, settings.shortDelay(), this::shoot);
        return null;
    }

    private void shoot(Game game) {
        Seat loser = game.current();
        for (Seat seat : game.seats()) {
            editBar(seat, loser.name);
            editCard(seat, ".", null);
        }

        if (loser.attempt + 1 == loser.chances) {
            loser.alive = false;
            loser.active = false;
            sendStickers(game, settings.deathSticker());
            if (game.aliveCount() == 1) {
                game.turn = game.firstAlive().index;
                game.phase = Phase.FINISHING;
                setTimer(game, settings.shortDelay(), this::win);
                return;
            }
        } else {
            loser.attempt++;
            sendStickers(game, settings.surviveSticker());
        }
        game.turn = game.nextAfter(loser.index, Game::aliveAndActive);
        game.phase = Phase.ROUND_OVER;
        setTimer(game, settings.shortDelay(), this::shuffle);
    }

    private void win(Game game) {
        deleteStickers(game);
        Seat winner = game.current();
        for (Seat seat : game.seats()) {
            editBar(seat, winner.name);
            editCard(seat, "..", null);
            tg.sendSticker(seat.userId, settings.winSticker());
            tg.send(seat.userId, Texts.RESTART, null);
        }
        end(game);
    }

    /** O'yin davomida chiqish: raund qaytadan boshlanadi yoki bitta tirik qolsa g'olib e'lon qilinadi. */
    private void leaveGame(Game game, Seat seat) {
        editBar(seat, Texts.YOU_LEFT_GAME_BAR);
        editCard(seat, Texts.YOU_LEFT_GAME_CARD, null);
        if (seat.sticker != null) {
            seat.sticker.thenAccept(id -> tg.delete(seat.userId, id));
        }
        game.remove(seat);
        gameByPlayer.remove(seat.userId, game);

        if (game.isEmpty()) {
            end(game);
            return;
        }
        if (game.aliveCount() == 0) {
            game.seats().forEach(s -> tg.send(s.userId, Texts.GAME_ENDED, null));
            end(game);
            return;
        }
        if (!seat.alive) {
            // Halok bo'lgan tomoshabin chiqdi: raund davom etadi
            updateTable(game);
            return;
        }

        if (game.aliveCount() == 1) {
            game.turn = game.firstAlive().index;
            game.phase = Phase.FINISHING;
            setTimer(game, settings.shortDelay(), this::win);
        } else {
            game.turn = game.nextAfter(seat.index, Game::alive);
            game.phase = Phase.ROUND_OVER;
            setTimer(game, settings.shortDelay(), this::shuffle);
        }
        updateTable(game);
        String text = Texts.leftLobby(seat.name);
        for (Seat s : game.seats()) {
            if (s.alive) {
                editCard(s, text, null);
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private boolean isTurnOf(Game game, Seat seat, long messageId) {
        return game.phase == Phase.TURN
                && game.turn == seat.index
                && seat.alive
                && seat.cardMessageId == messageId;
    }

    void updateTable(Game game) {
        String text = Texts.table(game);
        game.seats().forEach(s -> editBar(s, text));
    }

    /** Emoji tez-tez bosilganda stol xabari har bosishda emas, qisqa kutishdan keyin bir marta yangilanadi. */
    private void scheduleTableRefresh(Game game) {
        if (game.tableRefreshScheduled) {
            return;
        }
        game.tableRefreshScheduled = true;
        scheduler.schedule(settings.tableRefreshDelay(), () -> locked(game, () -> {
            game.tableRefreshScheduled = false;
            updateTable(game);
        }));
    }

    /** Matn o'zgarmagan bo'lsa, Telegram'ga so'rov yuborilmaydi. */
    private void editBar(Seat seat, String text) {
        if (text.equals(seat.barText)) {
            return;
        }
        seat.barText = text;
        tg.edit(seat.userId, seat.barMessageId, text, null);
    }

    /** Matn va tugmalar o'zgarmagan bo'lsa, Telegram'ga so'rov yuborilmaydi. */
    private void editCard(Seat seat, String text, Object markup) {
        if (text.equals(seat.cardText) && Objects.equals(markup, seat.cardMarkup)) {
            return;
        }
        seat.cardText = text;
        seat.cardMarkup = markup;
        tg.edit(seat.userId, seat.cardMessageId, text, markup);
    }

    private void sendStickers(Game game, String fileId) {
        for (Seat seat : game.seats()) {
            if (seat.sticker != null) {
                seat.sticker.thenAccept(id -> tg.delete(seat.userId, id));
            }
            seat.sticker = tg.sendSticker(seat.userId, fileId);
        }
    }

    private void deleteStickers(Game game) {
        for (Seat seat : game.seats()) {
            if (seat.sticker != null) {
                seat.sticker.thenAccept(id -> tg.delete(seat.userId, id));
                seat.sticker = null;
            }
        }
    }

    private int rollChances() {
        return random.nextInt(6) + 1;
    }

    static List<Character> newDeck() {
        List<Character> deck = new ArrayList<>(20);
        for (char c : new char[]{'A', 'K', 'Q'}) {
            for (int i = 0; i < 6; i++) {
                deck.add(c);
            }
        }
        deck.add('J');
        deck.add('J');
        return deck;
    }

    private void setTimer(Game game, Duration delay, Consumer<Game> action) {
        if (game.timer != null) {
            game.timer.cancel();
        }
        long token = ++game.timerToken;
        game.timer = scheduler.schedule(delay, () -> locked(game, () -> {
            if (game.timerToken != token) {
                return;
            }
            game.timer = null;
            action.accept(game);
        }));
    }

    private void locked(Game game, Runnable action) {
        game.lock.lock();
        try {
            if (!game.finished) {
                action.run();
            }
        } catch (RuntimeException e) {
            crash(game, e);
        } finally {
            game.lock.unlock();
        }
    }

    private String withGame(long userId, GameAction action) {
        Game game = gameByPlayer.get(userId);
        if (game == null) {
            return Texts.ALERT_ERROR;
        }
        game.lock.lock();
        try {
            Seat seat = game.seatOf(userId);
            if (game.finished || seat == null) {
                return Texts.ALERT_ERROR;
            }
            return action.apply(game, seat);
        } catch (RuntimeException e) {
            crash(game, e);
            return null;
        } finally {
            game.lock.unlock();
        }
    }

    private void crash(Game game, RuntimeException e) {
        log.error("Game {} failed, stopping it", game.id, e);
        abort(game, Texts.GAME_ERROR);
    }

    private void abort(Game game, String text) {
        game.seats().forEach(s -> tg.send(s.userId, text, null));
        end(game);
    }

    private void end(Game game) {
        game.finished = true;
        if (game.timer != null) {
            game.timer.cancel();
            game.timer = null;
        }
        games.remove(game.id, game);
        game.seats().forEach(s -> gameByPlayer.remove(s.userId, game));
    }

    @FunctionalInterface
    private interface GameAction {

        /** @return foydalanuvchiga ko'rsatiladigan ogohlantirish yoki null */
        String apply(Game game, Seat seat);
    }

    // ------------------------------------------------------------------ test helpers

    Game gameOf(long userId) {
        return gameByPlayer.get(userId);
    }

    int activeGames() {
        return games.size();
    }
}
