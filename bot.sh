#!/usr/bin/env bash
# Termux uchun botni boshqarish: ./bot.sh {start|stop|restart|update|status|log}
set -euo pipefail

cd "$(dirname "$(readlink -f "$0")")"

JAR="target/liars-bar-0.0.1-SNAPSHOT.jar"
PID_FILE="bot.pid"
LOG_FILE="bot.log"
PATTERN="^java .*liars-bar"
JAVA_OPTS="${JAVA_OPTS:--XX:+UseSerialGC -Xmx128m -XX:TieredStopAtLevel=1}"

has() { command -v "$1" >/dev/null 2>&1; }

pid() {
    if [[ -f $PID_FILE ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
        cat "$PID_FILE"
    else
        pgrep -f "$PATTERN" | head -n1 || true
    fi
}

build() {
    chmod +x mvnw
    local ver extra=()
    ver=$(java -version 2>&1 | sed -nE 's/.*version "([0-9]+).*/\1/p' | head -n1)
    # openjdk-25 bo'lmasa, o'rnatilgan Java versiyasi bilan yig'iladi
    [[ -n $ver && $ver -lt 25 ]] && extra=("-Djava.version=$ver")
    ./mvnw -B package -DskipTests "${extra[@]}"
}

start() {
    if [[ -n $(pid) ]]; then
        echo "Bot allaqachon ishlayapti (PID $(pid))"
        return
    fi
    [[ -f .env ]] || { echo ".env topilmadi: cp .env.example .env va to'ldiring"; exit 1; }
    [[ -f $JAR ]] || build
    has termux-wake-lock && termux-wake-lock
    # shellcheck disable=SC2086
    nohup java $JAVA_OPTS -Dspring.aot.enabled=true -jar "$JAR" > "$LOG_FILE" 2>&1 &
    echo $! > "$PID_FILE"
    sleep 3
    if kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
        echo "Bot ishga tushdi (PID $(cat "$PID_FILE")), log: $LOG_FILE"
    else
        echo "Bot ishga tushmadi, oxirgi loglar:"
        tail -n 20 "$LOG_FILE"
        rm -f "$PID_FILE"
        exit 1
    fi
}

stop() {
    local p
    p=$(pid)
    if [[ -z $p ]]; then
        echo "Bot ishlamayapti"
    else
        kill "$p"
        # 15 soniya kutiladi, to'xtamasa majburan o'chiriladi
        for _ in {1..15}; do
            kill -0 "$p" 2>/dev/null || break
            sleep 1
        done
        kill -0 "$p" 2>/dev/null && kill -9 "$p"
        echo "Bot to'xtatildi (PID $p)"
    fi
    rm -f "$PID_FILE"
    has termux-wake-unlock && termux-wake-unlock
    return 0
}

update() {
    stop
    git pull --ff-only
    build
    echo "Yangilandi. Ishga tushirish: ./bot.sh start"
}

status() {
    local p
    p=$(pid)
    if [[ -n $p ]]; then echo "Ishlayapti (PID $p)"; else echo "To'xtagan"; fi
}

case "${1:-}" in
    start)   start ;;
    stop)    stop ;;
    restart) stop; start ;;
    update)  update ;;
    status)  status ;;
    log)     tail -f "$LOG_FILE" ;;
    *)       echo "Foydalanish: $0 {start|stop|restart|update|status|log}"; exit 1 ;;
esac
