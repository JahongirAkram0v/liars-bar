# Liar's Bar — Telegram bot

2–4 kishilik "Liar's Bar" o'yini Telegram bot ko'rinishida. Spring Boot 3.5, Java 21, SQLite.

## O'yin qoidalari
- Koloda: 6×A, 6×K, 6×Q, 2×J (joker). Har bir tirik o'yinchiga 5 tadan karta tarqatiladi.
- Har raundda stol kartasi (A, K yoki Q) tanlanadi. J har doim to'g'ri karta hisoblanadi.
- Navbatdagi o'yinchi istalgancha kartani tanlab tashlaydi ("Throw") yoki oldingi o'yinchiga ishonmaydi ("Liar").
- "Liar" aytilganda kartalar ochiladi. Yolg'on bo'lsa tashlagan o'yinchi, rost bo'lsa ishonmagan o'yinchi to'pponcha tortadi.
- Har o'yinchining o'lim o'qi tasodifiy (1–6). Oxirgi tirik qolgan o'yinchi g'olib bo'ladi.
- Yurish uchun 35 soniya beriladi. Vaqt tugasa, tanlangan karta yoki birinchi karta avtomatik tashlanadi.
- Faqat bitta faol o'yinchi qolsa, u "Liar" deyishga majbur.
- Buyruqlar: `/start` — yangi o'yin yoki taklif havolasi orqali qo'shilish, `/quit` — chiqish.

## Arxitektura
```
Telegram ──HTTPS──▶ WebhookSecretFilter ─▶ WebhookController ─▶ UpdateRouter
                                                                   │ (foydalanuvchi bo'yicha navbat)
                                                                   ▼
                        SQLite ◀── PlayerStore ◀──────────── GameService (xotirada, o'yin qulfi)
                                                                   │            ▲
                                                                   ▼            │ taymerlar
                                                           TelegramOutbox    GameScheduler
                                                     (chat bo'yicha navbat, 30 msg/s, retry)
```
- Faol o'yinlar xotirada saqlanadi. Bitta o'yinning barcha o'zgarishlari (tugma, `/quit`, taymer) shu o'yin qulfi ostida bajariladi.
- Taymerlar token bilan himoyalangan: faza o'zgargach eski taymer ishlamaydi.
- SQLite'da faqat o'yinchilar va statistika (`games_played`, `wins`) saqlanadi.
- Server qayta ishga tushsa, davom etayotgan o'yinlar yo'qoladi.

## Tezlik
Bot tezligini Telegram limiti (~30 xabar/s) belgilaydi, shuning uchun asosiy e'tibor so'rovlar sonini kamaytirishga qaratilgan:
- Bitta xabarning navbatda kutayotgan tahrirlari birlashtiriladi: faqat oxirgi holat yuboriladi.
- Matni va tugmalari o'zgarmagan xabar qayta yuborilmaydi.
- Emoji bosilganda stol xabari 300 ms ichida bir marta yangilanadi.
- Tugma tasdig'i (`answerCallbackQuery`) navbat va limitni kutmasdan darhol yuboriladi.
- Tomcat va navbatlar virtual threadlarda ishlaydi.
- Spring AOT va CDS arxivi bilan ilova ~2 barobar tez ishga tushadi (o'lchovda 3.1 s → 1.5 s).

## Xavfsizlik
- Webhook faqat to'g'ri `X-Telegram-Bot-Api-Secret-Token` sarlavhasi bilan qabul qilinadi. Sarlavha body o'qilishidan oldin, doimiy vaqtda (constant time) solishtiriladi.
- Faqat shaxsiy chatlar qabul qilinadi. Callback ma'lumotlari qat'iy shablon bilan tekshiriladi.
- Eski xabarlardagi tugmalar va navbatdan tashqari bosishlar e'tiborsiz qoldiriladi.
- Har bir foydalanuvchi uchun so'rovlar soni cheklangan. Kiruvchi va chiquvchi navbatlarning hajmi chegaralangan.
- Lobbi 1 soatda o'chadi. Bir vaqtda ko'pi bilan 2000 ta o'yin bo'lishi mumkin.
- Bot tokeni loglarga yozilmaydi. Xato javoblarida ichki ma'lumot qaytarilmaydi.
- O'yin identifikatori UUID, tasodifiy sonlar `SecureRandom` bilan olinadi.

## Ishga tushirish
```bash
cp .env.example .env        # qiymatlarni to'ldiring
openssl rand -hex 32        # TELEGRAM_WEBHOOK_SECRET uchun
./mvnw spring-boot:run
./set_webhook.sh            # webhookni secret_token bilan ro'yxatdan o'tkazadi
```
Sozlamalarni `.env` o'rniga muhit o'zgaruvchilari orqali ham berish mumkin.
`DB_PATH` (standart qiymati `data/liars-bar.db`) doimiy diskda turishi kerak.

### Docker
```bash
docker build -t liars-bar .
docker run -d --env-file .env -p 8080:8080 -v liars-bar-data:/data liars-bar
```
Image AOT va CDS arxivi bilan yig'iladi, root bo'lmagan foydalanuvchi bilan ishlaydi, SQLite `/data` volume'da saqlanadi.

## Testlar
```bash
./mvnw test
```
