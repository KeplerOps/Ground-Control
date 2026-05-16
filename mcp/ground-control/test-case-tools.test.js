// Adapter-level tests for the gc_test_case MCP surface (TC-001 / ADR-040).
//
// The lib.js wrappers below back the `gc_test_case` consolidated tool registered
// in index.js (`create | update | delete | transition`) and the reads routed
// through gc_query under the /api/v1/test-cases allowlist.
//
// Each test stubs globalThis.fetch and asserts the outbound HTTP shape (method
// + path + query + camelCase body) plus pass-through of the parsed response.
// Snake-case → camelCase mapping is the gate this file exists for: every
// optional field on TestCaseRequest / UpdateTestCaseRequest must round-trip
// without silent drop-through (the codex cycle-1 finding).

import { afterEach, beforeEach, describe, it } from "node:test";
import assert from "node:assert/strict";
import {
  RequestError,
  TEST_CASE_PRIORITIES,
  TEST_CASE_STATUSES,
  TEST_CASE_TYPES,
  createTestCase,
  createTestCaseStep,
  deleteTestCase,
  deleteTestCaseStep,
  getTestCase,
  getTestCaseByUid,
  listTestCases,
  transitionTestCaseStatus,
  updateTestCase,
  updateTestCaseStep,
} from "./lib.js";

const BASE_URL = "http://gc-test:8000";
const TEST_CASE_ID = "11111111-1111-1111-1111-111111111111";
const STEP_ID = "22222222-2222-2222-2222-222222222222";

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

function parseUrl(call) {
  return new URL(call.url);
}

describe("TestCase enum mirrors", () => {
  it("exposes the four lifecycle states in order (matches TestCaseStatus.java)", () => {
    assert.deepEqual([...TEST_CASE_STATUSES], ["DRAFT", "APPROVED", "DEPRECATED", "ARCHIVED"]);
  });

  it("exposes the three type classifications in order (matches TestCaseType.java)", () => {
    assert.deepEqual([...TEST_CASE_TYPES], ["MANUAL", "AUTOMATED", "HYBRID"]);
  });

  it("exposes the four priority severities in order (matches TestCasePriority.java)", () => {
    assert.deepEqual([...TEST_CASE_PRIORITIES], ["CRITICAL", "HIGH", "MEDIUM", "LOW"]);
  });
});

describe("createTestCase (gc_test_case action=create)", () => {
  it("POSTs /api/v1/test-cases with camelCase body and forwards the project param", async () => {
    setNextResponse({
      body: { id: TEST_CASE_ID, uid: "TC-001", title: "Login flow", status: "DRAFT" },
    });

    const result = await createTestCase(
      {
        uid: "TC-001",
        title: "Login flow",
        type: "MANUAL",
        priority: "HIGH",
        description: "# desc",
        preconditions: "pre",
        postconditions: "post",
        estimated_duration_seconds: 300,
      },
      "ground-control",
    );

    assert.equal(fetchCalls.length, 1);
    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "POST");
    assert.equal(parsed.pathname, "/api/v1/test-cases");
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    // Critical contract — these field names must reach Spring's
    // TestCaseRequest. A regression here causes Jackson to silently drop the
    // field (issue #875 / codex cycle 1 finding).
    assert.deepEqual(JSON.parse(opts.body), {
      uid: "TC-001",
      title: "Login flow",
      type: "MANUAL",
      priority: "HIGH",
      description: "# desc",
      preconditions: "pre",
      postconditions: "post",
      estimatedDurationSeconds: 300,
    });
    assert.equal(result.id, TEST_CASE_ID);
  });
});

