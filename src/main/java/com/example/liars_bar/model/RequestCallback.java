package com.example.liars_bar.model;

public record RequestCallback(Long id, String command, int messageId, String callbackQueryId) {
}
