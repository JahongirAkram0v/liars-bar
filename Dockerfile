# syntax=docker/dockerfile:1

# Kam xotira va CPU uchun JVM sozlamalari (o'lchovda RSS: bo'sh 183 -> 129 MB, yuklama ostida 231 -> 145 MB):
#   SerialGC            - kichik heap uchun eng yengil GC, fon oqimlari yo'q
#   Xmx64m              - heap chegarasi; o'yinlar juda ko'p bo'lsa 96m-128m qiling
#   Xss512k             - har bir platforma oqimi steki kichikroq
#   MaxMetaspaceSize, ReservedCodeCacheSize - klass va JIT kodi uchun chegaralar
#   TieredStopAtLevel=1 - faqat C1 kompilyator: JIT uchun CPU/xotira kam ketadi
ARG JVM_OPTS="-XX:+UseSerialGC -Xmx64m -Xss512k -XX:MaxMetaspaceSize=96m -XX:ReservedCodeCacheSize=32m -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1 -XX:+ExitOnOutOfMemoryError"

# ---------- build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml lombok.config ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
    && java -Djarmode=tools -jar target/liars-bar-*.jar extract --destination /app

# ---------- CDS archive (training run) ----------
# Ilova bir marta ishga tushirilib, yuklangan klasslar arxivga yoziladi: keyingi start ~2 barobar tez.
FROM eclipse-temurin:21-jre AS cds
ARG JVM_OPTS
WORKDIR /app
COPY --from=build /app .
RUN TELEGRAM_BOT_TOKEN=build TELEGRAM_BOT_USERNAME=build_bot \
    TELEGRAM_WEBHOOK_SECRET=build-secret-0123456789 DB_PATH=/tmp/build.db \
    java $JVM_OPTS -XX:ArchiveClassesAtExit=app.jsa -Dspring.aot.enabled=true -Dspring.context.exit=onRefresh \
         -jar liars-bar-*.jar

# ---------- runtime ----------
FROM eclipse-temurin:21-jre
ARG JVM_OPTS
RUN useradd --system --uid 10001 --home /app bot && mkdir -p /data && chown bot /data
WORKDIR /app
COPY --from=cds --chown=bot /app .
USER bot
ENV DB_PATH=/data/liars-bar.db \
    JAVA_OPTS="${JVM_OPTS}"
VOLUME /data
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -XX:SharedArchiveFile=app.jsa -Dspring.aot.enabled=true -jar liars-bar-*.jar"]
