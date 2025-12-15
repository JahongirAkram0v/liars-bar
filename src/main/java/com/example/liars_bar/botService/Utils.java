package com.example.liars_bar.botService;

import com.example.liars_bar.model.Response;
import com.example.liars_bar.model.Which;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.liars_bar.model.Which.NOTHING;
import static com.example.liars_bar.model.Which.STICKER;

public class Utils {

    public static final List<String> emojis = List.of("", "\uD83D\uDE04", "\uD83E\uDD78", "\uD83D\uDE2D", "\uD83E\uDD2C", "\uD83D\uDE2E\u200D\uD83D\uDCA8");

    public static Response error(String callbackQueryId, String error) {
        return new Response(
                Map.of(
                        "callback_query_id", callbackQueryId,
                        "text", error,
                        "show_alert", true
                ),
                "answerCallbackQuery",
                NOTHING
        );
    }

    public static Response delete(Long id, Integer messageId) {
        return new Response(
                Map.of(
                        "chat_id", id,
                        "message_id", messageId
                ),
                "deleteMessage",
                NOTHING
        );
    }

    public static Response sticker(Long id, String fileId) {
        return new Response(
                Map.of(
                        "chat_id", id,
                        "sticker", fileId
                ),
                "sendSticker",
                STICKER
        );
    }

    public static Response text(Long id, String text, Which which) {
        return new Response(
                Map.of(
                        "chat_id", id,
                        "text", text
                ),
                "sendMessage",
                which
        );
    }

    public static Response text(Long id, String text, List<List<Map<String, Object>>> keyboard) {
        return new Response(
                Map.of(
                        "chat_id", id,
                        "text", text,
                        "reply_markup", Map.of("inline_keyboard", keyboard)
                ),
                "sendMessage",
                NOTHING
        );
    }

    public static Response editText(Long id, String text, int messageId) {
        return new Response(
                Map.of(
                        "chat_id", id,
                        "message_id", messageId,
                        "text", text
                ),
                "editMessageText",
                NOTHING
        );
    }

    public static Response editText(Long id, String text, List<List<Map<String, Object>>> keyboard, int messageId) {
        return new Response(
                Map.of(
                        "chat_id", id,
                        "message_id", messageId,
                        "text", text,
                        "reply_markup", Map.of("inline_keyboard", keyboard)
                ),
                "editMessageText",
                NOTHING
        );
    }

    public static List<List<Map<String, Object>>> getBid(List<Character> cards) {

        List<Map<String, Object>> row = new ArrayList<>();

        for (int i = 0; i < cards.size(); i++) {
            row.add(Map.of("text", cards.get(i), "callback_data", i));
        }

        return List.of(
                row,
                List.of(
                        Map.of("text", "Liar", "callback_data", "l"),
                        Map.of("text", "Throw", "callback_data", "t")
                )
        );
    }

    public static List<Map<String, Object>> getEditBid(List<Character> cards, List<Integer> temp) {

        List<Map<String, Object>> row = new ArrayList<>();

        for (int i = 0; i < cards.size(); i++) {

            if (temp.contains(i)) {
                row.add(Map.of("text", "✅ " + cards.get(i), "callback_data", i));
            } else {
                row.add(Map.of("text", cards.get(i), "callback_data", i));
            }
        }
        return row;
    }

    public static List<List<Map<String, Object>>> editCard(int index) {
        List<Map<String, Object>> row = new ArrayList<>(
                List.of(
                        new HashMap<>(Map.of("text", emojis.get(1), "callback_data", "e1")),
                        new HashMap<>(Map.of("text", emojis.get(2), "callback_data", "e2")),
                        new HashMap<>(Map.of("text", emojis.get(3), "callback_data", "e3")),
                        new HashMap<>(Map.of("text", emojis.get(4), "callback_data", "e4")),
                        new HashMap<>(Map.of("text", emojis.get(5), "callback_data", "e5"))
                )
        );

        if (index != 0) row.set(index-1, new HashMap<>(
                Map.of(
                        "text", "\uD83D\uDEAB",
                        "callback_data", "e0"
                )
        ));

        return List.of(row);
    }

}
