import { createHash } from "node:crypto";
import path from "node:path";

import { repoRoot } from "../ground_control/common.mjs";

export const DEFAULT_CATALOG_PATH = path.join(repoRoot, "packs", "catalog.json");

export async function loadPackCatalog(catalogPath = DEFAULT_CATALOG_PATH) {
  const raw = await readFile(catalogPath, "utf8");
  return JSON.parse(raw);
}

export function selectPackEntries(catalog, selectedPackIds = []) {
  if (selectedPackIds.length === 0) {
    return catalog.packs;
  }

  const selected = [];
  const byId = new Map(catalog.packs.map((entry) => [entry.packId, entry]));
  for (const packId of selectedPackIds) {
    const entry = byId.get(packId);
    if (!entry) {
      throw new Error(`Unknown packId in catalog: ${packId}`);
    }
    selected.push(entry);
  }
  return selected;
}

export function sha256Content(content) {
  return createHash("sha256").update(content).digest("hex");
}

export function sourceFileName(entry) {
  return path.posix.basename(new URL(entry.sourceUrl).pathname) || `${entry.packId}.json`;
}

export async function verifyCatalogEntry(entry, fetchImpl = fetch) {
  const response = await fetchImpl(entry.sourceUrl);
  if (!response.ok) {
    throw new Error(`Failed to download ${entry.packId} from ${entry.sourceUrl}: HTTP ${response.status}`);
  }
  const content = Buffer.from(await response.arrayBuffer());
  const actualHash = sha256Content(content);
  if (actualHash !== entry.sourceSha256) {
    throw new Error(
      `Source checksum mismatch for ${entry.packId}: expected ${entry.sourceSha256}, got ${actualHash}`,
    );
  }
  return { content, actualHash, fileName: sourceFileName(entry) };
}

export function determineInstallAction(installedPack, desiredVersion) {
  if (!installedPack) {
    return "INSTALL";
  }
  if (installedPack.version === desiredVersion) {
    return "SKIP";
  }
  return "UPGRADE";
}
