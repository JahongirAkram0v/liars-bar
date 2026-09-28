#!/bin/bash
set -euo pipefail
set -a
# shellcheck disable=SC1091
source .env
set +a

curl -sS -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/deleteWebhook"
echo
