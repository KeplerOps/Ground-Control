import { describe, it } from "node:test";
import assert from "node:assert/strict";
import {
  buildUrl,
  parseErrorBody,
  WORKFLOW_STATUSES,
  NODE_TYPES,
  TRIGGER_TYPES,
} from "./lib.js";

// ---------------------------------------------------------------------------
// buildUrl
// ---------------------------------------------------------------------------

describe("buildUrl", () => {
  it("builds a simple path", () => {
    const url = buildUrl("/api/v1/workflows");
    assert.ok(url.endsWith("/api/v1/workflows"));
  });

  it("appends query params", () => {
    const url = buildUrl("/api/v1/workflows", { workspace: "ws-1" });
    const parsed = new URL(url);
    assert.equal(parsed.searchParams.get("workspace"), "ws-1");
  });

  it("skips undefined and null params", () => {
    const url = buildUrl("/api/v1/workflows", {
      workspace: undefined,
      status: null,
      name: "",
      search: "hello",
    });
    const parsed = new URL(url);
    assert.equal(parsed.searchParams.get("workspace"), null);
    assert.equal(parsed.searchParams.get("status"), null);
    assert.equal(parsed.searchParams.get("name"), null);
    assert.equal(parsed.searchParams.get("search"), "hello");
  });

  it("uses GC_BASE_URL from env", () => {
    const url = buildUrl("/api/v1/workspaces");
    assert.ok(url.startsWith("http"));
    assert.ok(url.includes("/api/v1/workspaces"));
  });
});

// ---------------------------------------------------------------------------
// parseErrorBody
// ---------------------------------------------------------------------------

describe("parseErrorBody", () => {
  it("extracts message from error envelope", () => {
    const body = JSON.stringify({ error: { code: "NOT_FOUND", message: "Workflow not found" } });
    assert.equal(parseErrorBody(body), "Workflow not found");
  });

  it("returns raw text for non-JSON", () => {
    assert.equal(parseErrorBody("Internal Server Error"), "Internal Server Error");
  });

  it("returns raw text for unexpected JSON shape", () => {
    const body = JSON.stringify({ status: 500 });
    assert.equal(parseErrorBody(body), body);
  });
});

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

describe("constants", () => {
  it("WORKFLOW_STATUSES contains expected values", () => {
    assert.deepEqual(WORKFLOW_STATUSES, ["DRAFT", "ACTIVE", "PAUSED", "ARCHIVED"]);
  });

  it("NODE_TYPES contains expected values", () => {
    assert.equal(NODE_TYPES.length, 9);
    assert.ok(NODE_TYPES.includes("TRIGGER"));
    assert.ok(NODE_TYPES.includes("ACTION"));
    assert.ok(NODE_TYPES.includes("CONDITION"));
    assert.ok(NODE_TYPES.includes("HTTP_REQUEST"));
    assert.ok(NODE_TYPES.includes("CODE"));
  });

  it("TRIGGER_TYPES contains expected values", () => {
    assert.deepEqual(TRIGGER_TYPES, ["WEBHOOK", "SCHEDULE", "EVENT", "MANUAL"]);
  });
});
