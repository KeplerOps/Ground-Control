// Adapter-level tests for the gc_test_run MCP surface (TC-008 / ADR-049).
//
// The lib.js wrappers below back the `gc_test_run` consolidated tool registered
// in index.js (`create | update | delete | transition | add_tester |
// remove_tester | update_result`) and the reads routed through gc_query under
// the /api/v1/test-runs allowlist.
//
// Each test stubs globalThis.fetch and asserts the outbound HTTP shape
// (method + path + query + camelCase body) plus pass-through of the parsed
// response.

import { afterEach, beforeEach, describe, it } from "node:test";
import assert from "node:assert/strict";
import {
  TEST_RUN_CASE_RESULT_STATUSES,
  TEST_RUN_STATUSES,
  addTestRunTester,
  createTestRun,
  deleteTestRun,
  getTestRun,
  getTestRunByUid,
  listTestRunCaseResults,
  listTestRunStepResults,
  listTestRunTesters,
  listTestRuns,
  removeTestRunTester,
  transitionTestRunStatus,
  updateTestRun,
  updateTestRunCaseResult,
  updateTestRunCursor,
  updateTestRunStepResult,
} from "./lib.js";

const BASE_URL = "http://gc-test:8000";
const RUN_ID = "11111111-1111-1111-1111-111111111111";
const PLAN_ID = "22222222-2222-2222-2222-222222222222";
const SUITE_ID = "33333333-3333-3333-3333-333333333333";
const TC_ID = "44444444-4444-4444-4444-444444444444";

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

describe("TestRun enum mirrors", () => {
  it("exposes the five run lifecycle states in order (matches TestRunStatus.java)", () => {
    assert.deepEqual(
      [...TEST_RUN_STATUSES],
      ["PLANNED", "IN_PROGRESS", "COMPLETED", "ABORTED", "ARCHIVED"],
    );
  });

  it("exposes the five case-result statuses in order (matches TestRunCaseResultStatus.java)", () => {
    assert.deepEqual(
      [...TEST_RUN_CASE_RESULT_STATUSES],
      ["NOT_RUN", "PASSED", "FAILED", "BLOCKED", "SKIPPED"],
    );
  });
});

describe("createTestRun (gc_test_run action=create)", () => {
  it("POSTs /api/v1/test-runs with camelCase body and forwards the project param", async () => {
    setNextResponse({
      body: {
        id: RUN_ID,
        uid: "TR-001",
        name: "Smoke pass 1",
        status: "PLANNED",
        testPlanId: PLAN_ID,
        testSuiteId: SUITE_ID,
      },
    });

    const result = await createTestRun(
      {
        uid: "TR-001",
        name: "Smoke pass 1",
        test_plan_id: PLAN_ID,
        test_suite_id: SUITE_ID,
        environment: "staging",
        version: "1.2.0",
        build: "build-42",
        start_at: "2026-06-01T00:00:00Z",
        end_at: "2026-06-30T00:00:00Z",
      },
      "ground-control",
    );

    assert.equal(fetchCalls.length, 1);
    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "POST");
    assert.equal(parsed.pathname, "/api/v1/test-runs");
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    // Critical contract: every field must round-trip to Spring's TestRunRequest.
    // A snake→camel regression on test_plan_id / start_at would silently drop
    // the FK or schedule on the backend.
    assert.deepEqual(JSON.parse(opts.body), {
      uid: "TR-001",
      name: "Smoke pass 1",
      testPlanId: PLAN_ID,
      testSuiteId: SUITE_ID,
      environment: "staging",
      version: "1.2.0",
      build: "build-42",
      startAt: "2026-06-01T00:00:00Z",
      endAt: "2026-06-30T00:00:00Z",
    });
    // Pass-through assertion — an adapter regression that returned undefined
    // for the parsed body would still satisfy the method/path/body shape
    // checks above.
    assert.equal(result.id, RUN_ID);
    assert.equal(result.uid, "TR-001");
  });
});

describe("listTestRuns / getTestRun / getTestRunByUid (routed through gc_query)", () => {
  it("GETs /api/v1/test-runs forwarding the project param", async () => {
    setNextResponse({ body: [{ id: RUN_ID, uid: "TR-001" }] });
    const result = await listTestRuns("ground-control");

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "GET");
    assert.equal(parsed.pathname, "/api/v1/test-runs");
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    assert.equal(result[0].uid, "TR-001");
  });

  it("GETs /api/v1/test-runs/{id} and passes the parsed body through", async () => {
    setNextResponse({ body: { id: RUN_ID, uid: "TR-001" } });
    const result = await getTestRun(RUN_ID, "ground-control");
    const { url } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(parsed.pathname, `/api/v1/test-runs/${RUN_ID}`);
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    assert.equal(result.id, RUN_ID);
  });

  it("GETs /api/v1/test-runs/uid/{uid} and passes the parsed body through", async () => {
    setNextResponse({ body: { id: RUN_ID, uid: "TR-001" } });
    const result = await getTestRunByUid("TR-001", "ground-control");
    const { url } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(parsed.pathname, "/api/v1/test-runs/uid/TR-001");
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    assert.equal(result.uid, "TR-001");
  });
});

