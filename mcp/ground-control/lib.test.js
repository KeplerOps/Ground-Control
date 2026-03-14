import { describe, it } from "node:test";
import assert from "node:assert/strict";
import {
  buildUrl,
  parseErrorBody,
  formatIssueBody,
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
// formatIssueBody
// ---------------------------------------------------------------------------

describe("formatIssueBody", () => {
  it("formats a full requirement with all fields", () => {
    const req = {
      uid: "GC-D007",
      requirement_type: "FUNCTIONAL",
      priority: "SHOULD",
      wave: 1,
      status: "DRAFT",
      statement: "The system shall create GitHub issues.",
      rationale: "Reduces manual copy-paste during wave activation.",
    };
    const body = formatIssueBody(req);
    assert.ok(body.includes("> **GC-D007** | FUNCTIONAL | SHOULD | Wave 1 | DRAFT"));
    assert.ok(body.includes("## Statement"));
    assert.ok(body.includes("The system shall create GitHub issues."));
    assert.ok(body.includes("## Rationale"));
    assert.ok(body.includes("Reduces manual copy-paste during wave activation."));
    assert.ok(body.includes("*Created from Ground Control requirement GC-D007*"));
  });

  it("omits rationale and wave when null", () => {
    const req = {
      uid: "GC-A001",
      requirement_type: "CONSTRAINT",
      priority: "MUST",
      wave: null,
      status: "ACTIVE",
      statement: "Constraints apply.",
      rationale: null,
    };
    const body = formatIssueBody(req);
    assert.ok(body.includes("> **GC-A001** | CONSTRAINT | MUST | ACTIVE"));
    assert.ok(!body.includes("Wave"));
    assert.ok(!body.includes("## Rationale"));
  });

  it("appends extra body text", () => {
    const req = {
      uid: "GC-T001",
      statement: "Test requirement.",
    };
    const body = formatIssueBody(req, "## Acceptance Criteria\n- [ ] Done");
    assert.ok(body.includes("## Acceptance Criteria"));
    assert.ok(body.includes("- [ ] Done"));
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
    assert.deepEqual(RELATION_TYPES, ["PARENT", "DEPENDS_ON", "CONFLICTS_WITH", "REFINES", "SUPERSEDES", "RELATED"]);
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