describe("updateTestCase (gc_test_case action=update)", () => {
  it("PUTs /api/v1/test-cases/{id} forwarding every clear flag in camelCase", async () => {
    setNextResponse({ body: { id: TEST_CASE_ID, uid: "TC-001" } });

    const result = await updateTestCase(
      TEST_CASE_ID,
      {
        title: "New Title",
        type: "AUTOMATED",
        priority: "CRITICAL",
        estimated_duration_seconds: 900,
        clear_description: true,
        clear_preconditions: false,
        clear_postconditions: true,
        clear_estimated_duration: false,
      },
      "ground-control",
    );

    // Return value passes through so callers can read the updated record without
    // a follow-up GET. A `request()` regression that returned null / raw text
    // for PUT paths would have been invisible without this assertion.
    assert.equal(result.id, TEST_CASE_ID);
    assert.equal(result.uid, "TC-001");

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "PUT");
    assert.equal(parsed.pathname, `/api/v1/test-cases/${TEST_CASE_ID}`);
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    // Every clear flag must reach UpdateTestCaseRequest. If TO_CAMEL is missing
    // an entry, Spring receives `clear_description` (unknown field) and the
    // wipe never happens — visible-via-no-effect failure mode.
    assert.deepEqual(JSON.parse(opts.body), {
      title: "New Title",
      type: "AUTOMATED",
      priority: "CRITICAL",
      estimatedDurationSeconds: 900,
      clearDescription: true,
      clearPreconditions: false,
      clearPostconditions: true,
      clearEstimatedDuration: false,
    });
  });
});

describe("deleteTestCase (gc_test_case action=delete)", () => {
  it("DELETEs /api/v1/test-cases/{id}", async () => {
    setNextResponse({ ok: true, status: 204, body: null });

    await deleteTestCase(TEST_CASE_ID, "ground-control");

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "DELETE");
    assert.equal(parsed.pathname, `/api/v1/test-cases/${TEST_CASE_ID}`);
    assert.equal(parsed.searchParams.get("project"), "ground-control");
  });
});

describe("transitionTestCaseStatus (gc_test_case action=transition)", () => {
  it("PUTs /api/v1/test-cases/{id}/status with {status} body", async () => {
    setNextResponse({ body: { id: TEST_CASE_ID, status: "APPROVED" } });

    const result = await transitionTestCaseStatus(TEST_CASE_ID, "APPROVED", "ground-control");

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "PUT");
    assert.equal(parsed.pathname, `/api/v1/test-cases/${TEST_CASE_ID}/status`);
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    assert.deepEqual(JSON.parse(opts.body), { status: "APPROVED" });
    assert.equal(result.status, "APPROVED");
  });
});

describe("createTestCaseStep (gc_test_case action=step-create)", () => {
  it("POSTs /api/v1/test-cases/{tc}/steps with camelCase body (TC-002)", async () => {
    setNextResponse({
      body: {
        id: STEP_ID,
        testCaseId: TEST_CASE_ID,
        stepNumber: 1,
        action: "Open login page",
        expectedResult: "Page renders",
      },
    });

    const result = await createTestCaseStep(
      TEST_CASE_ID,
      {
        step_number: 1,
        action: "Open login page",
        expected_result: "Page renders",
        actual_result: null,
      },
      "ground-control",
    );

    assert.equal(fetchCalls.length, 1);
    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "POST");
    assert.equal(parsed.pathname, `/api/v1/test-cases/${TEST_CASE_ID}/steps`);
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    // Critical: snake → camel mapping must reach Spring's TestCaseStepRequest.
    // A missing TO_CAMEL entry would have Jackson silently drop the field —
    // same failure mode as TC-001 codex cycle 1.
    assert.deepEqual(JSON.parse(opts.body), {
      stepNumber: 1,
      action: "Open login page",
      expectedResult: "Page renders",
      actualResult: null,
    });
    assert.equal(result.id, STEP_ID);
  });
});

describe("updateTestCaseStep (gc_test_case action=step-update)", () => {
  it("PUTs /api/v1/test-cases/{tc}/steps/{s} with clear_actual_result mapped to camelCase", async () => {
    setNextResponse({ body: { id: STEP_ID, stepNumber: 1, actualResult: null } });

    const result = await updateTestCaseStep(
      TEST_CASE_ID,
      STEP_ID,
      {
        step_number: 2,
        action: "## Updated\n![img](https://example.com/x.png)",
        expected_result: "Updated result",
        clear_actual_result: true,
      },
      "ground-control",
    );

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "PUT");
    assert.equal(parsed.pathname, `/api/v1/test-cases/${TEST_CASE_ID}/steps/${STEP_ID}`);
    // Match every other write-test in this file: assert the `project` query
    // param is forwarded. A regression that dropped `params: { project }` from
    // updateTestCaseStep's request() call would route the update to whichever
    // project the server defaults to instead of the caller's intended one.
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    assert.deepEqual(JSON.parse(opts.body), {
      stepNumber: 2,
      action: "## Updated\n![img](https://example.com/x.png)",
      expectedResult: "Updated result",
      clearActualResult: true,
    });
    assert.equal(result.id, STEP_ID);
  });
});

