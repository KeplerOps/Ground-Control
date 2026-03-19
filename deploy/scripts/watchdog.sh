#!/bin/bash
# Ground Control health check watchdog
# Runs every 5 minutes via cron — restarts backend if health check fails
set -uo pipefail

HEALTH=$(curl -sf http://localhost:8000/actuator/health 2>/dev/null || echo "DOWN")

if echo "${HEALTH}" | grep -q '"status":"UP"'; then
  exit 0
fi

echo "$(date): Health check failed (${HEALTH}), restarting backend..." >> /var/log/gc-watchdog.log
cd /opt/gc && docker compose restart backend
