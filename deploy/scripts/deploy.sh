#!/bin/bash
# Ground Control manual deploy helper
# Usage: ssh gc-dev /opt/gc/deploy.sh [image-tag]
#   or:  ./deploy/scripts/deploy.sh [image-tag]
set -euo pipefail

TAG="${1:-latest}"
GC_DIR=/opt/gc

cd "${GC_DIR}"

# Refresh secrets from SSM (regenerates .env with latest values)
if [ -x "${GC_DIR}/refresh-env.sh" ]; then
  echo "Refreshing secrets from SSM..."
  "${GC_DIR}/refresh-env.sh"
fi

# Update image tag if specified
if [ "${TAG}" != "latest" ]; then
  sed -i "s|GC_IMAGE=.*|GC_IMAGE=ghcr.io/keplerops/ground-control:${TAG}|" .env
fi

echo "Pulling images..."
docker compose pull

echo "Starting services..."
docker compose up -d

echo "Waiting for health check..."
for i in $(seq 1 30); do
  if curl -sf http://localhost:8000/actuator/health | grep -q '"status":"UP"'; then
    echo "Deploy complete — application is UP"
    exit 0
  fi
  sleep 2
done

echo "WARNING: Health check did not pass within 60s"
docker compose logs --tail=50 backend
exit 1
