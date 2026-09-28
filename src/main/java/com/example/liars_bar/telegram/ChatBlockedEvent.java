package com.example.liars_bar.telegram;

/**
 * Telegram chatga xabar yuborishni rad etdi (403): foydalanuvchi botni bloklagan
 * yoki akkaunti o'chirilgan. Bu chatga boshqa xabar yetib bormaydi.
 */
public record ChatBlockedEvent(long chatId) {
}
