package com.example.liars_bar.game;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * O'yin ichidagi o'yinchi. Faqat o'z o'yinining qulfi ostida o'zgartiriladi.
 */
@RequiredArgsConstructor
final class Seat {

    final long userId;
    final String name;
    final int index;
    /** Nechanchi o'qda halok bo'ladi (1..6). */
    final int chances;

    int attempt;
    List<Character> cards = new ArrayList<>();
    /** Tanlangan kartalar indekslari (tanlash tartibida). */
    final List<Integer> selected = new ArrayList<>();
    int emoji;
    long barMessageId = -1;
    long cardMessageId = -1;
    boolean alive = true;
    boolean active = true;
    CompletableFuture<Long> sticker;

    /** Oxirgi yuborilgan matn/tugmalar: o'zgarmagan xabar qayta yuborilmaydi. */
    String barText;
    String cardText;
    Object cardMarkup;
}
