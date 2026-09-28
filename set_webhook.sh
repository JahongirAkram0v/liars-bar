#!/bin/bash
set -euo pipefail
set -a
# shellcheck disable=SC1091
source .env
set +a

curl -sS -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/setWebhook" \
  --data-urlencode "url=${WEBHOOK_URL}${TELEGRAM_WEBHOOK_PATH:-/webhook}" \
  --data-urlencode "secret_token=${TELEGRAM_WEBHOOK_SECRET}" \
  --data-urlencode 'allowed_updates=["message","callback_query"]' \
  --data-urlencode "drop_pending_updates=true"
echo
