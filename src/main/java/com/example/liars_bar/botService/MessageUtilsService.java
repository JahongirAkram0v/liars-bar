package com.example.liars_bar.botService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MessageUtilsService {

    public static Map<String, Object> sendMessage(Long chatId, String text) {

        return Map.of(
                "chat_id", chatId,
                "text", text
        );
    }

    public static Map<String, Object> sendMessage(Long chatId, String text, List<List<Map<String, Object>>> keyboard) {

        return Map.of(
                "chat_id", chatId,
                "text", text,
                "reply_markup", Map.of("inline_keyboard", keyboard)
        );
    }

    public static Map<String, Object> editMessage(Integer messageId, Long chatId, String text) {

        return Map.of(
                "message_id", messageId,
                "chat_id", chatId,
                "text", text
        );
    }

    public static Map<String, Object> editMessage(Integer messageId, Long chatId, String text, List<List<Map<String, Object>>> keyboard) {

        return Map.of(
                "message_id", messageId,
                "chat_id", chatId,
                "text", text,
                "reply_markup", Map.of("inline_keyboard", keyboard)
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

    public static Map<String, Object> errorMessage(String callbackQueryId) {

        return Map.of(
                "callback_query_id", callbackQueryId,
                "text", "ERROR",
                "show_alert", false
        );
    }

    public static Map<String, Object> start(Long id) {
        List<List<Map<String, Object>>> keyboards = List.of(
                List.of(
                        Map.of("text", "2", "callback_data", "c 2"),
                        Map.of("text", "3", "callback_data", "c 3"),
                        Map.of("text", "4", "callback_data", "c 4")
                )
        );

        return MessageUtilsService.sendMessage(
                id,
                "O'yinchilar sonini tanlang!",
                keyboards
        );
    }


}