describe("deleteTestCaseStep (gc_test_case action=step-delete)", () => {
  it("DELETEs /api/v1/test-cases/{tc}/steps/{s}", async () => {
    setNextResponse({ ok: true, status: 204, body: null });

    await deleteTestCaseStep(TEST_CASE_ID, STEP_ID, "ground-control");

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "DELETE");
    assert.equal(parsed.pathname, `/api/v1/test-cases/${TEST_CASE_ID}/steps/${STEP_ID}`);
    assert.equal(parsed.searchParams.get("project"), "ground-control");
  });
});

describe("Read paths (gc_query routing)", () => {
  it("listTestCases GETs /api/v1/test-cases with project param and returns the array", async () => {
    setNextResponse({ body: [{ id: TEST_CASE_ID, uid: "TC-001" }] });
    const result = await listTestCases("ground-control");

    const { url, opts } = fetchCalls[0];
    assert.equal(opts.method, "GET");
    assert.equal(parseUrl(fetchCalls[0]).pathname, "/api/v1/test-cases");
    assert.equal(parseUrl(fetchCalls[0]).searchParams.get("project"), "ground-control");
    // Return value must round-trip — a `request()` regression that returned
    // null for GET paths would silently break every caller of this wrapper.
    assert.ok(Array.isArray(result));
    assert.equal(result.length, 1);
    assert.equal(result[0].id, TEST_CASE_ID);
  });

  it("getTestCase GETs /api/v1/test-cases/{id} and returns the test case body", async () => {
    setNextResponse({ body: { id: TEST_CASE_ID, uid: "TC-001" } });
    const result = await getTestCase(TEST_CASE_ID, "ground-control");

    assert.equal(parseUrl(fetchCalls[0]).pathname, `/api/v1/test-cases/${TEST_CASE_ID}`);
    assert.equal(result.id, TEST_CASE_ID);
    assert.equal(result.uid, "TC-001");
  });

  it("getTestCaseByUid URI-encodes the uid segment and returns the test case body", async () => {
    setNextResponse({ body: { id: TEST_CASE_ID, uid: "TC INT 001" } });
    const result = await getTestCaseByUid("TC INT 001", "ground-control");

    assert.equal(parseUrl(fetchCalls[0]).pathname, "/api/v1/test-cases/uid/TC%20INT%20001");
    assert.equal(result.id, TEST_CASE_ID);
  });
});

describe("Error envelope propagation", () => {
  it("createTestCase surfaces a 422 as RequestError with structured detail", async () => {
    setNextResponse({
      ok: false,
      status: 422,
      body: {
        error: {
          code: "validation_failed",
          message: "estimatedDurationSeconds must be >= 0",
          detail: { field: "estimatedDurationSeconds" },
        },
      },
    });

    await assert.rejects(
      () =>
        createTestCase(
          { uid: "TC-001", title: "x", type: "MANUAL", priority: "LOW", estimated_duration_seconds: -1 },
          "ground-control",
        ),
      (e) =>
        e instanceof RequestError
        && e.status === 422
        && e.code === "validation_failed"
        && e.detail?.field === "estimatedDurationSeconds",
    );
  });

  it("createTestCaseStep surfaces a 409 duplicate_step_number envelope", async () => {
    setNextResponse({
      ok: false,
      status: 409,
      body: {
        error: {
          code: "conflict",
          message: "Step number 1 already exists in test case TC-001",
        },
      },
    });

    await assert.rejects(
      () =>
        createTestCaseStep(
          TEST_CASE_ID,
          { step_number: 1, action: "act", expected_result: "exp" },
          "ground-control",
        ),
      (e) => e instanceof RequestError && e.status === 409,
    );
  });

  it("transition surfaces a 422 invalid_status_transition envelope", async () => {
    setNextResponse({
      ok: false,
      status: 422,
      body: {
        error: {
          code: "invalid_status_transition",
          message: "Cannot transition test case status from APPROVED to DRAFT",
          detail: { current: "APPROVED", requested: "DRAFT" },
        },
      },
    });

    await assert.rejects(
      () => transitionTestCaseStatus(TEST_CASE_ID, "DRAFT", "ground-control"),
      (e) => e instanceof RequestError && e.status === 422 && e.code === "invalid_status_transition",
    );
  });
});
