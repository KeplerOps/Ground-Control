#!/bin/bash
set -euo pipefail

RAW_BASE_URL="${1:?usage: sync_pack_catalog.sh <raw_base_url> [project] [ssm_parameter_path] [pack_ids_csv] [base_url]}"
PROJECT="${2:-ground-control}"
PARAM_PATH="${3:-/gc/dev/pack_registry_security_json}"
PACK_IDS_CSV="${4:-}"
BASE_URL="${5:-http://localhost:8000}"
AWS_REGION="${AWS_REGION:-us-east-2}"

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

catalog_path="${tmp_dir}/catalog.json"
curl -fsSL "${RAW_BASE_URL}/packs/catalog.json" -o "${catalog_path}"

spring_json="$(aws ssm get-parameter --region "${AWS_REGION}" --name "${PARAM_PATH}" --with-decryption --query 'Parameter.Value' --output text)"
auth_token="$(printf '%s' "${spring_json}" | jq -r '.["ground-control"]["pack-registry"]["security"]["admin-credentials"][0].token // empty')"
if [ -z "${auth_token}" ]; then
  echo "Could not resolve pack-registry admin token from ${PARAM_PATH}."
  exit 1
fi

pack_filter_json="$(jq -nc --arg raw "${PACK_IDS_CSV}" '$raw | split(",") | map(gsub("^\\s+|\\s+$"; "")) | map(select(length > 0))')"

pack_stream="$(jq -c --argjson selected "${pack_filter_json}" '
  . as $catalog
  | .packs[]
  | select(($selected | length) == 0 or (.packId as $id | $selected | index($id)))
  | . + { upstreamData: $catalog.upstreams[.upstream] }
' "${catalog_path}")"

while IFS= read -r entry; do
  [ -n "${entry}" ] || continue

  pack_id="$(printf '%s' "${entry}" | jq -r '.packId')"
  version="$(printf '%s' "${entry}" | jq -r '.version')"
  source_sha="$(printf '%s' "${entry}" | jq -r '.sourceSha256')"
  pack_url="$(printf '%s' "${entry}" | jq -r '.sourceUrl')"
  local_file="${tmp_dir}/$(basename "${pack_url}")"
  options_file="${tmp_dir}/${pack_id}-options.json"

  curl -fsSL "${pack_url}" -o "${local_file}"
  actual_sha="$(sha256sum "${local_file}" | awk '{print $1}')"
  if [ "${actual_sha}" != "${source_sha}" ]; then
    echo "${pack_id}: checksum mismatch for ${pack_url}"
    exit 1
  fi
  echo "${pack_id}: verified source artifact ${actual_sha}"

  registry_status="$(curl -s -o /dev/null -w '%{http_code}' \
    -H "Authorization: Bearer ${auth_token}" \
    "${BASE_URL}/api/v1/pack-registry/${pack_id}/${version}?project=${PROJECT}")"

  if [ "${registry_status}" != "200" ]; then
    printf '%s' "${entry}" | jq -c '{
      format,
      packId,
      version,
      publisher,
      description,
      sourceUrl,
      defaultControlFunction,
      provenance: {
        upstreamRepository: .upstreamData.repository,
        upstreamTag: .upstreamData.tag,
        upstreamCommit: .upstreamData.commit,
        sourceArtifactSha256: .sourceSha256,
        sourceArtifactUrl: .sourceUrl
      },
      registryMetadata: {
        curationSource: "repo-pack-catalog",
        sourceArtifactSha256: .sourceSha256
      }
    }' > "${options_file}"

    curl -fsSL -X POST \
      -H "Authorization: Bearer ${auth_token}" \
      -F "file=@${local_file};type=application/json" \
      -F "options=@${options_file};type=application/json" \
      "${BASE_URL}/api/v1/pack-registry/import?project=${PROJECT}" > /dev/null
    echo "${pack_id}: imported ${version}"
  else
    echo "${pack_id}: registry already has ${version}"
  fi

  installed_body="${tmp_dir}/${pack_id}-installed.json"
  installed_status="$(curl -s -o "${installed_body}" -w '%{http_code}' \
    "${BASE_URL}/api/v1/control-packs/${pack_id}?project=${PROJECT}")"

  if [ "${installed_status}" = "404" ]; then
    curl -fsSL -X POST \
      -H "Authorization: Bearer ${auth_token}" \
      -H "Content-Type: application/json" \
      -d "{\"packId\":\"${pack_id}\",\"versionConstraint\":\"${version}\"}" \
      "${BASE_URL}/api/v1/pack-install-records/install?project=${PROJECT}" > /dev/null
    echo "${pack_id}: installed ${version}"
    continue
  fi

  installed_version="$(jq -r '.version // empty' "${installed_body}")"
  if [ "${installed_version}" = "${version}" ]; then
    echo "${pack_id}: already installed at ${version}"
    continue
  fi

  curl -fsSL -X POST \
    -H "Authorization: Bearer ${auth_token}" \
    -H "Content-Type: application/json" \
    -d "{\"packId\":\"${pack_id}\",\"versionConstraint\":\"${version}\"}" \
    "${BASE_URL}/api/v1/pack-install-records/upgrade?project=${PROJECT}" > /dev/null
  echo "${pack_id}: upgraded ${installed_version} -> ${version}"
done <<< "${pack_stream}"
