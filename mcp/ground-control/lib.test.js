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
  RESEARCH_PHASES,
  RESEARCH_MODES,
  HIGH_BLAST_RADIUS_TECHNIQUES,
  RESEARCH_QUESTION_UID_RE,
  validateResearchUid,
  selectResearchPhases,
  requiresSafetyPreflight,
  parseSafetyPreflightChecklist,
  buildResearchCharterPrompt,
  buildResearchLitReviewPrompt,
  buildResearchMethodologyPreflightPrompt,
  buildResearchSafetyPreflightPrompt,
  buildResearchSynthesisReviewPrompt,
  buildResearchExecArgs,
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

// ---------------------------------------------------------------------------
// Research workflow constants (ADR-024 / ADR-025 / ADR-026)
// ---------------------------------------------------------------------------

describe("research workflow constants", () => {
  it("RESEARCH_PHASES enumerates the ten phases in order", () => {
    assert.deepEqual(RESEARCH_PHASES, [
      "CHARTER",
      "LIT_REVIEW",
      "METHODOLOGY",
      "PROTOCOL",
      "SAFETY_PREFLIGHT",
      "EXECUTION",
      "ANALYSIS",
      "SYNTHESIS",
      "PEER_REVIEW",
      "PUBLICATION",
    ]);
  });

  it("RESEARCH_MODES matches the charter mode selector", () => {
    assert.deepEqual(RESEARCH_MODES, [
      "LITERATURE",
      "EXPERIMENTAL",
      "ADVERSARIAL_LAB",
      "MIXED",
    ]);
  });

  it("HIGH_BLAST_RADIUS_TECHNIQUES covers ADR-026 default categories", () => {
    assert.ok(Array.isArray(HIGH_BLAST_RADIUS_TECHNIQUES));
    const lowered = HIGH_BLAST_RADIUS_TECHNIQUES.map((t) => t.toLowerCase());
    for (const required of [
      "lateral_movement",
      "credential_theft",
      "persistence",
      "destructive_action",
      "exploitation_unpatched",
      "social_engineering_humans",
    ]) {
      assert.ok(lowered.includes(required), `missing technique: ${required}`);
    }
  });

  it("RESEARCH_QUESTION_UID_RE accepts research question and hypothesis UIDs", () => {
    assert.ok(RESEARCH_QUESTION_UID_RE.test("APTL-RQ001"));
    assert.ok(RESEARCH_QUESTION_UID_RE.test("APTL-H001"));
    assert.ok(RESEARCH_QUESTION_UID_RE.test("GC-RQ123"));
  });

  it("RESEARCH_QUESTION_UID_RE rejects malformed UIDs", () => {
    for (const bad of [
      "aptl-rq001",
      "APTL-RQ01",
      "APTL-Q001",
      "APTL_RQ001",
      "APTL-RQ-001",
      "RQ001",
      "",
    ]) {
      assert.equal(
        RESEARCH_QUESTION_UID_RE.test(bad),
        false,
        `should reject: ${bad}`,
      );
    }
  });
});

// ---------------------------------------------------------------------------
// validateResearchUid
// ---------------------------------------------------------------------------

describe("validateResearchUid", () => {
  it("accepts a valid research question UID and reports its kind", () => {
    const result = validateResearchUid("APTL-RQ001");
    assert.equal(result.ok, true);
    assert.equal(result.kind, "RQ");
    assert.equal(result.project_prefix, "APTL");
    assert.deepEqual(result.errors, []);
  });

  it("accepts a valid hypothesis UID", () => {
    const result = validateResearchUid("GC-H042");
    assert.equal(result.ok, true);
    assert.equal(result.kind, "H");
    assert.equal(result.project_prefix, "GC");
  });

  it("rejects a lowercase UID with a useful error", () => {
    const result = validateResearchUid("aptl-rq001");
    assert.equal(result.ok, false);
    assert.ok(result.errors.length > 0);
    assert.ok(result.errors[0].toLowerCase().includes("uid"));
  });

  it("rejects a non-string", () => {
    const result = validateResearchUid(null);
    assert.equal(result.ok, false);
    assert.ok(result.errors[0].toLowerCase().includes("uid"));
  });

  it("rejects software-style requirement UIDs that do not encode RQ or H", () => {
    const result = validateResearchUid("GC-J001");
    assert.equal(result.ok, false);
    assert.ok(result.errors[0].toLowerCase().includes("rq"));
  });
});

