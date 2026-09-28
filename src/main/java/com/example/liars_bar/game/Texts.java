package com.example.liars_bar.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bot xabarlari va klaviaturalari (asl loyihadagi matnlar saqlangan).
 */
final class Texts {

    static final List<String> EMOJIS = List.of(
            "", "😄", "🥸", "😭", "🤬", "😮‍💨");

    static final String CHOOSE_COUNT = "O'yinchilar sonini tanlang!";
    static final String INVITE = "O'yinni boshlash uchun dostlaringizga yuboring";
    static final String CANNOT_JOIN = "Bu o'yinga qo'shilib bo'lmaydi. Yangi o'yin uchun /start ni bosing.";
    static final String LOADING_BAR = "O'yin yuklanmoqda ⌛️⏳";
    static final String LOADING_CARD = "Biroz vaqt talab qiladi ⏳⌛️";
    static final String YOU_LEFT_LOBBY = "Siz guruhni tark etdingiz. /start tugmasini bosing";
    static final String YOU_LEFT_GAME_BAR = "guruhni tark etdingiz.";
    static final String YOU_LEFT_GAME_CARD = "Yangidan boshlash uchun /start ni bosing.";
    static final String YOUR_TURN = "🔹🔹🔹 Sizning yurishingiz 🔹🔹🔹";
    static final String NO_CARDS_LEFT = "Sizda karta qolmadi, o'yinni kuzating.";
    static final String RESTART = "O'yinni qayta boshlash uchun /start ni bosing!";
    static final String START_FAILED = "O'yinni boshlab bo'lmadi. Qaytadan /start ni bosing.";
    static final String LOBBY_EXPIRED = "O'yin bekor qilindi: guruh uzoq vaqt to'lmadi. /start ni bosing.";
    static final String GAME_ERROR = "Xatolik yuz berdi, o'yin to'xtatildi. Qaytadan /start ni bosing.";
    static final String GAME_ENDED = "O'yin tugadi. Qaytadan /start ni bosing.";

    static final String ALERT_PRESS_CARD = "Press card";
    static final String ALERT_ERROR = "SomeThing went wrong!!";
    static final String ALERT_BUSY = "Server band, keyinroq urinib ko'ring.";

    private Texts() {
    }

    static String joined(String name, int count, int capacity) {
        return name + " qo'shildi. (" + count + "/" + capacity + ")";
    }

    static String leftLobby(String name) {
        return name + " guruhni tark etdi.";
    }

    static String liarCalled(char tableCard, String name) {
        return "🃏 : " + tableCard + " | " + name + " ishonmadi";
    }

    /** Ochilgan kartalar va ularning to'g'ri/yolg'onligi. */
    static String reveal(List<Character> pile, char tableCard) {
        StringBuilder cards = new StringBuilder();
        StringBuilder marks = new StringBuilder();
        for (char c : pile) {
            marks.append(c == tableCard || c == 'J' ? "🟩" : "🟥");
            cards.append(" ").append(c).append(" ");
        }
        return cards + "\n" + marks;
    }

    static String hand(List<Character> cards) {
        StringBuilder text = new StringBuilder("🔹🔸🔹 ");
        for (char c : cards) {
            text.append(" ").append(c).append(" ");
        }
        return text + " 🔹🔸🔹";
    }

    /** Stol holati (BAR xabari). */
    static String table(Game game) {
        StringBuilder text = new StringBuilder("🔸 : ").append(game.tableCard);
        if (!game.pile.isEmpty()) {
            text.append(" ❗️").append(game.lastThrowerName == null ? "" : game.lastThrowerName)
                    .append(" 🃏x").append(game.pile.size());
        }
        text.append("\n➖➖➖➖➖➖➖➖➖➖\n");
        for (Seat seat : game.seats()) {
            text.append(seat.alive ? "👤 : " : "💀 : ").append(seat.name)
                    .append(" - (").append(seat.attempt).append("/6) ")
                    .append(seat.index == game.turn ? "👾" : "")
                    .append(" | 🃏x").append(seat.cards.size())
                    .append(seat.emoji == 0 ? "" : " /" + EMOJIS.get(seat.emoji))
                    .append("\n");
        }
        return text.toString().trim();
    }

    static Map<String, Object> countKeyboard() {
        return markup(List.of(List.of(
                button("2", "x2"),
                button("3", "x3"),
                button("4", "x4")
        )));
    }

    static Map<String, Object> inviteKeyboard(String url) {
        return markup(List.of(List.of(Map.of("text", "Join", "url", url))));
    }

    static Map<String, Object> bidKeyboard(List<Character> cards, List<Integer> selected) {
        List<Map<String, Object>> row = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            String label = selected.contains(i) ? "✅ " + cards.get(i) : String.valueOf(cards.get(i));
            row.add(button(label, String.valueOf(i)));
        }
        return markup(List.of(row, List.of(button("Liar", "l"), button("Throw", "t"))));
    }

    static Map<String, Object> emojiKeyboard(int current) {
        List<Map<String, Object>> row = new ArrayList<>();
        for (int i = 1; i < EMOJIS.size(); i++) {
            row.add(i == current ? button("🚫", "e0") : button(EMOJIS.get(i), "e" + i));
        }
        return markup(List.of(row));
    }

    private static Map<String, Object> button(String text, String data) {
        return Map.of("text", text, "callback_data", data);
    }

    private static Map<String, Object> markup(List<List<Map<String, Object>>> rows) {
        return Map.of("inline_keyboard", rows);
    }
}
