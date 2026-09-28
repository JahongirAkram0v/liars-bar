package com.example.liars_bar.telegram;

/**
 * Bot API xatosi. Xabar matnida so'rov URL'i (bot tokeni) hech qachon bo'lmaydi.
 */
public class TelegramException extends RuntimeException {

    public static final int NETWORK_ERROR = -1;

    private final int code;
    private final int retryAfterSeconds;

    public TelegramException(int code, String description, int retryAfterSeconds) {
        super(code + ": " + description);
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int code() {
        return code;
    }

    public int retryAfterSeconds() {
        return retryAfterSeconds;
    }

    public boolean isRetryable() {
        return code == 429 || code >= 500 || code == NETWORK_ERROR;
    }

    public boolean isNotModified() {
        return code == 400 && getMessage().contains("message is not modified");
    }
}