// ---------------------------------------------------------------------------
// selectResearchPhases
// ---------------------------------------------------------------------------

describe("selectResearchPhases", () => {
  it("returns all ten phases for full mode", () => {
    const result = selectResearchPhases({ mode: "MIXED", from: "CHARTER", to: "PUBLICATION" });
    assert.deepEqual(result.phases, RESEARCH_PHASES);
    assert.deepEqual(result.errors, []);
  });

  it("slices a contiguous range with from/to", () => {
    const result = selectResearchPhases({
      mode: "EXPERIMENTAL",
      from: "METHODOLOGY",
      to: "PROTOCOL",
    });
    assert.deepEqual(result.phases, ["METHODOLOGY", "PROTOCOL"]);
  });

  it("forces SAFETY_PREFLIGHT into the plan for ADVERSARIAL_LAB mode", () => {
    const result = selectResearchPhases({
      mode: "ADVERSARIAL_LAB",
      from: "PROTOCOL",
      to: "EXECUTION",
    });
    assert.ok(result.phases.includes("SAFETY_PREFLIGHT"));
    const safetyIdx = result.phases.indexOf("SAFETY_PREFLIGHT");
    const execIdx = result.phases.indexOf("EXECUTION");
    assert.ok(safetyIdx < execIdx, "SAFETY_PREFLIGHT must precede EXECUTION");
  });

  it("rejects skipping SAFETY_PREFLIGHT when ADVERSARIAL_LAB requires it", () => {
    const result = selectResearchPhases({
      mode: "ADVERSARIAL_LAB",
      from: "CHARTER",
      to: "PUBLICATION",
      skip: ["SAFETY_PREFLIGHT"],
    });
    assert.ok(result.errors.length > 0);
    assert.ok(result.errors[0].toLowerCase().includes("safety"));
  });

  it("rejects unknown mode", () => {
    const result = selectResearchPhases({ mode: "MAYBE", from: "CHARTER", to: "CHARTER" });
    assert.ok(result.errors.length > 0);
    assert.ok(result.errors[0].toLowerCase().includes("mode"));
  });

  it("rejects unknown phase in from/to", () => {
    const result = selectResearchPhases({ mode: "LITERATURE", from: "INTRO", to: "CHARTER" });
    assert.ok(result.errors.length > 0);
  });

  it("rejects from after to", () => {
    const result = selectResearchPhases({ mode: "LITERATURE", from: "SYNTHESIS", to: "CHARTER" });
    assert.ok(result.errors.length > 0);
    assert.ok(result.errors[0].toLowerCase().includes("from"));
  });

  it("supports the lit-review-only convenience slice", () => {
    const result = selectResearchPhases({
      mode: "LITERATURE",
      from: "CHARTER",
      to: "PUBLICATION",
      skip: ["METHODOLOGY", "PROTOCOL", "SAFETY_PREFLIGHT", "EXECUTION", "ANALYSIS"],
    });
    assert.deepEqual(result.errors, []);
    assert.deepEqual(result.phases, ["CHARTER", "LIT_REVIEW", "SYNTHESIS", "PEER_REVIEW", "PUBLICATION"]);
  });
});

// ---------------------------------------------------------------------------
// requiresSafetyPreflight
// ---------------------------------------------------------------------------

describe("requiresSafetyPreflight", () => {
  it("returns true for ADVERSARIAL_LAB mode regardless of techniques", () => {
    assert.equal(
      requiresSafetyPreflight({ mode: "ADVERSARIAL_LAB", techniques: [] }),
      true,
    );
  });

  it("returns false for LITERATURE mode without high-blast techniques", () => {
    assert.equal(
      requiresSafetyPreflight({ mode: "LITERATURE", techniques: [] }),
      false,
    );
  });

  it("returns true when any technique is in the high-blast-radius list", () => {
    assert.equal(
      requiresSafetyPreflight({ mode: "EXPERIMENTAL", techniques: ["lateral_movement"] }),
      true,
    );
  });

  it("returns true when opt-in is set", () => {
    assert.equal(
      requiresSafetyPreflight({ mode: "EXPERIMENTAL", techniques: [], optIn: true }),
      true,
    );
  });

  it("is case-insensitive on technique matching", () => {
    assert.equal(
      requiresSafetyPreflight({ mode: "EXPERIMENTAL", techniques: ["Persistence"] }),
      true,
    );
  });
});

