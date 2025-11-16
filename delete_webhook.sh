#!/bin/bash
# shellcheck disable=SC2046
export $(grep -v '^#' .env | xargs)

curl -s -X POST "$TELEGRAM_BASE_URL$TELEGRAM_BOT_TOKEN$TELEGRAM_BOT_WEBHOOK_PATH"