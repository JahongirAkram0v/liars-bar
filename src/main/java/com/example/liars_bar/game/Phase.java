package com.example.liars_bar.game;

enum Phase {
    /** O'yinchilar kutilmoqda. */
    LOBBY,
    /** Guruh to'ldi, BAR/CARD xabarlari yaratilmoqda, keyin SHUFFLE. */
    STARTING,
    /** Navbatdagi o'yinchi karta tashlaydi yoki "Liar" deydi (THROW taymeri). */
    TURN,
    /** "Liar" aytildi, kartalar ochildi, keyin o'q (LIE taymeri). */
    REVEAL,
    /** O'q uzildi, yangi raund kutilmoqda (SHUFFLE taymeri). */
    ROUND_OVER,
    /** Bitta tirik qoldi, g'olib e'lon qilinadi (WIN taymeri). */
    FINISHING
}
