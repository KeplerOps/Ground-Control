// Tests for the shared link_create surface (issue #875 follow-on).
//
// The four consolidated tools that expose link_create (gc_asset,
// gc_threat_model, gc_risk_scenario, gc_control) share the same backend link
// DTO shape and the same MCP-side body allowlist + required-field rules. This
// module is the single point of repair; regressing target_entity_id forwarding,
// target_url / target_title forwarding, or the target_type / link_type
// preconditions in any consolidated tool fails these tests.

import { describe, it } from "node:test";
import assert from "node:assert/strict";
import {
  LINK_CREATE_BODY_FIELDS,
  LINK_CREATE_REQUIRED_FIELDS,
  linkCreateOptionalSharedZodFields,
  performLinkCreate,
} from "./link-create.js";

describe("LINK_CREATE_BODY_FIELDS", () => {
  it("matches the snake_case shape of every backend link DTO", () => {
    // AssetLinkRequest, ThreatModelLinkRequest, RiskScenarioLinkRequest,
    // ControlLinkRequest all carry the same six fields.
    assert.deepEqual(
      [...LINK_CREATE_BODY_FIELDS].sort(),
      [
        "link_type",
        "target_entity_id",
        "target_identifier",
        "target_title",
        "target_type",
        "target_url",
      ],
    );
  });
});

describe("LINK_CREATE_REQUIRED_FIELDS", () => {
  it("requires the two @NotNull backend fields", () => {
    assert.deepEqual([...LINK_CREATE_REQUIRED_FIELDS].sort(), ["link_type", "target_type"]);
  });
});

describe("linkCreateOptionalSharedZodFields", () => {
  it("covers every link DTO field except the per-tool enums", () => {
    assert.deepEqual(
      Object.keys(linkCreateOptionalSharedZodFields).sort(),
      ["target_entity_id", "target_identifier", "target_title", "target_url"],
    );
  });
});

describe("performLinkCreate", () => {
  function makeCreateFnSpy(returnValue = { id: "link-uuid" }) {
    const calls = [];
    return {
      calls,
      fn: async (parentId, body, project) => {
        calls.push({ parentId, body, project });
        return returnValue;
      },
    };
  }

  it("validates parentIdField, target_type, link_type, then forwards every body field", async () => {
    const spy = makeCreateFnSpy();
    const PARENT = "11111111-1111-1111-1111-111111111111";
    const TARGET = "22222222-2222-2222-2222-222222222222";
    const result = await performLinkCreate(
      {
        action: "link_create",
        scenario_id: PARENT,
        target_type: "REQUIREMENT",
        target_entity_id: TARGET,
        link_type: "AFFECTS",
        target_url: "https://example.test/r",
        target_title: "Example",
        project: "proj-a",
        // Noise that must NOT leak into the body.
        id: "should-not-leak",
        title: "should-not-leak",
        random_extra: "should-not-leak",
      },
      "scenario_id",
      spy.fn,
    );
    assert.deepEqual(result, { id: "link-uuid" });
    assert.equal(spy.calls.length, 1);
    assert.equal(spy.calls[0].parentId, PARENT);
    assert.equal(spy.calls[0].project, "proj-a");
    assert.deepEqual(spy.calls[0].body, {
      target_type: "REQUIREMENT",
      target_entity_id: TARGET,
      link_type: "AFFECTS",
      target_url: "https://example.test/r",
      target_title: "Example",
    });
  });

  it("forwards target_identifier (external-target path) without target_entity_id", async () => {
    const spy = makeCreateFnSpy();
    await performLinkCreate(
      {
        asset_id: "33333333-3333-3333-3333-333333333333",
        target_type: "EXTERNAL",
        target_identifier: "ref://x/y",
        link_type: "ASSOCIATED",
      },
      "asset_id",
      spy.fn,
    );
    assert.deepEqual(spy.calls[0].body, {
      target_type: "EXTERNAL",
      target_identifier: "ref://x/y",
      link_type: "ASSOCIATED",
    });
  });

  for (const required of ["target_type", "link_type"]) {
    it(`rejects when '${required}' is missing before any backend call`, async () => {
      const spy = makeCreateFnSpy();
      const base = {
        threat_model_id: "44444444-4444-4444-4444-444444444444",
        target_type: "REQUIREMENT",
        target_entity_id: "55555555-5555-5555-5555-555555555555",
        link_type: "AFFECTS",
      };
      delete base[required];
      await assert.rejects(
        () => performLinkCreate(base, "threat_model_id", spy.fn),
        (e) => e.message.includes(`'${required}' is required`),
      );
      assert.equal(spy.calls.length, 0);
    });
  }

  it("rejects when the parentIdField is missing before any backend call", async () => {
    const spy = makeCreateFnSpy();
    await assert.rejects(
      () => performLinkCreate(
        {
          target_type: "REQUIREMENT",
          link_type: "AFFECTS",
          target_entity_id: "66666666-6666-6666-6666-666666666666",
        },
        "control_id",
        spy.fn,
      ),
      (e) => e.message.includes("'control_id' is required"),
    );
    assert.equal(spy.calls.length, 0);
  });
});