describe("updateTestRun (gc_test_run action=update)", () => {
  it("PUTs /api/v1/test-runs/{id} with every clear flag in camelCase", async () => {
    setNextResponse({ body: { id: RUN_ID, uid: "TR-001" } });

    const result = await updateTestRun(
      RUN_ID,
      {
        name: "Renamed",
        environment: "prod",
        version: "2.0.0",
        start_at: "2026-07-01T00:00:00Z",
        end_at: "2026-07-31T00:00:00Z",
        clear_environment: false,
        clear_version: false,
        clear_build: true,
        clear_start_at: false,
        clear_end_at: false,
      },
      "ground-control",
    );

    assert.equal(result.id, RUN_ID);
    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "PUT");
    assert.equal(parsed.pathname, `/api/v1/test-runs/${RUN_ID}`);
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    assert.deepEqual(JSON.parse(opts.body), {
      name: "Renamed",
      environment: "prod",
      version: "2.0.0",
      startAt: "2026-07-01T00:00:00Z",
      endAt: "2026-07-31T00:00:00Z",
      clearEnvironment: false,
      clearVersion: false,
      clearBuild: true,
      clearStartAt: false,
      clearEndAt: false,
    });
  });
});

describe("deleteTestRun (gc_test_run action=delete)", () => {
  it("DELETEs /api/v1/test-runs/{id}", async () => {
    setNextResponse({});
    await deleteTestRun(RUN_ID, "ground-control");
    const { url, opts } = fetchCalls[0];
    assert.equal(opts.method, "DELETE");
    const parsed = new URL(url);
    assert.equal(parsed.pathname, `/api/v1/test-runs/${RUN_ID}`);
    assert.equal(parsed.searchParams.get("project"), "ground-control");
  });
});

describe("transitionTestRunStatus (gc_test_run action=transition)", () => {
  it("PUTs /api/v1/test-runs/{id}/status with the supplied status", async () => {
    setNextResponse({ body: { id: RUN_ID, status: "IN_PROGRESS" } });
    const result = await transitionTestRunStatus(RUN_ID, "IN_PROGRESS", "ground-control");
    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "PUT");
    assert.equal(parsed.pathname, `/api/v1/test-runs/${RUN_ID}/status`);
    assert.deepEqual(JSON.parse(opts.body), { status: "IN_PROGRESS" });
    assert.equal(result.status, "IN_PROGRESS");
  });
});

describe("tester endpoints (gc_test_run actions=add_tester|remove_tester)", () => {
  it("POSTs /api/v1/test-runs/{id}/testers with camelCase body", async () => {
    setNextResponse({ body: { id: "00000000-0000-0000-0000-000000001000", testerName: "Alex Doe" } });
    const result = await addTestRunTester(RUN_ID, "Alex Doe", "ground-control");
    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "POST");
    assert.equal(parsed.pathname, `/api/v1/test-runs/${RUN_ID}/testers`);
    assert.deepEqual(JSON.parse(opts.body), { testerName: "Alex Doe" });
    // request() normalizes the JSON response through toSnakeCase, so the
    // caller sees the field in MCP snake form (mirrors gc_test_suite).
    assert.equal(result.tester_name, "Alex Doe");
  });

  it("GETs /api/v1/test-runs/{id}/testers", async () => {
    setNextResponse({ body: [{ testerName: "Alex" }] });
    const result = await listTestRunTesters(RUN_ID, "ground-control");
    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "GET");
    assert.equal(parsed.pathname, `/api/v1/test-runs/${RUN_ID}/testers`);
    // Backend requireProjectId scopes every read; a regression that dropped
    // the project param would route to the wrong project (or fail 4xx) and
    // a method/path-only assertion would miss it.
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    assert.equal(result[0].tester_name, "Alex");
  });

  it("DELETEs /api/v1/test-runs/{id}/testers/{testerName} with URL-encoded name", async () => {
    setNextResponse({});
    await removeTestRunTester(RUN_ID, "Alex Doe", "ground-control");
    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "DELETE");
    assert.equal(parsed.pathname, `/api/v1/test-runs/${RUN_ID}/testers/Alex%20Doe`);
    assert.equal(parsed.searchParams.get("project"), "ground-control");
  });
});

