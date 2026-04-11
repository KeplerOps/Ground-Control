import test from "node:test";
import assert from "node:assert/strict";

import { determineInstallAction, selectPackEntries, sourceFileName, verifyCatalogEntry } from "./pack_catalog.mjs";

test("determineInstallAction returns install when pack is absent", () => {
  assert.equal(determineInstallAction(null, "1.0.0"), "INSTALL");
});

test("determineInstallAction returns skip when versions match", () => {
  assert.equal(determineInstallAction({ version: "1.0.0" }, "1.0.0"), "SKIP");
});

test("determineInstallAction returns upgrade when versions differ", () => {
  assert.equal(determineInstallAction({ version: "1.0.0" }, "2.0.0"), "UPGRADE");
});

test("selectPackEntries rejects unknown pack IDs", () => {
  assert.throws(
    () => selectPackEntries({ packs: [{ packId: "known-pack" }] }, ["missing-pack"]),
    /Unknown packId/,
  );
});

test("sourceFileName derives the upload file name from the source URL", () => {
  assert.equal(
    sourceFileName({
      packId: "nist-sp800-53-rev5",
      sourceUrl: "https://example.test/catalogs/NIST_SP-800-53_rev5_catalog.json",
    }),
    "NIST_SP-800-53_rev5_catalog.json",
  );
});

test("verifyCatalogEntry downloads content and validates the source checksum", async () => {
  const content = Buffer.from('{"catalog":"ok"}', "utf8");
  const entry = {
    packId: "test-pack",
    sourceUrl: "https://example.test/test-pack.json",
    sourceSha256: "479f2d3b4bb807d2bbbb349a5aaf7cb7c44bbcf025410ca44cbd03deecd4e7c3",
  };
  const response = new Response(content, { status: 200 });
  const result = await verifyCatalogEntry(entry, async () => response);
  assert.equal(result.actualHash, entry.sourceSha256);
  assert.equal(result.fileName, "test-pack.json");
  assert.deepEqual(result.content, content);
});

test("verifyCatalogEntry rejects checksum drift", async () => {
  const entry = {
    packId: "test-pack",
    sourceUrl: "https://example.test/test-pack.json",
    sourceSha256: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
  };
  await assert.rejects(
    () =>
      verifyCatalogEntry(
        entry,
        async () => new Response(Buffer.from("{}", "utf8"), { status: 200 }),
      ),
    /Source checksum mismatch/,
  );
});
