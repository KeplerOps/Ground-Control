# Pack Catalog

This directory contains the tracked metadata for control-pack source artifacts
that are intended to be imported into a Ground Control instance through the
pack registry.

The source of truth is [catalog.json](/home/atomik/src/Ground-Control/packs/catalog.json):

- It records pack IDs, versions, upstream source URLs, and source-file SHA-256
  digests.
- It is the input for the repo-native sync tool:
  `node tools/packs/sync_packs.mjs`.

The repo does not store the large upstream OSCAL payloads in git. The sync
tool downloads pinned upstream artifacts at runtime, verifies the SHA-256 from
the catalog, and imports them through the backend importer. That keeps a single
canonical OSCAL-to-control-pack conversion path without bloating the repository.

Current catalog:

- `nist-sp800-53-rev5`
- `nist-sp800-171-rev3`
- `nist-sp800-218-ssdf`

To sync the cataloged packs into a live Ground Control instance:

```sh
export GC_BASE_URL="http://gc-dev:8000"
export GROUND_CONTROL_PACK_REGISTRY_ADMIN_TOKEN="..."
node tools/packs/sync_packs.mjs
```

Requirements:

- The target instance must expose the pack-registry APIs.
- Pack-registry admin authentication must be configured on the server.
- The client must provide `GROUND_CONTROL_PACK_REGISTRY_ADMIN_TOKEN`.
