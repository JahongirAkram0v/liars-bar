package com.example.liars_bar.telegram;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Bot API xatosi. Xabar matnida so'rov URL'i (bot tokeni) hech qachon bo'lmaydi.
 */
@Getter
@Accessors(fluent = true)
public class TelegramException extends RuntimeException {

    public static final int NETWORK_ERROR = -1;

    private final int code;
    private final int retryAfterSeconds;

    public TelegramException(int code, String description, int retryAfterSeconds) {
        super(code + ": " + description);
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public boolean isRetryable() {
        return code == 429 || code >= 500 || code == NETWORK_ERROR;
    }

    /** Foydalanuvchi botni bloklagan, akkaunti o'chirilgan yoki chat mavjud emas. */
    public boolean isForbidden() {
        return code == 403;
    }

    public boolean isNotModified() {
        return code == 400 && getMessage().contains("message is not modified");
    }
}
