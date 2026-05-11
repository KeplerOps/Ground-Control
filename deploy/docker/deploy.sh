#!/bin/bash
# Deploy script for the red-dragon docker-compose stack (ADR-030).
#
# Invoked as the SSH forced command for the `gc-deploy` user — the CI deploy
# job SSHes to gc-deploy@red-dragon and the authorized_keys entry forces
# /opt/gc/deploy.sh to run with no argv. SSH exit code is this script's
# exit code, so the CI job's pass/fail reflects the deploy outcome.
#
# Contract:
#   - `/opt/gc/.env` pins `GC_IMAGE` to a FLOATING tag (`...:main` for
#     production). Each `docker compose pull` resolves that tag to whatever
#     the CI `docker` job most recently pushed, so the deploy script does
#     not need to be told the image SHA out-of-band.
#   - `/opt/gc/docker-compose.yml` is the production compose file (this repo
#     ships the canonical copy at `deploy/docker/docker-compose.prod.yml`).
#   - Health check runs INSIDE the backend container via `docker compose
#     exec` because the host port-binding is restricted to the tailnet IP
#     (per #828 / ADR-026 defense in depth); a host-side `curl localhost:8000`
#     can't reach the listener. The JRE Alpine base image ships `wget` but
#     not `curl`, so this script uses `wget`.
set -euo pipefail
cd /opt/gc
docker compose --env-file .env pull
docker compose --env-file .env up -d
echo "Waiting for health check..."
for i in $(seq 1 30); do
  if docker compose --env-file .env exec -T backend \
       wget -q -O - http://localhost:8000/actuator/health 2>/dev/null \
       | grep -q '"UP"'; then
    echo "Deploy complete - application is UP"
    exit 0
  fi
  sleep 2
done
echo "ERROR: Health check did not pass within 60s"
docker compose --env-file .env logs --tail=50 backend
exit 1
