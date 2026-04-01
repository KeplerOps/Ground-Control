/**
 * Contract tests that verify frontend enum values exactly match the backend/MCP
 * single source of truth.  Any drift between the REQUIREMENT_TYPES,
 * ARTIFACT_TYPES, or LINK_TYPES constants in api.ts and the backend Java enums
 * will cause this test suite to fail in CI.
 *
 * Backend sources:
 *   - RequirementType: domain/requirements/state/RequirementType.java
 *   - ArtifactType:    domain/requirements/state/ArtifactType.java
 *   - LinkType:        domain/requirements/state/LinkType.java
 * MCP source:
 *   - mcp/ground-control/lib.js
 */

import { describe, expect, it } from "vitest";
import { ARTIFACT_TYPES, LINK_TYPES, REQUIREMENT_TYPES } from "./api";

function sorted(values: readonly string[]): string[] {
  return [...values].sort();
}

describe("Enum contract: RequirementType", () => {
  it("contains exactly the backend-supported values", () => {
    expect(sorted(REQUIREMENT_TYPES)).toEqual(
      sorted(["FUNCTIONAL", "NON_FUNCTIONAL", "CONSTRAINT", "INTERFACE"]),
    );
  });

  it("does not contain removed/unsupported values", () => {
    const removed = ["PERFORMANCE", "SECURITY", "DATA"];
    for (const v of removed) {
      expect(REQUIREMENT_TYPES).not.toContain(v);
    }
  });
});

describe("Enum contract: ArtifactType", () => {
  it("contains exactly the backend-supported values", () => {
    expect(sorted(ARTIFACT_TYPES)).toEqual(
      sorted([
        "GITHUB_ISSUE",
        "CODE_FILE",
        "ADR",
        "CONFIG",
        "POLICY",
        "TEST",
        "SPEC",
        "PROOF",
        "DOCUMENTATION",
      ]),
    );
  });

  it("does not contain removed/unsupported values", () => {
    const removed = [
      "GITHUB_PR",
      "JIRA_ISSUE",
      "CONFLUENCE_PAGE",
      "TEST_CASE",
      "DESIGN_DOC",
      "OTHER",
    ];
    for (const v of removed) {
      expect(ARTIFACT_TYPES).not.toContain(v);
    }
  });
});

describe("Enum contract: LinkType", () => {
  it("contains exactly the backend-supported values", () => {
    expect(sorted(LINK_TYPES)).toEqual(
      sorted(["IMPLEMENTS", "TESTS", "DOCUMENTS", "CONSTRAINS", "VERIFIES"]),
    );
  });

  it("does not contain removed/unsupported values", () => {
    const removed = ["TRACES_TO", "DERIVED_FROM"];
    for (const v of removed) {
      expect(LINK_TYPES).not.toContain(v);
    }
  });
});
