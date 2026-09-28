package com.example.liars_bar.telegram;

import java.util.concurrent.CompletableFuture;

/**
 * Chiquvchi xabarlar. Barcha metodlar bloklanmaydi: xabar navbatga qo'yiladi
 * va bitta chat uchun yuborish tartibi saqlanadi.
 */
public interface TelegramSender {

    /** @return yuborilgan xabarning message_id si */
    CompletableFuture<Long> send(long chatId, String text, Object replyMarkup);

    void edit(long chatId, long messageId, String text, Object replyMarkup);

    /** @return yuborilgan stikerning message_id si */
    CompletableFuture<Long> sendSticker(long chatId, String fileId);

    void delete(long chatId, long messageId);

    /** @param alert null bo'lsa, tugma jimgina tasdiqlanadi */
    void answerCallback(long chatId, String callbackQueryId, String alert);
}
