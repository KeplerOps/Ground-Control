#!/bin/bash
set -euo pipefail

GC_DIR=/opt/gc
REFRESH_ENV="${GC_DIR}/refresh-env.sh"
COMPOSE_FILE="${GC_DIR}/docker-compose.yml"
ENV_FILE="${GC_DIR}/.env"
PARAM_PATH="${1:-/gc/dev/pack_registry_security_json}"

# Refuse to run when the host is already configured with ADR-026 indexed
# credential slots. Spring's bound configuration treats SPRING_APPLICATION_JSON
# as a higher-precedence list source — and List properties replace, not merge —
# so injecting the SSM-supplied JSON credential block would silently retire any
# MCP / operator / automation principals the operator added via indexed env
# vars. The ADR-026 path is the supported way forward; route the pack-registry
# admin credential to a free GROUNDCONTROL_SECURITY_CREDENTIALS_<N>_* slot
# instead of running this script (#828 cycle 3).
if [ -r "${ENV_FILE}" ] && grep -qE '^GROUNDCONTROL_SECURITY_CREDENTIALS_[0-9]+_' "${ENV_FILE}"; then
  cat <<EOF >&2
ERROR: ${ENV_FILE} already contains GROUNDCONTROL_SECURITY_CREDENTIALS_* entries.
This script writes the pack-registry admin credential into SPRING_APPLICATION_JSON,
which would replace the indexed ADR-026 credential list at startup and 401 every
other consumer. Add the pack-registry admin token to a free
GROUNDCONTROL_SECURITY_CREDENTIALS_<N>_{PRINCIPAL_NAME,TOKEN,ROLE} slot
(role=ADMIN) directly in ${ENV_FILE} instead and skip this bootstrap.
EOF
  exit 2
fi

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

# Accept either of the two compose shapes the canonical
# deploy/docker/docker-compose.prod.yml has used: legacy map form
# (SPRING_APPLICATION_JSON: ${...}) or post-#828 list form
# (- SPRING_APPLICATION_JSON=${...} / bare - SPRING_APPLICATION_JSON). The
# canonical file now ships the list-form line in both production and the
# runtime mirror, so the patch is a no-op there. The map-form fallback
# preserves compatibility with any pre-cutover host that still carries the
# older shape. The single-dollar form below matches what compose actually
# interpolates from .env / the host environment — a `$$` escape would
# inject the literal string `${SPRING_APPLICATION_JSON:-}` into the
# container, which is not what the operator wants.
list_form_present = any(
    fragment in content
    for fragment in (
        "- SPRING_APPLICATION_JSON=${SPRING_APPLICATION_JSON:-}\n",
        "- SPRING_APPLICATION_JSON\n",
    )
)
map_form_line = "      SPRING_APPLICATION_JSON: ${SPRING_APPLICATION_JSON:-}\n"
if not list_form_present and map_form_line not in content:
    map_marker = "      SPRING_PROFILES_ACTIVE: prod\n"
    list_marker = "      - SPRING_PROFILES_ACTIVE=prod\n"
    if map_marker in content:
        content = content.replace(map_marker, map_form_line + map_marker, 1)
    elif list_marker in content:
        content = content.replace(
            list_marker,
            "      - SPRING_APPLICATION_JSON=${SPRING_APPLICATION_JSON:-}\n" + list_marker,
            1,
        )
    else:
        raise SystemExit("Unable to patch docker-compose.yml: backend environment marker not found")
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
