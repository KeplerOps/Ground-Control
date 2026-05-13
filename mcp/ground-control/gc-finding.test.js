// Adapter-level tests for the gc_finding MCP handler. Exercise the full path
// Zod-parsed args → handler dispatch → backend HTTP call (mocked fetch) →
// wire body shape. Locks in the snake_case → camelCase mapping for finding
// fields (GC-V001) so the bug shape from issue #875 — adapter accepts a
// snake_case key but TO_CAMEL has no mapping, so backend Bean Validation
// rejects the request — cannot regress for the finding tool.

import { describe, it, beforeEach, afterEach } from "node:test";
import assert from "node:assert/strict";
import { z } from "zod";
import {
  GC_FINDING_ACTIONS,
  GC_FINDING_CREATE_BODY_FIELDS,
  GC_FINDING_UPDATE_BODY_FIELDS,
  GC_FINDING_CREATE_REQUIRED_FIELDS,
  gcFindingZodShape,
  gcFindingToolHandler,
} from "./gc-finding.js";

const ORIGINAL_FETCH = globalThis.fetch;
const ORIGINAL_BASE_URL = process.env.GC_BASE_URL;
const ORIGINAL_API_TOKEN = process.env.GROUND_CONTROL_API_TOKEN;

function makeFetchSpy({ status = 200, body = { id: "f-uuid" } } = {}) {
  const calls = [];
  globalThis.fetch = async (url, opts) => {
    const parsedBody = opts && opts.body ? JSON.parse(opts.body) : null;
    calls.push({ url: url.toString(), method: opts?.method ?? "GET", body: parsedBody });
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

describe("gcFindingZodShape", () => {
  const schema = z.object(gcFindingZodShape);

  it("preserves every backend create body field through Zod parse", () => {
    const parsed = schema.parse({
      action: "create",
      uid: "FIND-1",
      title: "Finding",
      finding_type: "AUDIT_FINDING",
      severity: "HIGH",
      description: "desc",
      root_cause_analysis: "cause",
      owner: "alice",
      due_date: "2026-06-30",
    });
    for (const k of GC_FINDING_CREATE_BODY_FIELDS) {
      assert.ok(k in parsed, `Zod stripped '${k}' — the create body would be missing it on the wire`);
    }
  });

  it("preserves the update-only clear_* flags through Zod parse", () => {
    const parsed = schema.parse({
      action: "update",
      id: "11111111-1111-1111-1111-111111111111",
      clear_root_cause_analysis: true,
      clear_owner: false,
      clear_due_date: true,
    });
    assert.equal(parsed.clear_root_cause_analysis, true);
    assert.equal(parsed.clear_owner, false);
    assert.equal(parsed.clear_due_date, true);
  });

  it("rejects an unknown action enum value", () => {
    assert.throws(() => schema.parse({ action: "merge" }));
  });

  it("rejects an unknown finding_type enum value", () => {
    assert.throws(() => schema.parse({ action: "create", finding_type: "NOT_A_TYPE" }));
  });
});

describe("GC_FINDING_CREATE_BODY_FIELDS", () => {
  it("contains every backend @NotBlank / @NotNull create field", () => {
    for (const required of ["uid", "title", "finding_type", "severity", "description"]) {
      assert.ok(GC_FINDING_CREATE_BODY_FIELDS.includes(required));
    }
  });

  it("does NOT contain update-only clear_* flags", () => {
    assert.ok(!GC_FINDING_CREATE_BODY_FIELDS.includes("clear_root_cause_analysis"));
    assert.ok(!GC_FINDING_CREATE_BODY_FIELDS.includes("clear_owner"));
    assert.ok(!GC_FINDING_CREATE_BODY_FIELDS.includes("clear_due_date"));
  });
});

describe("GC_FINDING_UPDATE_BODY_FIELDS", () => {
  it("contains every backend Update DTO field", () => {
    for (const f of [
      "title", "finding_type", "severity", "description", "root_cause_analysis",
      "owner", "due_date",
      "clear_root_cause_analysis", "clear_owner", "clear_due_date",
    ]) {
      assert.ok(GC_FINDING_UPDATE_BODY_FIELDS.includes(f));
    }
  });

  it("does NOT contain create-only 'uid'", () => {
    assert.ok(!GC_FINDING_UPDATE_BODY_FIELDS.includes("uid"));
  });
});

describe("GC_FINDING_ACTIONS", () => {
  it("matches the action verbs the handler dispatches", () => {
    assert.deepEqual(
      [...GC_FINDING_ACTIONS].sort(),
      ["create", "delete", "link_create", "link_delete", "transition", "update"],
    );
  });
});

describe("gcFindingToolHandler create", () => {
  it("sends every required field to /api/v1/findings as camelCase", async () => {
    const calls = makeFetchSpy();
    await gcFindingToolHandler({
      action: "create",
      uid: "FIND-1",
      title: "Title",
      finding_type: "CONTROL_DEFICIENCY",
      severity: "HIGH",
      description: "desc",
      root_cause_analysis: "cause",
      owner: "alice",
      due_date: "2026-06-30",
      project: "proj-a",
    });
    assert.equal(calls.length, 1);
    assert.equal(calls[0].method, "POST");
    assert.match(calls[0].url, /\/api\/v1\/findings\b/);
    assert.match(calls[0].url, /project=proj-a/);
    assert.deepEqual(calls[0].body, {
      uid: "FIND-1",
      title: "Title",
      findingType: "CONTROL_DEFICIENCY",
      severity: "HIGH",
      description: "desc",
      rootCauseAnalysis: "cause",
      owner: "alice",
      dueDate: "2026-06-30",
    });
  });

  it("rejects when a required field is missing", async () => {
    makeFetchSpy();
    await assert.rejects(
      () =>
        gcFindingToolHandler({
          action: "create",
          uid: "FIND-1",
          // title intentionally missing
          finding_type: "AUDIT_FINDING",
          severity: "LOW",
          description: "x",
        }),
      /title/i,
    );
    for (const required of GC_FINDING_CREATE_REQUIRED_FIELDS) {
      await assert.rejects(
        () =>
          gcFindingToolHandler({
            action: "create",
            uid: required === "uid" ? undefined : "FIND-X",
            title: required === "title" ? undefined : "T",
            finding_type: required === "finding_type" ? undefined : "AUDIT_FINDING",
            severity: required === "severity" ? undefined : "LOW",
            description: required === "description" ? undefined : "x",
          }),
        new RegExp(required, "i"),
      );
    }
  });
});

describe("gcFindingToolHandler update", () => {
  it("sends clear_* flags as camelCase to PUT /api/v1/findings/{id}", async () => {
    const calls = makeFetchSpy();
    await gcFindingToolHandler({
      action: "update",
      id: "11111111-1111-1111-1111-111111111111",
      title: "Updated",
      clear_root_cause_analysis: true,
      clear_owner: false,
      clear_due_date: true,
    });
    assert.equal(calls.length, 1);
    assert.equal(calls[0].method, "PUT");
    assert.match(calls[0].url, /\/api\/v1\/findings\/11111111/);
    assert.deepEqual(calls[0].body, {
      title: "Updated",
      clearRootCauseAnalysis: true,
      clearOwner: false,
      clearDueDate: true,
    });
  });

  it("does NOT forward create-only fields", async () => {
    const calls = makeFetchSpy();
    await gcFindingToolHandler({
      action: "update",
      id: "11111111-1111-1111-1111-111111111111",
      uid: "should-be-stripped",
      title: "T",
    });
    assert.deepEqual(calls[0].body, { title: "T" });
  });
});

describe("gcFindingToolHandler transition", () => {
  it("sends camelCase status to PUT /api/v1/findings/{id}/status", async () => {
    const calls = makeFetchSpy();
    await gcFindingToolHandler({
      action: "transition",
      id: "22222222-2222-2222-2222-222222222222",
      status: "REMEDIATION_IN_PROGRESS",
    });
    assert.equal(calls.length, 1);
    assert.equal(calls[0].method, "PUT");
    assert.match(calls[0].url, /\/api\/v1\/findings\/22222222.*\/status/);
    assert.deepEqual(calls[0].body, { status: "REMEDIATION_IN_PROGRESS" });
  });
});

describe("gcFindingToolHandler link_create", () => {
  it("sends internal-target link as camelCase to /api/v1/findings/{findingId}/links", async () => {
    const calls = makeFetchSpy({ body: { id: "link-uuid" } });
    await gcFindingToolHandler({
      action: "link_create",
      finding_id: "33333333-3333-3333-3333-333333333333",
      target_type: "CONTROL",
      target_entity_id: "44444444-4444-4444-4444-444444444444",
      link_type: "MITIGATED_BY",
      target_title: "Access policy",
    });
    assert.equal(calls.length, 1);
    assert.equal(calls[0].method, "POST");
    assert.match(calls[0].url, /\/api\/v1\/findings\/33333333.*\/links/);
    assert.deepEqual(calls[0].body, {
      targetType: "CONTROL",
      targetEntityId: "44444444-4444-4444-4444-444444444444",
      linkType: "MITIGATED_BY",
      targetTitle: "Access policy",
    });
  });
});

describe("gcFindingToolHandler link_delete", () => {
  it("calls DELETE /api/v1/findings/{findingId}/links/{linkId}", async () => {
    const calls = makeFetchSpy({ status: 200, body: {} });
    await gcFindingToolHandler({
      action: "link_delete",
      finding_id: "33333333-3333-3333-3333-333333333333",
      link_id: "55555555-5555-5555-5555-555555555555",
    });
    assert.equal(calls.length, 1);
    assert.equal(calls[0].method, "DELETE");
    assert.match(
      calls[0].url,
      /\/api\/v1\/findings\/33333333.*\/links\/55555555/,
    );
  });
});