describe("per-case result endpoints (gc_test_run action=update_result)", () => {
  it("PUTs /api/v1/test-runs/{id}/results/{testCaseId} with camelCase body", async () => {
    setNextResponse({
      body: { id: "00000000-0000-0000-0000-000000002000", status: "FAILED", testCaseUid: "TC-001" },
    });
    const result = await updateTestRunCaseResult(
      RUN_ID,
      TC_ID,
      { status: "FAILED", notes: "Step 3 broken", clearNotes: false },
      "ground-control",
    );
    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "PUT");
    assert.equal(parsed.pathname, `/api/v1/test-runs/${RUN_ID}/results/${TC_ID}`);
    assert.deepEqual(JSON.parse(opts.body), {
      status: "FAILED",
      notes: "Step 3 broken",
      clearNotes: false,
    });
    assert.equal(result.status, "FAILED");
  });

  it("GETs /api/v1/test-runs/{id}/results", async () => {
    setNextResponse({ body: [{ testCaseUid: "TC-001", status: "NOT_RUN" }] });
    const result = await listTestRunCaseResults(RUN_ID, "ground-control");
    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "GET");
    assert.equal(parsed.pathname, `/api/v1/test-runs/${RUN_ID}/results`);
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    assert.equal(result[0].status, "NOT_RUN");
  });
});

// TC-009 / ADR-050 — per-step result + cursor adapter shapes.
const CASE_RESULT_ID = "55555555-5555-5555-5555-555555555555";
const STEP_RESULT_ID = "66666666-6666-6666-6666-666666666666";

describe("per-step result endpoints (gc_test_run actions=update_step_result/update_cursor)", () => {
  it("GETs /api/v1/test-runs/{id}/results/{caseResultId}/steps", async () => {
    setNextResponse({
      body: [
        {
          id: STEP_RESULT_ID,
          testRunCaseResultId: CASE_RESULT_ID,
          stepNumberSnapshot: 1,
          actionSnapshot: "Open",
          expectedResultSnapshot: "Form visible",
          snapshotOrder: 0,
          status: "NOT_RUN",
        },
      ],
    });
    const result = await listTestRunStepResults(RUN_ID, CASE_RESULT_ID, "ground-control");
    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "GET");
    assert.equal(parsed.pathname, `/api/v1/test-runs/${RUN_ID}/results/${CASE_RESULT_ID}/steps`);
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    // toSnakeCase pass on the response — every snapshot field the runner UI
    // displays must survive the camel→snake transformation. A future
    // adapter regression that dropped one of these would render an empty
    // step in the runner; pin each field explicitly.
    assert.equal(result[0].status, "NOT_RUN");
    assert.equal(result[0].step_number_snapshot, 1);
    assert.equal(result[0].action_snapshot, "Open");
    assert.equal(result[0].expected_result_snapshot, "Form visible");
    assert.equal(result[0].snapshot_order, 0);
    assert.equal(result[0].test_run_case_result_id, CASE_RESULT_ID);
  });

  it("PUTs /api/v1/test-runs/{id}/results/{caseResultId}/steps/{stepResultId} with camelCase body", async () => {
    setNextResponse({ body: { id: STEP_RESULT_ID, status: "PASSED" } });
    const result = await updateTestRunStepResult(
      RUN_ID,
      CASE_RESULT_ID,
      STEP_RESULT_ID,
      {
        status: "PASSED",
        comment: "Looks good",
        clearComment: false,
        executedAt: "2026-06-15T12:00:00Z",
        clearExecutedAt: false,
      },
      "ground-control",
    );
    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "PUT");
    assert.equal(
      parsed.pathname,
      `/api/v1/test-runs/${RUN_ID}/results/${CASE_RESULT_ID}/steps/${STEP_RESULT_ID}`,
    );
    assert.deepEqual(JSON.parse(opts.body), {
      status: "PASSED",
      comment: "Looks good",
      clearComment: false,
      executedAt: "2026-06-15T12:00:00Z",
      clearExecutedAt: false,
    });
    assert.equal(result.status, "PASSED");
  });

  it("PUTs /api/v1/test-runs/{id}/cursor with the cursor body", async () => {
    setNextResponse({
      body: {
        id: RUN_ID,
        currentCaseResultId: CASE_RESULT_ID,
        currentStepResultId: STEP_RESULT_ID,
      },
    });
    const result = await updateTestRunCursor(
      RUN_ID,
      {
        currentCaseResultId: CASE_RESULT_ID,
        currentStepResultId: STEP_RESULT_ID,
        clearCursor: false,
      },
      "ground-control",
    );
    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "PUT");
    assert.equal(parsed.pathname, `/api/v1/test-runs/${RUN_ID}/cursor`);
    assert.deepEqual(JSON.parse(opts.body), {
      currentCaseResultId: CASE_RESULT_ID,
      currentStepResultId: STEP_RESULT_ID,
      clearCursor: false,
    });
    // lib.request applies toSnakeCase to the parsed response body, so the
    // assertion targets the snake-cased echo from the response, not the
    // camelCase shape we sent in the body. Both cursor fields are covered
    // so a future toSnakeCase regression that drops the step-cursor would
    // be visible from the test suite.
    assert.equal(result.current_case_result_id, CASE_RESULT_ID);
    assert.equal(result.current_step_result_id, STEP_RESULT_ID);
  });
});
