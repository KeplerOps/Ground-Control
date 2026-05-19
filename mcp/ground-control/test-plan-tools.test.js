// Adapter-level tests for the gc_test_plan MCP surface (TC-006 / ADR-044).
//
// The lib.js wrappers below back the `gc_test_plan` consolidated tool
// registered in index.js (`create | update | delete | transition`) and the
// reads routed through gc_query under the /api/v1/test-plans allowlist.
//
// Each test stubs globalThis.fetch and asserts the outbound HTTP shape
// (method + path + query + camelCase body) plus pass-through of the parsed
// response. Snake-case → camelCase mapping is the gate this file exists for:
// every optional field on TestPlanRequest / UpdateTestPlanRequest must round
// -trip without silent drop-through (the codex pre-push cycle 1 finding).

import { afterEach, beforeEach, describe, it } from "node:test";
import assert from "node:assert/strict";
import {
  RequestError,
  TEST_PLAN_STATUSES,
  createTestPlan,
  deleteTestPlan,
  getTestPlan,
  getTestPlanByUid,
  listTestPlans,
  transitionTestPlanStatus,
  updateTestPlan,
} from "./lib.js";

const BASE_URL = "http://gc-test:8000";
const PLAN_ID = "11111111-1111-1111-1111-111111111111";

let fetchCalls;
let originalFetch;
let originalBaseUrl;

function setNextResponse({ ok = true, status = 200, body = null } = {}) {
  globalThis.fetch = async (url, opts) => {
    fetchCalls.push({ url: typeof url === "string" ? url : url.toString(), opts });
    return {
      ok,
      status,
      text: async () => (body === null ? "" : typeof body === "string" ? body : JSON.stringify(body)),
    };
  };
}

beforeEach(() => {
  fetchCalls = [];
  originalFetch = globalThis.fetch;
  originalBaseUrl = process.env.GC_BASE_URL;
  process.env.GC_BASE_URL = BASE_URL;
});

afterEach(() => {
  globalThis.fetch = originalFetch;
  if (originalBaseUrl === undefined) {
    delete process.env.GC_BASE_URL;
  } else {
    process.env.GC_BASE_URL = originalBaseUrl;
  }
});

describe("TestPlan enum mirror", () => {
  it("exposes the five lifecycle states in order (matches TestPlanStatus.java)", () => {
    assert.deepEqual(
      [...TEST_PLAN_STATUSES],
      ["DRAFT", "ACTIVE", "IN_PROGRESS", "COMPLETED", "ARCHIVED"],
    );
  });
});

describe("createTestPlan (gc_test_plan action=create)", () => {
  it("POSTs /api/v1/test-plans with camelCase body and forwards the project param", async () => {
    setNextResponse({
      body: {
        id: PLAN_ID,
        uid: "TP-001",
        name: "Wave-1 acceptance",
        status: "DRAFT",
        startDate: "2026-06-01",
        endDate: "2026-06-30",
      },
    });

    const result = await createTestPlan(
      {
        uid: "TP-001",
        name: "Wave-1 acceptance",
        description: "scope notes",
        product: "ground-control",
        version: "1.2.0",
        build: "build-42",
        start_date: "2026-06-01",
        end_date: "2026-06-30",
      },
      "ground-control",
    );

    assert.equal(fetchCalls.length, 1);
    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "POST");
    assert.equal(parsed.pathname, "/api/v1/test-plans");
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    // Critical contract — every field must round-trip to Spring's
    // TestPlanRequest. A regression on start_date / end_date snake→camel
    // mapping would silently drop the dates on the backend.
    assert.deepEqual(JSON.parse(opts.body), {
      uid: "TP-001",
      name: "Wave-1 acceptance",
      description: "scope notes",
      product: "ground-control",
      version: "1.2.0",
      build: "build-42",
      startDate: "2026-06-01",
      endDate: "2026-06-30",
    });
    assert.equal(result.id, PLAN_ID);
  });
});

describe("listTestPlans (read routed through gc_query)", () => {
  it("GETs /api/v1/test-plans and forwards the project param", async () => {
    setNextResponse({ body: [{ id: PLAN_ID, uid: "TP-001" }] });

    const result = await listTestPlans("ground-control");

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "GET");
    assert.equal(parsed.pathname, "/api/v1/test-plans");
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    assert.equal(Array.isArray(result), true);
    assert.equal(result[0].uid, "TP-001");
  });
});

