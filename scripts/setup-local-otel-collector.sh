#!/usr/bin/env bash
#
# setup-local-otel-collector.sh — install and run the OpenTelemetry
# collector as a native systemd user daemon for Claude Code telemetry.
#
# Background
# ----------
# Claude Code is configured at the user level to export tool-detail logs
# via OTLP to http://localhost:4317 (see ~/.claude/settings.json,
# OTEL_LOGS_EXPORTER=otlp, OTEL_LOG_TOOL_DETAILS=1). If nothing is
# listening on that port, Claude Code silently drops every tool call,
# rejection reason, and MCP event — you lose the observability the
# setting was meant to give you.
#
# Previously the collector ran via `docker compose` in
# ~/.claude/telemetry/. That setup is vulnerable to Docker Desktop's WSL
# bind-mount translation layer — a container restart or WSL path drift
# leaves the config bind-mount stale ("not a directory" OCI error), the
# container exits 127, and nobody notices until someone actually looks
# for a telemetry log. It's been broken in exactly that way for
# >11 days in at least one recorded case.
#
# This script replaces the docker-compose setup with a native systemd
# user unit. It:
#
#   1. Ensures the otelcol-contrib binary is present at
#      ~/.local/bin/otelcol-contrib (pinned version; verifies with the
#      binary's own --version, redownloads if missing or stale).
#   2. Ensures ~/.claude/telemetry/otel-collector-config.yaml points at
#      a host-local file path (the old compose config used the
#      container-internal /var/log/otel path which is meaningless for a
#      native daemon).
#   3. Writes ~/.config/systemd/user/claude-otel-collector.service.
#   4. Reloads the systemd user daemon, enables the unit, and starts it.
#   5. Verifies the collector is listening on 127.0.0.1:4317.
#
# Idempotent. Re-running after an upgrade updates the binary and the
# unit without touching user-owned log data.
#
# Usage
# -----
#   scripts/setup-local-otel-collector.sh               # install + start
#   OTEL_VERSION=0.150.0 scripts/setup-local-otel-collector.sh
#   SKIP_START=1 scripts/setup-local-otel-collector.sh  # install files only
#
# Prerequisites
# -------------
#   - curl, tar, sha256sum (standard on Ubuntu / WSL)
#   - systemd --user session available (verified at runtime)
#   - ~/.local/bin on PATH (existing Ground-Control dev convention)
#
# What it intentionally does NOT do
# ---------------------------------
#   - Touch the existing docker-compose.yml in ~/.claude/telemetry/. That
#     file is left in place for reference / rollback. The script will
#     fail if the dead container is holding port 4317, but in practice
#     an `Exited` container does not bind the host port.
#   - Modify ~/.claude/settings.json. The existing OTEL_* env vars
#     already point at localhost:4317 — no config change is needed
#     client-side.
#   - Take down the old docker-compose container. If the user wants to
#     purge it, `docker rm telemetry-otel-collector-1` after running
#     this script is safe.

set -euo pipefail

OTEL_VERSION="${OTEL_VERSION:-0.149.0}"
OTEL_BIN_DIR="${HOME}/.local/bin"
OTEL_BIN="${OTEL_BIN_DIR}/otelcol-contrib"
OTEL_DIR="${HOME}/.claude/telemetry"
CONFIG_PATH="${OTEL_DIR}/otel-collector-config.yaml"
DATA_DIR="${OTEL_DIR}/data"
LOG_PATH="${DATA_DIR}/claude-code.jsonl"
UNIT_DIR="${HOME}/.config/systemd/user"
UNIT_NAME="claude-otel-collector.service"
UNIT_PATH="${UNIT_DIR}/${UNIT_NAME}"

log() { printf '[otel-setup] %s\n' "$*"; }
fail() { printf '[otel-setup] ERROR: %s\n' "$*" >&2; exit 1; }

# Sanity checks -----------------------------------------------------------

command -v curl >/dev/null 2>&1 || fail "curl is required"
command -v tar >/dev/null 2>&1 || fail "tar is required"
command -v sha256sum >/dev/null 2>&1 || fail "sha256sum is required"

