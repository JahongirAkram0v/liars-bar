#!/bin/bash
# shellcheck disable=SC2046
export $(grep -v '^#' .env | xargs)

curl -X POST "$TELEGRAM_BASE_URL$TELEGRAM_BOT_TOKEN/setWebhook" -d "url=$WEBHOOK_URL$TELEGRAM_BOT_WEBHOOK_PATH"
echo