describe("getTestPlan / getTestPlanByUid", () => {
  it("GETs /api/v1/test-plans/{id} forwarding the project param and passing the parsed body through", async () => {
    setNextResponse({ body: { id: PLAN_ID, uid: "TP-001" } });

    // Pass-through assertion (test-quality cycle 1): an adapter regression
    // that returned undefined / null for the GET path would still satisfy
    // a method+path+project-only check.
    const result = await getTestPlan(PLAN_ID, "ground-control");

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "GET");
    assert.equal(parsed.pathname, `/api/v1/test-plans/${PLAN_ID}`);
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    assert.equal(result.id, PLAN_ID);
    assert.equal(result.uid, "TP-001");
  });

  it("GETs /api/v1/test-plans/uid/{uid} forwarding the project param and passing the parsed body through", async () => {
    setNextResponse({ body: { id: PLAN_ID, uid: "TP-001" } });

    const result = await getTestPlanByUid("TP-001", "ground-control");

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "GET");
    assert.equal(parsed.pathname, "/api/v1/test-plans/uid/TP-001");
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    assert.equal(result.id, PLAN_ID);
    assert.equal(result.uid, "TP-001");
  });
});

describe("updateTestPlan (gc_test_plan action=update)", () => {
  it("PUTs /api/v1/test-plans/{id} forwarding every clear flag in camelCase", async () => {
    setNextResponse({ body: { id: PLAN_ID, uid: "TP-001" } });

    const result = await updateTestPlan(
      PLAN_ID,
      {
        name: "Renamed",
        description: "updated",
        product: "new-product",
        version: "2.0.0",
        start_date: "2026-07-01",
        end_date: "2026-07-31",
        clear_description: false,
        clear_product: false,
        clear_version: false,
        clear_build: true,
        clear_start_date: false,
        clear_end_date: false,
      },
      "ground-control",
    );

    // Return value passes through so callers can read the updated record
    // without a follow-up GET.
    assert.equal(result.id, PLAN_ID);

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "PUT");
    assert.equal(parsed.pathname, `/api/v1/test-plans/${PLAN_ID}`);
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    // Every clear flag must reach UpdateTestPlanRequest. A missing TO_CAMEL
    // entry would let Spring drop the snake-case field name silently and the
    // wipe would never happen — visible-via-no-effect failure mode the
    // codex cycle-1 lesson exists to catch.
    assert.deepEqual(JSON.parse(opts.body), {
      name: "Renamed",
      description: "updated",
      product: "new-product",
      version: "2.0.0",
      startDate: "2026-07-01",
      endDate: "2026-07-31",
      clearDescription: false,
      clearProduct: false,
      clearVersion: false,
      clearBuild: true,
      clearStartDate: false,
      clearEndDate: false,
    });
  });
});

describe("deleteTestPlan (gc_test_plan action=delete)", () => {
  it("DELETEs /api/v1/test-plans/{id}", async () => {
    setNextResponse({ ok: true, status: 204, body: null });

    await deleteTestPlan(PLAN_ID, "ground-control");

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "DELETE");
    assert.equal(parsed.pathname, `/api/v1/test-plans/${PLAN_ID}`);
    assert.equal(parsed.searchParams.get("project"), "ground-control");
  });
});

describe("transitionTestPlanStatus (gc_test_plan action=transition)", () => {
  it("PUTs /api/v1/test-plans/{id}/status with {status} body", async () => {
    setNextResponse({ body: { id: PLAN_ID, status: "ACTIVE" } });

    await transitionTestPlanStatus(PLAN_ID, "ACTIVE", "ground-control");

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "PUT");
    assert.equal(parsed.pathname, `/api/v1/test-plans/${PLAN_ID}/status`);
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    assert.deepEqual(JSON.parse(opts.body), { status: "ACTIVE" });
  });

  it("surfaces 422 invalid_status_transition from the backend as RequestError", async () => {
    setNextResponse({
      ok: false,
      status: 422,
      body: { error: { code: "INVALID_STATUS_TRANSITION", message: "Cannot transition" } },
    });

    await assert.rejects(
      () => transitionTestPlanStatus(PLAN_ID, "DRAFT", "ground-control"),
      (err) => err instanceof RequestError && err.status === 422,
    );
  });
});