if ! systemctl --user status >/dev/null 2>&1; then
  fail "systemd --user session is not available on this host. \
The native-daemon install path requires a user session; if you are on \
WSL without systemd, re-enable systemd under [boot] in /etc/wsl.conf \
and run 'wsl --shutdown' before retrying."
fi

arch="$(uname -m)"
if [ "${arch}" != "x86_64" ]; then
  fail "unsupported architecture: ${arch} (only linux_amd64 is wired into this script)"
fi

mkdir -p "${OTEL_BIN_DIR}" "${OTEL_DIR}" "${DATA_DIR}" "${UNIT_DIR}"

# 1. Install binary -------------------------------------------------------

install_binary=1
if [ -x "${OTEL_BIN}" ]; then
  current_version=""
  if "${OTEL_BIN}" --version >/tmp/otelcol-version.$$ 2>&1; then
    current_version="$(awk '{print $NF}' /tmp/otelcol-version.$$ | head -1 | sed 's/^v//')"
  fi
  rm -f /tmp/otelcol-version.$$
  if [ "${current_version}" = "${OTEL_VERSION}" ]; then
    log "otelcol-contrib ${OTEL_VERSION} already installed at ${OTEL_BIN}"
    install_binary=0
  else
    log "otelcol-contrib present but version '${current_version}' != target '${OTEL_VERSION}' — reinstalling"
  fi
fi

if [ "${install_binary}" = "1" ]; then
  tmp_dir="$(mktemp -d)"
  trap 'rm -rf "${tmp_dir}"' EXIT
  asset="otelcol-contrib_${OTEL_VERSION}_linux_amd64.tar.gz"
  url="https://github.com/open-telemetry/opentelemetry-collector-releases/releases/download/v${OTEL_VERSION}/${asset}"
  log "downloading ${url}"
  curl -sSfL --connect-timeout 10 --max-time 120 -o "${tmp_dir}/${asset}" "${url}"
  log "extracting"
  tar -xzf "${tmp_dir}/${asset}" -C "${tmp_dir}"
  if [ ! -f "${tmp_dir}/otelcol-contrib" ]; then
    fail "expected otelcol-contrib binary in extracted tarball"
  fi
  install -m 0755 "${tmp_dir}/otelcol-contrib" "${OTEL_BIN}"
  log "installed ${OTEL_BIN}"
fi

# 2. Write collector config ----------------------------------------------

# Native-daemon config: bind to loopback only, receive OTLP gRPC,
# export logs to a rotating file the user can tail. Matches the shape
# of the previous docker-compose config but with a host-accessible
# path instead of the container-internal /var/log/otel path.
cat > "${CONFIG_PATH}.new" <<YAML
# Generated by scripts/setup-local-otel-collector.sh. Re-run the script
# to update; manual edits will be overwritten on the next install.

receivers:
  otlp:
    protocols:
      # gRPC endpoint is the one Claude Code's OTEL_EXPORTER_OTLP_ENDPOINT
      # points at by default (http://localhost:4317). HTTP (port 4318) is
      # enabled alongside it so simple tools (curl, grpcurl-less smoke
      # tests) can exercise the file exporter without a gRPC client.
      grpc:
        endpoint: 127.0.0.1:4317
      http:
        endpoint: 127.0.0.1:4318

exporters:
  file:
    path: ${LOG_PATH}
    rotation:
      max_megabytes: 100
      max_days: 90
      max_backups: 10

service:
  pipelines:
    logs:
      receivers: [otlp]
      exporters: [file]
    traces:
      receivers: [otlp]
      exporters: [file]
    metrics:
      receivers: [otlp]
      exporters: [file]
YAML

if [ -f "${CONFIG_PATH}" ] && cmp -s "${CONFIG_PATH}" "${CONFIG_PATH}.new"; then
  rm -f "${CONFIG_PATH}.new"
  log "config ${CONFIG_PATH} already up to date"
