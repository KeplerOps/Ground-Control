// Adapter-level tests for the GC-L007 GRC analysis kinds added to `gc_analyze`.
// Drives lib.js helpers (analyzeEvidenceFreshness, analyzeObservationProjection,
// aggregateVendorRisk) through a mocked fetch so the URL + query-parameter shape
// is locked. Per the GC-L007 codex preflight, MCP helper signatures, Zod field
// names, and backend query parameter names must be pinned by adapter tests.

import { describe, it, beforeEach, afterEach } from "node:test";
import assert from "node:assert/strict";
import {
  analyzeEvidenceFreshness,
  analyzeObservationProjection,
  aggregateVendorRisk,
} from "./lib.js";

const ORIGINAL_FETCH = globalThis.fetch;
const ORIGINAL_BASE_URL = process.env.GC_BASE_URL;
const ORIGINAL_API_TOKEN = process.env.GROUND_CONTROL_API_TOKEN;

function makeFetchSpy({ status = 200, body = {} } = {}) {
  const calls = [];
  globalThis.fetch = async (url, opts) => {
    calls.push({ url: url.toString(), method: opts?.method ?? "GET" });
    return new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    });
  };
  return calls;
}

beforeEach(() => {
  process.env.GC_BASE_URL = "https://gc.test";
  delete process.env.GROUND_CONTROL_API_TOKEN;
});

afterEach(() => {
  globalThis.fetch = ORIGINAL_FETCH;
  if (ORIGINAL_BASE_URL === undefined) delete process.env.GC_BASE_URL;
  else process.env.GC_BASE_URL = ORIGINAL_BASE_URL;
  if (ORIGINAL_API_TOKEN === undefined) delete process.env.GROUND_CONTROL_API_TOKEN;
  else process.env.GROUND_CONTROL_API_TOKEN = ORIGINAL_API_TOKEN;
});

describe("analyzeEvidenceFreshness (GC-L007)", () => {
  it("hits /api/v1/analysis/grc/evidence-freshness with camelCase params", async () => {
    const calls = makeFetchSpy({ body: { analysisKind: "evidence_freshness" } });

    await analyzeEvidenceFreshness({
      project: "ground-control",
      asOf: "2026-05-18T12:00:00Z",
      freshnessWindowDays: 30,
      includeSuperseded: true,
      assetId: "00000000-0000-0000-0000-000000000001",
      controlId: "00000000-0000-0000-0000-000000000002",
    });

    assert.equal(calls.length, 1);
    const url = new URL(calls[0].url);
    assert.equal(url.pathname, "/api/v1/analysis/grc/evidence-freshness");
    assert.equal(calls[0].method, "GET");
    assert.equal(url.searchParams.get("project"), "ground-control");
    assert.equal(url.searchParams.get("asOf"), "2026-05-18T12:00:00Z");
    assert.equal(url.searchParams.get("freshnessWindowDays"), "30");
    assert.equal(url.searchParams.get("includeSuperseded"), "true");
    assert.equal(url.searchParams.get("assetId"), "00000000-0000-0000-0000-000000000001");
    assert.equal(url.searchParams.get("controlId"), "00000000-0000-0000-0000-000000000002");
  });

  it("omits undefined params", async () => {
    const calls = makeFetchSpy();

    await analyzeEvidenceFreshness({ project: "ground-control" });

    const url = new URL(calls[0].url);
    assert.equal(url.searchParams.get("project"), "ground-control");
    assert.equal(url.searchParams.get("asOf"), null);
    assert.equal(url.searchParams.get("freshnessWindowDays"), null);
    assert.equal(url.searchParams.get("includeSuperseded"), null);
    assert.equal(url.searchParams.get("assetId"), null);
    assert.equal(url.searchParams.get("controlId"), null);
  });

  it("returns the JSON body", async () => {
    makeFetchSpy({ body: { analysisKind: "evidence_freshness", counts: { fresh: 3 } } });

    const result = await analyzeEvidenceFreshness({ project: "ground-control" });

    assert.equal(result.analysisKind, "evidence_freshness");
    assert.equal(result.counts.fresh, 3);
  });
});

