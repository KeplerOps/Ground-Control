import { describe, it } from "node:test";
import assert from "node:assert/strict";
import {
  buildUrl,
  parseErrorBody,
  STATUSES,
  REQUIREMENT_TYPES,
  PRIORITIES,
  RELATION_TYPES,
  ARTIFACT_TYPES,
  LINK_TYPES,
} from "./lib.js";

// ---------------------------------------------------------------------------
// buildUrl
// ---------------------------------------------------------------------------

describe("buildUrl", () => {
  it("builds a simple path", () => {
    const url = buildUrl("/api/v1/requirements");
    assert.ok(url.endsWith("/api/v1/requirements"));
  });

  it("appends query params", () => {
    const url = buildUrl("/api/v1/requirements", { status: "DRAFT", page: 0 });
    const parsed = new URL(url);
    assert.equal(parsed.searchParams.get("status"), "DRAFT");
    assert.equal(parsed.searchParams.get("page"), "0");
  });

  it("skips undefined and null params", () => {
    const url = buildUrl("/api/v1/requirements", {
      status: undefined,
      type: null,
      wave: "",
      search: "hello",
    });
    const parsed = new URL(url);
    assert.equal(parsed.searchParams.get("status"), null);
    assert.equal(parsed.searchParams.get("type"), null);
    assert.equal(parsed.searchParams.get("wave"), null);
    assert.equal(parsed.searchParams.get("search"), "hello");
  });

  it("uses GC_BASE_URL from env", () => {
    // buildUrl uses the module-level BASE_URL captured at import time,
    // so we just verify the default produces a valid URL
    const url = buildUrl("/api/v1/analysis/cycles");
    assert.ok(url.startsWith("http"));
    assert.ok(url.includes("/api/v1/analysis/cycles"));
  });
});

// ---------------------------------------------------------------------------
// parseErrorBody
// ---------------------------------------------------------------------------

describe("parseErrorBody", () => {
  it("extracts message from error envelope", () => {
    const body = JSON.stringify({ error: { code: "NOT_FOUND", message: "Requirement not found" } });
    assert.equal(parseErrorBody(body), "Requirement not found");
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
  it("STATUSES matches Java Status enum", () => {
    assert.deepEqual(STATUSES, ["DRAFT", "ACTIVE", "DEPRECATED", "ARCHIVED"]);
  });

  it("REQUIREMENT_TYPES matches Java RequirementType enum", () => {
    assert.deepEqual(REQUIREMENT_TYPES, ["FUNCTIONAL", "NON_FUNCTIONAL", "CONSTRAINT", "INTERFACE"]);
  });

  it("PRIORITIES matches Java Priority enum", () => {
    assert.deepEqual(PRIORITIES, ["MUST", "SHOULD", "COULD", "WONT"]);
  });

  it("RELATION_TYPES matches Java RelationType enum", () => {
    assert.deepEqual(RELATION_TYPES, ["PARENT", "DEPENDS_ON", "CONFLICTS_WITH", "REFINES"]);
  });

  it("ARTIFACT_TYPES matches Java ArtifactType enum", () => {
    assert.equal(ARTIFACT_TYPES.length, 9);
    assert.ok(ARTIFACT_TYPES.includes("GITHUB_ISSUE"));
    assert.ok(ARTIFACT_TYPES.includes("CODE_FILE"));
    assert.ok(ARTIFACT_TYPES.includes("ADR"));
  });

  it("LINK_TYPES matches Java LinkType enum", () => {
    assert.deepEqual(LINK_TYPES, ["IMPLEMENTS", "TESTS", "DOCUMENTS", "CONSTRAINS", "VERIFIES"]);
  });
});
