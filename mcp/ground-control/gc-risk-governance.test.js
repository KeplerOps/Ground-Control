// Adapter-level tests for the gc_risk_governance MCP handler. Covers per-entity
// create/update body allowlists and the camelCased wire body produced by the
// shared toCamelCase map. Locks in the fixes for issues #878 (risk assessment
// result), #879 (risk register record), and #880 (treatment plan) against the
// authoritative backend Request records under
// backend/src/main/java/com/keplerops/groundcontrol/api/riskscenarios/.

import { describe, it, beforeEach, afterEach } from "node:test";
import assert from "node:assert/strict";
import {
  GOVERNANCE_FIELDS,
  pick,
  createRiskAssessmentResult,
  updateRiskAssessmentResult,
  createRiskRegisterRecord,
  updateRiskRegisterRecord,
  createTreatmentPlan,
  updateTreatmentPlan,
} from "./lib.js";

const ORIGINAL_FETCH = globalThis.fetch;
const ORIGINAL_BASE_URL = process.env.GC_BASE_URL;
const ORIGINAL_API_TOKEN = process.env.GROUND_CONTROL_API_TOKEN;

function makeFetchSpy({ status = 200, body = { id: "ent-uuid" } } = {}) {
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

// ---------------------------------------------------------------------------
// Shape: GOVERNANCE_FIELDS[entity][action]
// ---------------------------------------------------------------------------

describe("GOVERNANCE_FIELDS per-action shape", () => {
  it("indexes by entity then action", () => {
    for (const entity of ["risk_register_record", "risk_assessment_result", "treatment_plan"]) {
      assert.ok(GOVERNANCE_FIELDS[entity], `missing entity '${entity}'`);
      assert.ok(Array.isArray(GOVERNANCE_FIELDS[entity].create), `${entity}.create not an array`);
      assert.ok(Array.isArray(GOVERNANCE_FIELDS[entity].update), `${entity}.update not an array`);
    }
  });
});

// ---------------------------------------------------------------------------
// risk_assessment_result allowlist (#878)
// ---------------------------------------------------------------------------

describe("risk_assessment_result create allowlist (#878)", () => {
  const list = () => GOVERNANCE_FIELDS.risk_assessment_result.create;

  it("contains every backend RiskAssessmentResultRequest field", () => {
    for (const f of [
      "risk_scenario_id", "risk_register_record_id", "methodology_profile_id",
      "analyst_identity", "assumptions", "input_factors",
      "observation_date", "assessment_at", "time_horizon", "confidence",
      "uncertainty_metadata", "computed_outputs",
      "evidence_refs", "notes", "observation_ids",
    ]) {
      assert.ok(list().includes(f), `${f} missing from risk_assessment_result.create`);
    }
  });

  it("does NOT contain stale fields that the backend DTO does not accept", () => {
    for (const f of [
      "uid", "title", "description",
      "quantitative_value", "qualitative_value",
      "scenario_id", "approval_state", "metadata", "status",
    ]) {
      assert.ok(!list().includes(f), `${f} should not be on risk_assessment_result.create`);
    }
  });
});

describe("risk_assessment_result update allowlist (#878)", () => {
  const list = () => GOVERNANCE_FIELDS.risk_assessment_result.update;

  it("contains every UpdateRiskAssessmentResultRequest field", () => {
    for (const f of [
      "risk_register_record_id", "methodology_profile_id",
      "analyst_identity", "assumptions", "input_factors",
      "observation_date", "assessment_at", "time_horizon", "confidence",
      "uncertainty_metadata", "computed_outputs",
      "evidence_refs", "notes", "observation_ids",
    ]) {
      assert.ok(list().includes(f), `${f} missing from risk_assessment_result.update`);
    }
  });

  it("does NOT contain create-only 'risk_scenario_id' (scenario is immutable after create)", () => {
    assert.ok(!list().includes("risk_scenario_id"));
  });

  it("does NOT contain stale fields", () => {
    for (const f of ["uid", "title", "description", "quantitative_value", "qualitative_value", "scenario_id", "approval_state", "metadata", "status"]) {
      assert.ok(!list().includes(f), `${f} should not be on risk_assessment_result.update`);
    }
  });
});

describe("risk_assessment_result wire body (#878)", () => {
  const SCENARIO = "11111111-1111-1111-1111-111111111111";
  const REGISTER = "22222222-2222-2222-2222-222222222222";
  const PROFILE = "33333333-3333-3333-3333-333333333333";

  it("create produces a camelCased body with every backend create field", async () => {
    const calls = makeFetchSpy();
    const args = {
      risk_scenario_id: SCENARIO,
      risk_register_record_id: REGISTER,
      methodology_profile_id: PROFILE,
      analyst_identity: "agent-a",
      assumptions: "stable inputs",
      input_factors: { ale: 100 },
      observation_date: "2026-01-15T00:00:00Z",
      assessment_at: "2026-01-16T00:00:00Z",
      time_horizon: "12 months",
      confidence: "HIGH",
      uncertainty_metadata: { variance: 0.05 },
      computed_outputs: { ale: 485000 },
      evidence_refs: ["EV-1", "EV-2"],
      notes: "Phase A smoke",
      observation_ids: ["44444444-4444-4444-4444-444444444444"],
    };
    const body = pick(args, GOVERNANCE_FIELDS.risk_assessment_result.create);
    await createRiskAssessmentResult(body, "proj-a");
    assert.equal(calls.length, 1);
    assert.equal(calls[0].method, "POST");
    assert.match(calls[0].url, /\/api\/v1\/risk-assessment-results\b/);
    assert.match(calls[0].url, /project=proj-a/);
    assert.deepEqual(calls[0].body, {
      riskScenarioId: SCENARIO,
      riskRegisterRecordId: REGISTER,
      methodologyProfileId: PROFILE,
      analystIdentity: "agent-a",
      assumptions: "stable inputs",
      inputFactors: { ale: 100 },
      observationDate: "2026-01-15T00:00:00Z",
      assessmentAt: "2026-01-16T00:00:00Z",
      timeHorizon: "12 months",
      confidence: "HIGH",
      uncertaintyMetadata: { variance: 0.05 },
      computedOutputs: { ale: 485000 },
      evidenceRefs: ["EV-1", "EV-2"],
      notes: "Phase A smoke",
      observationIds: ["44444444-4444-4444-4444-444444444444"],
    });
  });

  it("create drops stale caller fields before reaching the wire", async () => {
    const calls = makeFetchSpy();
    const args = {
      risk_scenario_id: SCENARIO,
      methodology_profile_id: PROFILE,
      // stale fields the bug report shipped with:
      uid: "RAR-1",
      title: "drop me",
      description: "drop me",
      quantitative_value: 485000,
      qualitative_value: "HIGH",
      scenario_id: SCENARIO, // legacy alias, must NOT reach the wire
      approval_state: "DRAFT",
      metadata: { stale: true },
      status: "DRAFT",
    };
    const body = pick(args, GOVERNANCE_FIELDS.risk_assessment_result.create);
    await createRiskAssessmentResult(body, "proj-a");
    assert.equal(calls.length, 1);
    for (const stale of [
      "uid", "title", "description", "quantitativeValue", "qualitativeValue",
      "scenarioId", "approvalState", "metadata", "status",
    ]) {
      assert.ok(!(stale in calls[0].body), `${stale} leaked onto the wire`);
    }
  });

  it("update strips create-only 'risk_scenario_id' even if the caller passes it", async () => {
    const calls = makeFetchSpy();
    const args = {
      risk_scenario_id: SCENARIO, // must NOT reach the wire (immutable after create)
      methodology_profile_id: PROFILE,
      analyst_identity: "agent-b",
      notes: "second-pass",
    };
    const body = pick(args, GOVERNANCE_FIELDS.risk_assessment_result.update);
    await updateRiskAssessmentResult("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", body, "proj-a");
    assert.equal(calls.length, 1);
    assert.equal(calls[0].method, "PUT");
    assert.ok(!("riskScenarioId" in calls[0].body));
    assert.equal(calls[0].body.methodologyProfileId, PROFILE);
    assert.equal(calls[0].body.analystIdentity, "agent-b");
    assert.equal(calls[0].body.notes, "second-pass");
  });
});

// ---------------------------------------------------------------------------
// risk_register_record allowlist (#879)
// ---------------------------------------------------------------------------

describe("risk_register_record create allowlist (#879)", () => {
  const list = () => GOVERNANCE_FIELDS.risk_register_record.create;

  it("contains every backend RiskRegisterRecordRequest field", () => {
    for (const f of [
      "uid", "title", "owner", "review_cadence", "next_review_at",
      "category_tags", "decision_metadata", "asset_scope_summary",
      "risk_scenario_ids",
    ]) {
      assert.ok(list().includes(f), `${f} missing from risk_register_record.create`);
    }
  });

  it("does NOT contain singular 'scenario_id' (backend is List<UUID> riskScenarioIds)", () => {
    assert.ok(!list().includes("scenario_id"));
  });

  it("does NOT contain 'description' or 'status' (not on the Create DTO; status is transition-only)", () => {
    assert.ok(!list().includes("description"));
    assert.ok(!list().includes("status"));
  });

  it("does NOT contain 'metadata' (typed fields must not tunnel through metadata)", () => {
    assert.ok(!list().includes("metadata"));
  });
});

describe("risk_register_record update allowlist (#879)", () => {
  const list = () => GOVERNANCE_FIELDS.risk_register_record.update;

  it("contains every UpdateRiskRegisterRecordRequest field", () => {
    for (const f of [
      "title", "owner", "review_cadence", "next_review_at",
      "category_tags", "decision_metadata", "asset_scope_summary",
      "risk_scenario_ids",
    ]) {
      assert.ok(list().includes(f), `${f} missing from risk_register_record.update`);
    }
  });

  it("does NOT contain create-only 'uid'", () => {
    assert.ok(!list().includes("uid"));
  });

  it("does NOT contain stale 'scenario_id', 'description', 'status', 'metadata'", () => {
    for (const f of ["scenario_id", "description", "status", "metadata"]) {
      assert.ok(!list().includes(f), `${f} should not be on risk_register_record.update`);
    }
  });
});

describe("risk_register_record wire body (#879)", () => {
  const SC1 = "55555555-5555-5555-5555-555555555555";
  const SC2 = "66666666-6666-6666-6666-666666666666";

  it("create routes multi-scenario via risk_scenario_ids as List<UUID>", async () => {
    const calls = makeFetchSpy();
    const args = {
      uid: "REG-1",
      title: "Portfolio record",
      owner: "team-x",
      review_cadence: "quarterly",
      next_review_at: "2026-04-01T00:00:00Z",
      category_tags: ["regulatory", "supplier"],
      decision_metadata: { rationale: "merged after retro" },
      asset_scope_summary: "covers PCI-DSS assets in prod",
      risk_scenario_ids: [SC1, SC2],
    };
    const body = pick(args, GOVERNANCE_FIELDS.risk_register_record.create);
    await createRiskRegisterRecord(body, "proj-a");
    assert.equal(calls.length, 1);
    assert.match(calls[0].url, /\/api\/v1\/risk-register-records\b/);
    assert.deepEqual(calls[0].body, {
      uid: "REG-1",
      title: "Portfolio record",
      owner: "team-x",
      reviewCadence: "quarterly",
      nextReviewAt: "2026-04-01T00:00:00Z",
      categoryTags: ["regulatory", "supplier"],
      decisionMetadata: { rationale: "merged after retro" },
      assetScopeSummary: "covers PCI-DSS assets in prod",
      riskScenarioIds: [SC1, SC2],
    });
  });

  it("create drops singular 'scenario_id', 'description', 'status', 'metadata' before reaching the wire", async () => {
    const calls = makeFetchSpy();
    const args = {
      uid: "REG-2",
      title: "T",
      scenario_id: SC1, // singular, must NOT reach the wire
      description: "drop me",
      status: "ACCEPTED",
      metadata: { stale: true },
    };
    const body = pick(args, GOVERNANCE_FIELDS.risk_register_record.create);
    await createRiskRegisterRecord(body, "proj-a");
    assert.equal(calls.length, 1);
    for (const stale of ["scenarioId", "description", "status", "metadata"]) {
      assert.ok(!(stale in calls[0].body), `${stale} leaked onto the wire`);
    }
  });

  it("update strips create-only 'uid' even if the caller passes it", async () => {
    const calls = makeFetchSpy();
    const args = {
      uid: "REG-LEAK",
      title: "rename",
      risk_scenario_ids: [SC1],
    };
    const body = pick(args, GOVERNANCE_FIELDS.risk_register_record.update);
    await updateRiskRegisterRecord("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", body, "proj-a");
    assert.equal(calls.length, 1);
    assert.equal(calls[0].method, "PUT");
    assert.ok(!("uid" in calls[0].body));
    assert.equal(calls[0].body.title, "rename");
    assert.deepEqual(calls[0].body.riskScenarioIds, [SC1]);
  });
});

// ---------------------------------------------------------------------------
// treatment_plan allowlist (#880)
// ---------------------------------------------------------------------------

describe("treatment_plan create allowlist (#880)", () => {
  const list = () => GOVERNANCE_FIELDS.treatment_plan.create;

  it("contains every backend TreatmentPlanRequest field", () => {
    for (const f of [
      "uid", "title", "risk_scenario_id", "risk_register_record_id",
      "strategy", "owner", "rationale", "due_date", "status",
      "action_items", "reassessment_triggers",
    ]) {
      assert.ok(list().includes(f), `${f} missing from treatment_plan.create`);
    }
  });

  it("does NOT contain 'description' (backend has 'rationale', not 'description')", () => {
    assert.ok(!list().includes("description"));
  });

  it("does NOT contain misnamed 'due_at' (backend field is 'dueDate' / 'due_date')", () => {
    assert.ok(!list().includes("due_at"));
  });

  it("does NOT contain singular 'scenario_id' (backend field is 'riskScenarioId' / 'risk_scenario_id')", () => {
    assert.ok(!list().includes("scenario_id"));
  });

  it("does NOT contain 'metadata' (typed fields must not tunnel through metadata)", () => {
    assert.ok(!list().includes("metadata"));
  });
});

describe("treatment_plan update allowlist (#880)", () => {
  const list = () => GOVERNANCE_FIELDS.treatment_plan.update;

  it("contains every UpdateTreatmentPlanRequest field", () => {
    for (const f of [
      "title", "risk_scenario_id", "strategy", "owner",
      "rationale", "due_date", "action_items", "reassessment_triggers",
    ]) {
      assert.ok(list().includes(f), `${f} missing from treatment_plan.update`);
    }
  });

  it("does NOT contain create-only 'uid', 'risk_register_record_id', 'status'", () => {
    // Status changes go through the transition action — the dedicated /status sub-resource.
    for (const f of ["uid", "risk_register_record_id", "status"]) {
      assert.ok(!list().includes(f), `${f} should not be on treatment_plan.update`);
    }
  });

  it("does NOT contain stale 'scenario_id', 'due_at', 'description', 'metadata'", () => {
    for (const f of ["scenario_id", "due_at", "description", "metadata"]) {
      assert.ok(!list().includes(f), `${f} should not be on treatment_plan.update`);
    }
  });
});

describe("treatment_plan wire body (#880)", () => {
  const SCEN = "77777777-7777-7777-7777-777777777777";
  const REG = "88888888-8888-8888-8888-888888888888";

  it("create produces a camelCased body with every backend create field", async () => {
    const calls = makeFetchSpy();
    const args = {
      uid: "TP-1",
      title: "Mitigate scenario X",
      risk_scenario_id: SCEN,
      risk_register_record_id: REG,
      strategy: "MITIGATE",
      owner: "team-y",
      rationale: "Cost-benefit favors mitigation",
      due_date: "2026-06-30T00:00:00Z",
      status: "PLANNED",
      action_items: [{ what: "patch", who: "team-y" }],
      reassessment_triggers: ["new evidence", "control change"],
    };
    const body = pick(args, GOVERNANCE_FIELDS.treatment_plan.create);
    await createTreatmentPlan(body, "proj-a");
    assert.equal(calls.length, 1);
    assert.match(calls[0].url, /\/api\/v1\/treatment-plans\b/);
    assert.deepEqual(calls[0].body, {
      uid: "TP-1",
      title: "Mitigate scenario X",
      riskScenarioId: SCEN,
      riskRegisterRecordId: REG,
      strategy: "MITIGATE",
      owner: "team-y",
      rationale: "Cost-benefit favors mitigation",
      dueDate: "2026-06-30T00:00:00Z",
      status: "PLANNED",
      actionItems: [{ what: "patch", who: "team-y" }],
      reassessmentTriggers: ["new evidence", "control change"],
    });
  });

  it("create drops stale 'due_at', 'scenario_id', 'description', 'metadata' before reaching the wire", async () => {
    const calls = makeFetchSpy();
    const args = {
      uid: "TP-2",
      title: "T",
      strategy: "ACCEPT",
      risk_register_record_id: REG,
      // stale fields the bug report shipped with:
      due_at: "2026-06-30T00:00:00Z", // misnamed → must NOT reach the wire as 'dueAt'
      scenario_id: SCEN,
      description: "drop me",
      metadata: { stale: true },
    };
    const body = pick(args, GOVERNANCE_FIELDS.treatment_plan.create);
    await createTreatmentPlan(body, "proj-a");
    assert.equal(calls.length, 1);
    for (const stale of ["dueAt", "scenarioId", "description", "metadata"]) {
      assert.ok(!(stale in calls[0].body), `${stale} leaked onto the wire`);
    }
    // And dueDate is not present either (caller didn't supply due_date)
    assert.ok(!("dueDate" in calls[0].body));
  });

  it("update strips create-only 'uid', 'risk_register_record_id', 'status'", async () => {
    const calls = makeFetchSpy();
    const args = {
      uid: "TP-LEAK",
      risk_register_record_id: REG,
      status: "IN_PROGRESS", // must go through transition action, not update
      title: "rename",
      due_date: "2026-07-01T00:00:00Z",
    };
    const body = pick(args, GOVERNANCE_FIELDS.treatment_plan.update);
    await updateTreatmentPlan("cccccccc-cccc-cccc-cccc-cccccccccccc", body, "proj-a");
    assert.equal(calls.length, 1);
    assert.equal(calls[0].method, "PUT");
    for (const stale of ["uid", "riskRegisterRecordId", "status"]) {
      assert.ok(!(stale in calls[0].body), `${stale} leaked onto the wire on update`);
    }
    assert.equal(calls[0].body.title, "rename");
    assert.equal(calls[0].body.dueDate, "2026-07-01T00:00:00Z");
  });
});