describe("analyzeObservationProjection (GC-L007)", () => {
  it("hits /api/v1/analysis/grc/observation-projection with mode=ASSET_EXPOSURE", async () => {
    const calls = makeFetchSpy({ body: { analysisKind: "observation_exposure" } });

    await analyzeObservationProjection({
      project: "ground-control",
      asOf: "2026-05-18T12:00:00Z",
      mode: "ASSET_EXPOSURE",
      assetId: "00000000-0000-0000-0000-000000000001",
    });

    const url = new URL(calls[0].url);
    assert.equal(url.pathname, "/api/v1/analysis/grc/observation-projection");
    assert.equal(url.searchParams.get("project"), "ground-control");
    assert.equal(url.searchParams.get("asOf"), "2026-05-18T12:00:00Z");
    assert.equal(url.searchParams.get("mode"), "ASSET_EXPOSURE");
    assert.equal(url.searchParams.get("assetId"), "00000000-0000-0000-0000-000000000001");
    assert.equal(url.searchParams.get("controlId"), null);
  });

  it("supports CONTROL_STATE mode with control filter", async () => {
    const calls = makeFetchSpy();

    await analyzeObservationProjection({
      project: "ground-control",
      mode: "CONTROL_STATE",
      controlId: "00000000-0000-0000-0000-000000000003",
    });

    const url = new URL(calls[0].url);
    assert.equal(url.searchParams.get("mode"), "CONTROL_STATE");
    assert.equal(url.searchParams.get("controlId"), "00000000-0000-0000-0000-000000000003");
  });

  it("returns the JSON body", async () => {
    // Mirrors the evidence_freshness "returns the JSON body" test — locks in
    // that the helper actually parses the response, not just dispatches the
    // request. lib.js's request() applies toSnakeCase to the response, but
    // toSnakeCase only renames keys that are in the TO_CAMEL/TO_SNAKE table
    // (e.g. controlUid → control_uid). Keys not in the table pass through
    // unchanged, so analysisKind stays camelCase.
    makeFetchSpy({ body: { analysisKind: "control_state", controlStates: [{ controlUid: "CTRL-1" }] } });

    const result = await analyzeObservationProjection({
      project: "ground-control",
      mode: "CONTROL_STATE",
    });

    assert.equal(result.analysisKind, "control_state");
    // controlUid is in the snake-case mapping (see lib.js TO_CAMEL); the
    // outer controlStates key is not.
    assert.equal(result.controlStates[0].control_uid, "CTRL-1");
  });
});

describe("aggregateVendorRisk (GC-L007)", () => {
  it("hits /api/v1/analysis/grc/vendor-risk with camelCase params", async () => {
    const calls = makeFetchSpy({ body: { analysisKind: "vendor_risk_aggregation" } });

    await aggregateVendorRisk({
      project: "ground-control",
      asOf: "2026-05-18T12:00:00Z",
      freshnessWindowDays: 60,
      vendorAssetId: "00000000-0000-0000-0000-00000000000a",
    });

    const url = new URL(calls[0].url);
    assert.equal(url.pathname, "/api/v1/analysis/grc/vendor-risk");
    assert.equal(url.searchParams.get("project"), "ground-control");
    assert.equal(url.searchParams.get("asOf"), "2026-05-18T12:00:00Z");
    assert.equal(url.searchParams.get("freshnessWindowDays"), "60");
    assert.equal(url.searchParams.get("vendorAssetId"), "00000000-0000-0000-0000-00000000000a");
  });

  it("omits undefined params", async () => {
    const calls = makeFetchSpy();

    await aggregateVendorRisk({ project: "ground-control" });

    const url = new URL(calls[0].url);
    assert.equal(url.searchParams.get("project"), "ground-control");
    assert.equal(url.searchParams.get("asOf"), null);
    assert.equal(url.searchParams.get("freshnessWindowDays"), null);
    assert.equal(url.searchParams.get("vendorAssetId"), null);
  });
});
