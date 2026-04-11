import process from "node:process";

import {
  DEFAULT_CATALOG_PATH,
  determineInstallAction,
  loadPackCatalog,
  selectPackEntries,
  verifyCatalogEntry,
} from "./pack_catalog.mjs";

function parseArgs(argv) {
  const options = {
    catalogPath: DEFAULT_CATALOG_PATH,
    project: null,
    baseUrl: process.env.GC_BASE_URL ?? null,
    dryRun: false,
    importOnly: false,
    installOnly: false,
    packIds: [],
  };

  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    switch (arg) {
      case "--catalog":
        options.catalogPath = requireValue(argv, ++i, "--catalog");
        break;
      case "--project":
        options.project = requireValue(argv, ++i, "--project");
        break;
      case "--base-url":
        options.baseUrl = requireValue(argv, ++i, "--base-url");
        break;
      case "--pack":
        options.packIds.push(requireValue(argv, ++i, "--pack"));
        break;
      case "--dry-run":
        options.dryRun = true;
        break;
      case "--import-only":
        options.importOnly = true;
        break;
      case "--install-only":
        options.installOnly = true;
        break;
      case "--help":
        printHelp();
        process.exit(0);
      default:
        throw new Error(`Unknown argument: ${arg}`);
    }
  }

  if (options.importOnly && options.installOnly) {
    throw new Error("Choose at most one of --import-only or --install-only");
  }

  return options;
}

function requireValue(argv, index, flag) {
  const value = argv[index];
  if (!value) {
    throw new Error(`${flag} requires a value`);
  }
  return value;
}

function printHelp() {
  console.log(`Usage: node tools/packs/sync_packs.mjs [options]

Options:
  --catalog <path>    Use a different pack catalog JSON file
  --project <id>      Override the Ground Control project identifier
  --base-url <url>    Override GC_BASE_URL
  --pack <packId>     Sync only the selected pack (repeatable)
  --dry-run           Print planned actions without mutating the server
  --import-only       Register catalog entries but skip install/upgrade
  --install-only      Skip import and only install/upgrade existing registry entries
  --help              Show this help text
`);
}

function adminToken() {
  return process.env.GROUND_CONTROL_PACK_REGISTRY_ADMIN_TOKEN ?? null;
}

function ensureConfigured(baseUrl) {
  if (!baseUrl) {
    throw new Error("GC_BASE_URL is required. Pass --base-url or export GC_BASE_URL.");
  }
  if (!adminToken()) {
    throw new Error(
      "GROUND_CONTROL_PACK_REGISTRY_ADMIN_TOKEN is required for pack registry import/install operations.",
    );
  }
}

function buildUrl(baseUrl, pathname, params = {}) {
  const url = new URL(pathname, baseUrl);
  for (const [key, value] of Object.entries(params)) {
    if (value != null && value !== "") {
      url.searchParams.set(key, value);
    }
  }
  return url;
}

async function apiRequest(baseUrl, method, pathname, { params, body, formData, auth = false } = {}) {
  const headers = {};
  if (auth) {
    const token = adminToken();
    if (!token) {
      throw new Error("GROUND_CONTROL_PACK_REGISTRY_ADMIN_TOKEN is not set.");
    }
    headers.Authorization = `Bearer ${token}`;
  }
  if (body) {
    headers["Content-Type"] = "application/json";
  }

  const response = await fetch(buildUrl(baseUrl, pathname, params), {
    method,
    headers,
    body: body ? JSON.stringify(body) : formData,
  });

  const rawText = await response.text();
  const data = rawText ? tryParseJson(rawText) : null;
  if (!response.ok) {
    throw toApiError(method, pathname, response.status, data, rawText);
  }
  return data;
}

