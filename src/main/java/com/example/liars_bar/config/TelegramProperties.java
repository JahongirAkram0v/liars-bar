package com.example.liars_bar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.regex.Pattern;

/**
 * Bot sozlamalari. Noto'g'ri yoki bo'sh qiymat bilan ilova ishga tushmaydi.
 */
@ConfigurationProperties("telegram")
public record TelegramProperties(
        String botToken,
        String botUsername,
        String apiUrl,
        boolean pollingEnabled,
        Stickers stickers
) {

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{5,32}");

    public record Stickers(String death, String survive, String win) {
    }

    public TelegramProperties {
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalArgumentException("TELEGRAM_BOT_TOKEN is not set");
        }
        if (botUsername == null || !USERNAME.matcher(botUsername).matches()) {
            throw new IllegalArgumentException("TELEGRAM_BOT_USERNAME is missing or invalid");
        }
        if (apiUrl == null || !apiUrl.startsWith("https://")) {
            throw new IllegalArgumentException("TELEGRAM_API_URL must use https");
        }
        if (stickers == null) {
            stickers = new Stickers(null, null, null);
        }
    }
}
