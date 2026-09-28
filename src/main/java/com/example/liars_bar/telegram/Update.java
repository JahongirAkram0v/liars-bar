package com.example.liars_bar.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Telegram Update obyektining bot uchun kerakli qismi.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Update(
        @JsonProperty("update_id") long updateId,
        Message message,
        @JsonProperty("callback_query") CallbackQuery callbackQuery
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            @JsonProperty("message_id") long messageId,
            User from,
            Chat chat,
            String text
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CallbackQuery(String id, User from, Message message, String data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(
            long id,
            @JsonProperty("is_bot") boolean isBot,
            @JsonProperty("first_name") String firstName
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Chat(long id, String type) {

        public boolean isPrivate() {
            return "private".equals(type);
        }
    }
}
