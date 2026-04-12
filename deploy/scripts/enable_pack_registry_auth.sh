#!/bin/bash
set -euo pipefail

GC_DIR=/opt/gc
REFRESH_ENV="${GC_DIR}/refresh-env.sh"
COMPOSE_FILE="${GC_DIR}/docker-compose.yml"
PARAM_PATH="${1:-/gc/dev/pack_registry_security_json}"

python3 - "$REFRESH_ENV" "$PARAM_PATH" <<'PY'
from pathlib import Path
import sys

refresh_path = Path(sys.argv[1])
param_path = sys.argv[2]
content = refresh_path.read_text(encoding="utf-8")

if "PACK_REGISTRY_SECURITY_JSON=" not in content:
    marker = 'EMBEDDING_API_KEY=$(aws ssm get-parameter --region "$${REGION}" --name "${ssm_embedding_api_key}" --with-decryption --query \'Parameter.Value\' --output text 2>/dev/null || echo "")\n'
    replacement = (
        marker
        + f'PACK_REGISTRY_SECURITY_JSON=$(aws ssm get-parameter --region "$${{REGION}}" --name "{param_path}" --with-decryption --query \'Parameter.Value\' --output text 2>/dev/null || echo "")\n'
    )
    if marker not in content:
        raise SystemExit("Unable to patch refresh-env.sh: embedding API key lookup marker not found")
    content = content.replace(marker, replacement, 1)

if "SPRING_APPLICATION_JSON=$${PACK_REGISTRY_SECURITY_JSON}" not in content:
    marker = "GC_EMBEDDING_API_KEY=$${EMBEDDING_API_KEY}\n"
    replacement = marker + "SPRING_APPLICATION_JSON=$${PACK_REGISTRY_SECURITY_JSON}\n"
    if marker not in content:
        raise SystemExit("Unable to patch refresh-env.sh: .env heredoc marker not found")
    content = content.replace(marker, replacement, 1)

refresh_path.write_text(content, encoding="utf-8")
PY

python3 - "$COMPOSE_FILE" <<'PY'
from pathlib import Path
import sys

compose_path = Path(sys.argv[1])
content = compose_path.read_text(encoding="utf-8")
line = "      SPRING_APPLICATION_JSON: $${SPRING_APPLICATION_JSON:-}\n"
if line not in content:
    marker = "      SPRING_PROFILES_ACTIVE: prod\n"
    if marker not in content:
        raise SystemExit("Unable to patch docker-compose.yml: backend environment marker not found")
    content = content.replace(marker, line + marker, 1)
compose_path.write_text(content, encoding="utf-8")
PY

"${REFRESH_ENV}"
docker compose -f "${COMPOSE_FILE}" up -d

for i in $(seq 1 120); do
  if curl -sf http://localhost:8000/actuator/health | grep -q '"status":"UP"'; then
    echo "Pack registry auth patch applied and backend is healthy."
    exit 0
  fi
  sleep 2
done

echo "Pack registry auth patch applied but backend health check did not pass in time."
docker compose -f "${COMPOSE_FILE}" logs --tail=50 backend
exit 1