// ---------------------------------------------------------------------------
// parseSafetyPreflightChecklist
// ---------------------------------------------------------------------------

describe("parseSafetyPreflightChecklist", () => {
  const validChecklist = `
# Safety Preflight — APTL-RQ001

## Authorizing party

Jane Doe (Director of Security Research), 2026-04-30.

## Authorization basis

Internal SoW SR-2026-12, dated 2026-04-29.

## Scope window

Start: 2026-05-01
End: 2026-05-15

## In-scope assets

- ASSET-001 (lab segment range)
- ASSET-014 (purple-team SIEM)

## Out-of-scope assets

- ASSET-PROD-* (all production hosts explicitly excluded)

## Blast radius

Worst credible: lateral movement to corporate identity store.
Containment: lab range is air-gapped via SEG-LAB-1, dedicated
identities, snapshot rollback verified.

## Data handling

Captured artifacts retained for 90 days, redaction rules per SR-2026-12.

## Abort conditions

- Detection of lateral movement out of SEG-LAB-1
- Loss of console access to lab range
- Authorizing party revokes scope

## Sign-off

Signed-off-by: Jane Doe, 2026-04-30
`;

  it("parses a complete checklist as ok", () => {
    const result = parseSafetyPreflightChecklist(validChecklist);
    assert.equal(result.ok, true);
    assert.deepEqual(result.missing, []);
    assert.ok(result.sections.authorizing_party.includes("Jane Doe"));
    assert.ok(result.sections.in_scope.includes("ASSET-001"));
    assert.ok(result.sections.sign_off.toLowerCase().includes("signed-off-by"));
  });

  it("reports every missing required section", () => {
    const result = parseSafetyPreflightChecklist("# Safety Preflight\n\nNothing here.\n");
    assert.equal(result.ok, false);
    for (const required of [
      "authorizing_party",
      "authorization_basis",
      "scope_window",
      "in_scope",
      "out_of_scope",
      "blast_radius",
      "data_handling",
      "abort_conditions",
      "sign_off",
    ]) {
      assert.ok(result.missing.includes(required), `expected missing: ${required}`);
    }
  });

  it("flags a sign-off section that lacks a signed-off-by line", () => {
    const text = validChecklist.replace("Signed-off-by: Jane Doe, 2026-04-30", "(pending)");
    const result = parseSafetyPreflightChecklist(text);
    assert.equal(result.ok, false);
    assert.ok(result.errors.some((e) => e.toLowerCase().includes("sign-off")));
  });

  it("flags vague authorizing party text", () => {
    const text = validChecklist.replace(
      "Jane Doe (Director of Security Research), 2026-04-30.",
      "Approved by management.",
    );
    const result = parseSafetyPreflightChecklist(text);
    assert.equal(result.ok, false);
    assert.ok(result.errors.some((e) => e.toLowerCase().includes("authoriz")));
  });

  it("rejects non-string input", () => {
    const result = parseSafetyPreflightChecklist(null);
    assert.equal(result.ok, false);
    assert.ok(result.errors[0].toLowerCase().includes("string"));
  });
});

// ---------------------------------------------------------------------------
// Research prompt builders
// ---------------------------------------------------------------------------

describe("buildResearchCharterPrompt", () => {
  it("includes the question, mode, project, and demands explicit gaps to be raised", () => {
    const prompt = buildResearchCharterPrompt({
      researchQuestion: { uid: "APTL-RQ001", statement: "Can EDR-X detect technique T1059 in this lab?" },
      project: "aptl",
      mode: "ADVERSARIAL_LAB",
      priorContext: "Operator has run two pilot runs without formal charter.",
    });
    assert.ok(prompt.includes("APTL-RQ001"));
    assert.ok(prompt.includes("ADVERSARIAL_LAB"));
    assert.ok(prompt.includes("aptl"));
    assert.ok(prompt.toLowerCase().includes("authorization"));
    assert.ok(prompt.toLowerCase().includes("threats to validity"));
    assert.ok(prompt.toLowerCase().includes("stop"));
    assert.ok(prompt.toLowerCase().includes("do not fabricate"));
  });
});

