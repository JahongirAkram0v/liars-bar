# syntax=docker/dockerfile:1

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
WORKDIR /app
COPY --from=build /app .
RUN TELEGRAM_BOT_TOKEN=build TELEGRAM_BOT_USERNAME=build_bot \
    TELEGRAM_WEBHOOK_SECRET=build-secret-0123456789 DB_PATH=/tmp/build.db \
    java -XX:ArchiveClassesAtExit=app.jsa -Dspring.aot.enabled=true -Dspring.context.exit=onRefresh \
         -jar liars-bar-*.jar

# ---------- runtime ----------
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --home /app bot && mkdir -p /data && chown bot /data
WORKDIR /app
COPY --from=cds --chown=bot /app .
USER bot
ENV DB_PATH=/data/liars-bar.db \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
VOLUME /data
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java -XX:SharedArchiveFile=app.jsa -Dspring.aot.enabled=true -jar liars-bar-*.jar"]