else
  mv "${CONFIG_PATH}.new" "${CONFIG_PATH}"
  log "wrote ${CONFIG_PATH}"
fi

# Ensure the log file exists with the right ownership. The old
# docker-compose bind-mount left it owned by uid 10001 (container
# user), which a native daemon running as the current user cannot
# write to. Replace it with an empty host-owned file if needed.
if [ -f "${LOG_PATH}" ] && [ ! -w "${LOG_PATH}" ]; then
  log "log file ${LOG_PATH} is not writable by $(id -un); replacing"
  rm -f "${LOG_PATH}"
fi
touch "${LOG_PATH}"

# 3. Write systemd user unit ---------------------------------------------

cat > "${UNIT_PATH}.new" <<UNIT
# Generated by scripts/setup-local-otel-collector.sh.
[Unit]
Description=Claude Code OpenTelemetry Collector
Documentation=https://opentelemetry.io/docs/collector/
After=default.target

[Service]
Type=simple
ExecStart=%h/.local/bin/otelcol-contrib --config=%h/.claude/telemetry/otel-collector-config.yaml
Restart=on-failure
RestartSec=5s
StandardOutput=journal
StandardError=journal
# Keep the daemon lightweight — the collector uses tens of MB by
# default; this ceiling catches runaway pipelines before they eat
# the dev box.
MemoryMax=256M

[Install]
WantedBy=default.target
UNIT

if [ -f "${UNIT_PATH}" ] && cmp -s "${UNIT_PATH}" "${UNIT_PATH}.new"; then
  rm -f "${UNIT_PATH}.new"
  log "unit ${UNIT_PATH} already up to date"
else
  mv "${UNIT_PATH}.new" "${UNIT_PATH}"
  log "wrote ${UNIT_PATH}"
fi

# 4. Reload + start -------------------------------------------------------

systemctl --user daemon-reload

if [ "${SKIP_START:-0}" = "1" ]; then
  log "SKIP_START=1; files installed but unit not enabled/started"
  exit 0
fi

# If the unit is already running, do a clean restart so the new config
# takes effect. This is the re-run path, and it has to handle the case
# where the unit is currently bound to :4317 itself — we must NOT treat
# that as an external conflict.
unit_was_active=0
if systemctl --user is-active --quiet "${UNIT_NAME}"; then
  unit_was_active=1
  log "${UNIT_NAME} already active; restarting to pick up config changes"
  systemctl --user restart "${UNIT_NAME}"
else
  # Only worry about foreign port holders when we are NOT already running.
  # An external process holding :4317 (e.g., the old docker-compose
  # container, or a stale collector started by hand) would otherwise
  # produce a misleading systemd bind error.
  if command -v ss >/dev/null 2>&1 && ss -tln 2>/dev/null | grep -q ':4317 '; then
    holder="$(ss -tlnp 2>/dev/null | awk '/:4317 / {print $NF}' | head -1 || true)"
    fail "port 4317 is already in use (${holder:-unknown}). \
Stop the existing listener before running this script. \
If it is the old docker-compose container, run: docker rm -f telemetry-otel-collector-1"
  fi
  systemctl --user enable --now "${UNIT_NAME}"
fi

# 5. Verify ---------------------------------------------------------------

sleep 1
if ! systemctl --user is-active --quiet "${UNIT_NAME}"; then
  systemctl --user status "${UNIT_NAME}" || true
  fail "${UNIT_NAME} did not become active. Check 'journalctl --user -u ${UNIT_NAME}' for details."
fi

if command -v ss >/dev/null 2>&1; then
  if ss -tln 2>/dev/null | grep -q '127.0.0.1:4317 '; then
    log "collector is listening on 127.0.0.1:4317"
  else
    log "WARNING: unit is active but nothing is listening on 127.0.0.1:4317 yet. It may still be starting — wait a moment and check with: ss -tln | grep 4317"
  fi
fi

log "done. Tail the output log with:"
log "  tail -F ${LOG_PATH}"
log "View unit logs with:"
log "  journalctl --user -u ${UNIT_NAME} -f"
