import { describe, it } from "node:test";
import assert from "node:assert/strict";
import {
  buildUrl,
  parseErrorBody,
  formatIssueBody,
  buildGroundControlContextSnippet,
  parseRepoGroundControlContext,
  buildCodexArchitecturePreflightPrompt,
  buildCodexArchitectureExecArgs,
  buildCodexReviewPrompt,
  buildCodexReviewArgs,
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
  const originalBaseUrl = process.env.GC_BASE_URL;

  function withBaseUrl(baseUrl, fn) {
    if (baseUrl === undefined) {
      delete process.env.GC_BASE_URL;
    } else {
      process.env.GC_BASE_URL = baseUrl;
    }
    try {
      fn();
    } finally {
      if (originalBaseUrl === undefined) {
        delete process.env.GC_BASE_URL;
      } else {
        process.env.GC_BASE_URL = originalBaseUrl;
      }
    }
  }

  it("builds a simple path", () => {
    withBaseUrl("http://gc-dev:8000", () => {
      const url = buildUrl("/api/v1/requirements");
      assert.ok(url.endsWith("/api/v1/requirements"));
    });
  });

  it("appends query params", () => {
    withBaseUrl("http://gc-dev:8000", () => {
      const url = buildUrl("/api/v1/requirements", { status: "DRAFT", page: 0 });
      const parsed = new URL(url);
      assert.equal(parsed.searchParams.get("status"), "DRAFT");
      assert.equal(parsed.searchParams.get("page"), "0");
    });
  });

  it("skips undefined and null params", () => {
    withBaseUrl("http://gc-dev:8000", () => {
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
  });

  it("uses GC_BASE_URL from env", () => {
    withBaseUrl("http://gc-dev:8000", () => {
      const url = buildUrl("/api/v1/analysis/cycles");
      assert.ok(url.startsWith("http://gc-dev:8000"));
      assert.ok(url.includes("/api/v1/analysis/cycles"));
    });
  });

  it("fails fast when GC_BASE_URL is unset", () => {
    withBaseUrl(undefined, () => {
      assert.throws(
        () => buildUrl("/api/v1/analysis/cycles"),
        /GC_BASE_URL must be set/,
      );
    });
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
// Ground Control context helpers
// ---------------------------------------------------------------------------

describe("buildGroundControlContextSnippet", () => {
  it("renders the standardized AGENTS.md snippet", () => {
    const snippet = buildGroundControlContextSnippet("aces-sdl");
    assert.ok(snippet.includes("## Ground Control Context"));
    assert.ok(snippet.includes("ground_control:"));
    assert.ok(snippet.includes("project: aces-sdl"));
  });
});

describe("parseRepoGroundControlContext", () => {
  it("parses a valid Ground Control Context section", () => {
    const result = parseRepoGroundControlContext(`
# Agent Instructions

## Ground Control Context

\`\`\`yaml
ground_control:
  project: aces-sdl
\`\`\`
`);

    assert.equal(result.status, "ok");
    assert.equal(result.project, "aces-sdl");
    assert.deepEqual(result.errors, []);
  });

  it("reports a missing Ground Control Context section", () => {
    const result = parseRepoGroundControlContext("# Agent Instructions\n");

    assert.equal(result.status, "missing_ground_control_context");
    assert.equal(result.project, null);
    assert.ok(result.errors[0].includes("Ground Control Context"));
    assert.ok(result.suggested_agents_snippet.includes("ground_control:"));
  });

  it("reports an invalid project identifier", () => {
    const result = parseRepoGroundControlContext(`
## Ground Control Context

\`\`\`yaml
ground_control:
  project: ACES_SDL
\`\`\`
`);

    assert.equal(result.status, "invalid_ground_control_context");
    assert.equal(result.project, null);
    assert.ok(result.errors[0].includes("lowercase identifier"));
  });
});

// ---------------------------------------------------------------------------
// Codex workflow helpers
// ---------------------------------------------------------------------------

describe("buildCodexArchitecturePreflightPrompt", () => {
  it("captures the architecture-preflight guardrails", () => {
    const prompt = buildCodexArchitecturePreflightPrompt({
      requirement: {
        uid: "GC-A123",
        title: "Shared Concept Authority",
        statement: "The system shall define a canonical concept authority.",
      },
      traceabilityLinks: [
        {
          artifact_type: "ADR",
          artifact_identifier: "ADR-012",
          artifact_title: "Shared Concept Authority",
          link_type: "DOCUMENTS",
        },
      ],
      issueContext: { number: 501, title: "Implement GC-A123" },
    });

    assert.ok(prompt.includes("Do not implement the requirement itself."));
    assert.ok(prompt.includes("top-tier production engineering bar"));
    assert.ok(prompt.includes("GC-A123"));
    assert.ok(prompt.includes("ADR-012"));
    assert.ok(prompt.includes("\"number\": 501"));
    assert.ok(prompt.includes("gotchas and anti-patterns"));
  });
});

describe("buildCodexArchitectureExecArgs", () => {
  it("builds codex exec args with workspace-write, stdin prompt, and output capture", () => {
    const args = buildCodexArchitectureExecArgs({
      repoPath: "/tmp/repo",
      outputPath: "/tmp/out.txt",
    });

    assert.deepEqual(args, [
      "exec",
      "--ephemeral",
      "--sandbox",
      "workspace-write",
      "-C",
      "/tmp/repo",
      "--output-last-message",
      "/tmp/out.txt",
      "-",
    ]);
  });
});

describe("buildCodexReviewPrompt", () => {
  it("demands an exhaustive non-triaged review", () => {
    const prompt = buildCodexReviewPrompt("dev");
    assert.ok(prompt.includes("Review the changes against dev."));
    assert.ok(prompt.includes("Enumerate all material issues"));
    assert.ok(prompt.includes("Do not prioritize"));
    assert.ok(prompt.includes("The caller intends to fix everything now."));
    assert.ok(prompt.includes("precise file and line references"));
  });
});

describe("buildCodexReviewArgs", () => {
  it("builds args for a committed branch review", () => {
    const args = buildCodexReviewArgs({
      baseBranch: "dev",
      uncommitted: false,
    });
    assert.deepEqual(args, ["review", "--base", "dev", "-"]);
  });

  it("adds the uncommitted flag when requested", () => {
    const args = buildCodexReviewArgs({
      baseBranch: "main",
      uncommitted: true,
    });
    assert.deepEqual(args, ["review", "--base", "main", "--uncommitted", "-"]);
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
    assert.deepEqual(ARTIFACT_TYPES, [
      "GITHUB_ISSUE",
      "PULL_REQUEST",
      "CODE_FILE",
      "ADR",
      "CONFIG",
      "POLICY",
      "TEST",
      "SPEC",
      "PROOF",
      "DOCUMENTATION",
      "RISK_SCENARIO",
      "CONTROL",
    ]);
  });

  it("LINK_TYPES matches Java LinkType enum", () => {
    assert.deepEqual(LINK_TYPES, ["IMPLEMENTS", "TESTS", "DOCUMENTS", "CONSTRAINS", "VERIFIES"]);
  });
});