function tryParseJson(text) {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function toApiError(method, pathname, status, data, rawText) {
  const message =
    typeof data === "object" && data !== null
      ? data.error?.message ?? data.message ?? rawText
      : rawText;
  if (status === 401 && String(message).includes("authentication is not configured")) {
    return new Error(
      `Pack registry admin authentication is not configured on the server. ${method} ${pathname} cannot proceed.`,
    );
  }
  const error = new Error(`${method} ${pathname} failed with ${status}: ${message}`);
  error.status = status;
  return error;
}

async function getRegistryEntry(baseUrl, project, packId, version) {
  try {
    return await apiRequest(baseUrl, "GET", `/api/v1/pack-registry/${encodeURIComponent(packId)}/${encodeURIComponent(version)}`, {
      params: { project },
      auth: true,
    });
  } catch (error) {
    if (error.status === 404) {
      return null;
    }
    throw error;
  }
}

async function getInstalledPack(baseUrl, project, packId) {
  try {
    return await apiRequest(baseUrl, "GET", `/api/v1/control-packs/${encodeURIComponent(packId)}`, {
      params: { project },
    });
  } catch (error) {
    if (error.status === 404) {
      return null;
    }
    throw error;
  }
}

async function importPack(baseUrl, project, entry, fileName, content, catalog) {
  const form = new FormData();
  form.append("file", new Blob([content], { type: "application/json" }), fileName);

  const upstream = catalog.upstreams[entry.upstream];
  if (!upstream) {
    throw new Error(`Unknown upstream '${entry.upstream}' for ${entry.packId}`);
  }
  const options = {
    format: entry.format,
    packId: entry.packId,
    version: entry.version,
    publisher: entry.publisher,
    description: entry.description,
    sourceUrl: entry.sourceUrl,
    defaultControlFunction: entry.defaultControlFunction,
    provenance: {
      upstreamRepository: upstream.repository,
      upstreamTag: upstream.tag,
      upstreamCommit: upstream.commit,
      sourceArtifactSha256: entry.sourceSha256,
      sourceArtifactUrl: entry.sourceUrl,
    },
    registryMetadata: {
      curationSource: "repo-pack-catalog",
      sourceArtifactSha256: entry.sourceSha256,
    },
  };
  form.append(
    "options",
    new Blob([JSON.stringify(options)], { type: "application/json" }),
    "options.json",
  );

  return apiRequest(baseUrl, "POST", "/api/v1/pack-registry/import", {
    params: { project },
    formData: form,
    auth: true,
  });
}

async function installPack(baseUrl, project, entry, action) {
  const path = action === "INSTALL"
    ? "/api/v1/pack-install-records/install"
    : "/api/v1/pack-install-records/upgrade";
  return apiRequest(baseUrl, "POST", path, {
    params: { project },
    body: {
      packId: entry.packId,
      versionConstraint: entry.version,
    },
    auth: true,
  });
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  ensureConfigured(options.baseUrl);

  const catalog = await loadPackCatalog(options.catalogPath);
  const project = options.project ?? catalog.project;
  const selectedEntries = selectPackEntries(catalog, options.packIds);

  for (const entry of selectedEntries) {
    const { content, actualHash, fileName } = await verifyCatalogEntry(entry);
    console.log(`${entry.packId}: verified source artifact ${actualHash}`);

    const registryEntry =
      options.installOnly
        ? null
        : await getRegistryEntry(options.baseUrl, project, entry.packId, entry.version);

    if (!options.installOnly) {
      if (registryEntry) {
        console.log(`${entry.packId}: registry already has ${entry.version}`);
      } else if (options.dryRun) {
        console.log(`${entry.packId}: would import ${entry.version} from ${entry.sourceUrl}`);
      } else {
        await importPack(options.baseUrl, project, entry, fileName, content, catalog);
        console.log(`${entry.packId}: imported ${entry.version}`);
      }
    }

    if (options.importOnly) {
      continue;
    }

    const installedPack = await getInstalledPack(options.baseUrl, project, entry.packId);
    const action = determineInstallAction(installedPack, entry.version);
    if (action === "SKIP") {
      console.log(`${entry.packId}: already installed at ${entry.version}`);
      continue;
    }

    if (options.dryRun) {
      console.log(`${entry.packId}: would ${action.toLowerCase()} to ${entry.version}`);
      continue;
    }

    const result = await installPack(options.baseUrl, project, entry, action);
    console.log(`${entry.packId}: ${result.installOutcome.toLowerCase()} ${result.resolvedVersion}`);
  }
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