describe("buildResearchLitReviewPrompt", () => {
  it("requests a fresh survey when no existing review is supplied", () => {
    const prompt = buildResearchLitReviewPrompt({
      researchQuestion: { uid: "GC-RQ002", statement: "What is the state of LLM red-team evals?" },
      mode: "LITERATURE",
      existingLitReviewPath: null,
    });
    assert.ok(prompt.includes("GC-RQ002"));
    assert.ok(prompt.toLowerCase().includes("inclusion"));
    assert.ok(prompt.toLowerCase().includes("gaps"));
    assert.ok(!prompt.toLowerCase().includes("existing lit review file"));
  });

  it("delegates to importing/summarizing when an existing review path is given", () => {
    const prompt = buildResearchLitReviewPrompt({
      researchQuestion: { uid: "GC-RQ002", statement: "What is the state of LLM red-team evals?" },
      mode: "LITERATURE",
      existingLitReviewPath: "/abs/path/to/existing.md",
    });
    assert.ok(prompt.includes("/abs/path/to/existing.md"));
    assert.ok(prompt.toLowerCase().includes("do not re-run"));
    assert.ok(prompt.toLowerCase().includes("gaps"));
  });
});

describe("buildResearchMethodologyPreflightPrompt", () => {
  it("demands reuse of existing instruments and explicit reproducibility constraints", () => {
    const prompt = buildResearchMethodologyPreflightPrompt({
      researchQuestion: { uid: "APTL-RQ001", statement: "Q?" },
      charterDoc: "Charter: Q?\nSuccess: detection rate >= 0.9.",
      litReviewSummary: "Three prior surveys cover the area.",
      mode: "ADVERSARIAL_LAB",
    });
    assert.ok(prompt.includes("APTL-RQ001"));
    assert.ok(prompt.toLowerCase().includes("reuse"));
    assert.ok(prompt.toLowerCase().includes("reproducib"));
    assert.ok(prompt.toLowerCase().includes("alternatives considered"));
  });
});

describe("buildResearchSafetyPreflightPrompt", () => {
  it("rejects vague authorization and demands named roles, named assets, dated scope", () => {
    const prompt = buildResearchSafetyPreflightPrompt({
      researchQuestion: { uid: "APTL-RQ001", statement: "Q?" },
      protocolDoc: "Step 1: run T1059. Step 2: collect SIEM events.",
      mode: "ADVERSARIAL_LAB",
      highBlastRadiusTechniques: HIGH_BLAST_RADIUS_TECHNIQUES,
    });
    assert.ok(prompt.toLowerCase().includes("named role"));
    assert.ok(prompt.toLowerCase().includes("named asset"));
    assert.ok(prompt.toLowerCase().includes("dated"));
    assert.ok(prompt.toLowerCase().includes("vague"));
    assert.ok(prompt.toLowerCase().includes("do not"));
    // The prompt must enumerate the high-blast-radius list verbatim
    for (const technique of HIGH_BLAST_RADIUS_TECHNIQUES) {
      assert.ok(prompt.includes(technique), `missing technique in prompt: ${technique}`);
    }
  });
});

describe("buildResearchSynthesisReviewPrompt", () => {
  it("demands an exhaustive non-triaged peer review of methodology and synthesis", () => {
    const prompt = buildResearchSynthesisReviewPrompt({
      researchQuestion: { uid: "APTL-RQ001", statement: "Q?" },
      charterSummary: "...",
      methodologyAdrUid: "ADR-077",
      synthesisSummary: "We found X.",
    });
    assert.ok(prompt.includes("APTL-RQ001"));
    assert.ok(prompt.includes("ADR-077"));
    assert.ok(prompt.toLowerCase().includes("threats to validity"));
    assert.ok(prompt.toLowerCase().includes("enumerate all"));
    assert.ok(prompt.toLowerCase().includes("do not"));
    assert.ok(prompt.toLowerCase().includes("unsupported claims"));
  });
});

describe("buildResearchExecArgs", () => {
  it("matches the codex exec invocation shape used elsewhere", () => {
    const args = buildResearchExecArgs({
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
