# Liar's Bar — Telegram bot

2–4 kishilik "Liar's Bar" o'yini Telegram bot ko'rinishida. Spring Boot 3.5, Java 25. Ma'lumotlar bazasi yo'q.

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
Telegram ◀──getUpdates (long polling)── UpdatePoller ─▶ UpdateRouter
                                                                   │ (foydalanuvchi bo'yicha navbat)
                                                                   ▼
                                                             GameService (xotirada, o'yin qulfi)
                                                                   │            ▲
                                                                   ▼            │ taymerlar
                                                           TelegramOutbox    GameScheduler
                                                     (chat bo'yicha navbat, 30 msg/s, retry)
```
- Update'lar long polling orqali olinadi: veb-server, ochiq port, domen va HTTPS sertifikat kerak emas.
  Ilova ishga tushganda webhook avtomatik o'chiriladi. Bot bir vaqtda faqat bitta nusxada ishlashi kerak.
- Faol o'yinlar xotirada saqlanadi. Bitta o'yinning barcha o'zgarishlari (tugma, `/quit`, taymer) shu o'yin qulfi ostida bajariladi.
- Taymerlar token bilan himoyalangan: faza o'zgargach eski taymer ishlamaydi.
- Ma'lumotlar bazasi ishlatilmaydi: o'yin tugagach uning ma'lumotlari o'chadi, disk bilan ishlash yo'q.
- Server qayta ishga tushsa, davom etayotgan o'yinlar yo'qoladi.

## Tezlik
Bot tezligini Telegram limiti (~30 xabar/s) belgilaydi, shuning uchun asosiy e'tibor so'rovlar sonini kamaytirishga qaratilgan:
- Bitta xabarning navbatda kutayotgan tahrirlari birlashtiriladi: faqat oxirgi holat yuboriladi.
- Matni va tugmalari o'zgarmagan xabar qayta yuborilmaydi.
- Emoji bosilganda stol xabari 300 ms ichida bir marta yangilanadi.
- Tugma tasdig'i (`answerCallbackQuery`) navbat va limitni kutmasdan darhol yuboriladi.
- Veb-server (Tomcat) yo'q, polling va navbatlar virtual threadlarda ishlaydi.
- Spring AOT va CDS arxivi bilan ilova ~2 barobar tez ishga tushadi (o'lchovda 3.1 s → 1.5 s).

## Xavfsizlik
- Tashqaridan kiruvchi ulanish yo'q: bot faqat o'zi Telegram API'ga murojaat qiladi.
- Faqat shaxsiy chatlar qabul qilinadi. Callback ma'lumotlari qat'iy shablon bilan tekshiriladi.
- Eski xabarlardagi tugmalar va navbatdan tashqari bosishlar e'tiborsiz qoldiriladi.
- Har bir foydalanuvchi uchun so'rovlar soni cheklangan. Kiruvchi va chiquvchi navbatlarning hajmi chegaralangan.
- Lobbi 1 soatda o'chadi. Bir vaqtda ko'pi bilan 2000 ta o'yin bo'lishi mumkin.
- Bot tokeni loglarga yozilmaydi.
- O'yin identifikatori UUID, tasodifiy sonlar `SecureRandom` bilan olinadi.

## Ishga tushirish
```bash
cp .env.example .env        # TELEGRAM_BOT_TOKEN va TELEGRAM_BOT_USERNAME ni to'ldiring
./mvnw spring-boot:run
```
Sozlamalarni `.env` o'rniga muhit o'zgaruvchilari orqali ham berish mumkin.

### Termux (Android)

**1. Paketlarni o'rnatish.** Git, Java, `mvnw` Maven'ni yuklab olishi uchun `curl` va `unzip`, jarayonlarni tekshirish uchun `procps` kerak:
```bash
pkg update && pkg upgrade -y
pkg install -y git curl unzip procps openjdk-25
java -version
```
Termux'da `openjdk-25` bo'lmasa (`pkg search openjdk` bilan tekshiring), `openjdk-21` ni o'rnating
va 4-qadamda yig'ish buyrug'iga `-Djava.version=21` qo'shing.

**2. Loyihani yuklab olish:**
```bash
cd ~
git clone https://github.com/JahongirAkram0v/liars-bar.git
cd liars-bar
```
Repozitoriy yopiq (private) bo'lsa, git parol o'rniga GitHub'dagi Personal Access Token'ni so'raydi.

**3. Sozlamalar:**
```bash
cp .env.example .env
nano .env        # TELEGRAM_BOT_TOKEN va TELEGRAM_BOT_USERNAME ni yozing (nano yo'q bo'lsa: pkg install nano)
```

**4. Jar faylni yaratish.** Maven'ni alohida o'rnatish shart emas: `mvnw` birinchi ishga tushganda uni o'zi
`~/.m2` ga yuklab oladi (internet kerak, bir necha daqiqa ketadi):
```bash
chmod +x mvnw
./mvnw -B package -DskipTests
# openjdk-21 bilan: ./mvnw -B package -DskipTests -Djava.version=21
ls target/*.jar  # target/liars-bar-0.0.1-SNAPSHOT.jar
```

**5. Orqa fonda ishga tushirish.** `.env` joriy papkadan o'qiladi, shuning uchun loyiha papkasida ishga tushiring:
```bash
cd ~/liars-bar
termux-wake-lock  # telefon uxlaganda Android jarayonni to'xtatib qo'ymasligi uchun
nohup java -XX:+UseSerialGC -Xmx128m -XX:TieredStopAtLevel=1 -Dspring.aot.enabled=true \
    -jar target/liars-bar-0.0.1-SNAPSHOT.jar > bot.log 2>&1 &
```
Android sozlamalarida Termux uchun batareya optimizatsiyasini o'chirib qo'ying, aks holda tizim uni yopib qo'yishi mumkin.
Termux bildirishnomasidagi "Exit" tugmasini bosmang: u barcha jarayonlarni to'xtatadi.

**6. Bot ishlayotganini tekshirish:**
```bash
pgrep -af "^java .*liars-bar"   # jarayon ro'yxatda bo'lsa, bot ishlayapti; bo'sh bo'lsa, to'xtagan
tail -f bot.log       # loglarni kuzatish (chiqish: Ctrl+C, bot to'xtamaydi)
```

**7. Botni to'xtatish:**
```bash
pkill -f "^java .*liars-bar"
termux-wake-unlock
```

**Yangilash** (kodda o'zgarish bo'lganda):
```bash
cd ~/liars-bar
pkill -f "^java .*liars-bar"
git pull
./mvnw -B package -DskipTests
nohup java -XX:+UseSerialGC -Xmx128m -XX:TieredStopAtLevel=1 -Dspring.aot.enabled=true \
    -jar target/liars-bar-0.0.1-SNAPSHOT.jar > bot.log 2>&1 &
```

### Docker
```bash
docker build -t liars-bar .
docker run -d --restart unless-stopped --env-file .env liars-bar
```
Image AOT va CDS arxivi bilan yig'iladi, root bo'lmagan foydalanuvchi bilan ishlaydi.

Konteyner kam resurs uchun sozlangan JVM bilan ishlaydi: SerialGC, 64 MB heap, faqat C1 kompilyator.
O'lchovda xotira (RSS) bo'sh holatda 183 → 129 MB, yuklama ostida 231 → 145 MB ga tushdi.
Long polling'ga o'tilgach (Tomcat'siz) bo'sh holatda 117 MB, ishga tushish ~1 s.
O'yinlar juda ko'p bo'lsa, heap'ni oshiring:
```bash
docker run -e JAVA_OPTS="-XX:+UseSerialGC -Xmx128m -XX:TieredStopAtLevel=1" ...
```

## Testlar
```bash
./mvnw test
```
