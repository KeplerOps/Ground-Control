// Adapter-level tests for the gc_test_suite MCP surface (TC-007 / ADR-047).
//
// The lib.js wrappers below back the `gc_test_suite` consolidated tool
// registered in index.js (create | update | delete | resolve | add_member |
// remove_member | reorder_members | add_source_requirement |
// remove_source_requirement) and the reads routed through gc_query under
// the /api/v1/test-suites allowlist.
//
// Each test stubs globalThis.fetch and asserts the outbound HTTP shape
// (method + path + query + camelCase body) plus pass-through of the parsed
// response. Snake-case → camelCase mapping is the gate this file exists for:
// every optional field on TestSuiteRequest / UpdateTestSuiteRequest /
// AddTestSuiteMemberRequest / etc. must round-trip without silent
// drop-through.

import { afterEach, beforeEach, describe, it } from "node:test";
import assert from "node:assert/strict";
import {
  TEST_SUITE_POPULATION_MODES,
  addTestSuiteMember,
  addTestSuiteSourceRequirement,
  createTestSuite,
  deleteTestSuite,
  getTestSuite,
  getTestSuiteByUid,
  listTestSuites,
  removeTestSuiteMember,
  removeTestSuiteSourceRequirement,
  reorderTestSuiteMembers,
  resolveTestSuiteTestCases,
  updateTestSuite,
} from "./lib.js";

const BASE_URL = "http://gc-test:8000";
const SUITE_ID = "11111111-1111-1111-1111-111111111111";
const TC_ID = "22222222-2222-2222-2222-222222222222";
const REQ_ID = "33333333-3333-3333-3333-333333333333";

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

describe("TestSuite enum mirror", () => {
  it("exposes the three population modes in order (matches TestSuitePopulationMode.java)", () => {
    assert.deepEqual(
      [...TEST_SUITE_POPULATION_MODES],
      ["STATIC", "REQUIREMENTS_BASED", "QUERY_BASED"],
    );
  });
});

describe("createTestSuite (gc_test_suite action=create)", () => {
  it("POSTs /api/v1/test-suites with camelCase body and forwards the project param", async () => {
    setNextResponse({
      body: {
        id: SUITE_ID,
        uid: "TS-001",
        name: "Wave-1 selection",
        populationMode: "STATIC",
      },
    });

    const result = await createTestSuite(
      {
        uid: "TS-001",
        name: "Wave-1 selection",
        description: "notes",
        population_mode: "STATIC",
      },
      "ground-control",
    );

    assert.equal(fetchCalls.length, 1);
    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "POST");
    assert.equal(parsed.pathname, "/api/v1/test-suites");
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    assert.deepEqual(JSON.parse(opts.body), {
      uid: "TS-001",
      name: "Wave-1 selection",
      description: "notes",
      populationMode: "STATIC",
    });
    assert.equal(result.id, SUITE_ID);
  });

  it("camelCases QUERY_BASED criteria fields", async () => {
    setNextResponse({ body: { id: SUITE_ID, uid: "TS-Q-001", populationMode: "QUERY_BASED" } });

    await createTestSuite(
      {
        uid: "TS-Q-001",
        name: "n",
        population_mode: "QUERY_BASED",
        criteria_status: "APPROVED",
        criteria_type: "AUTOMATED",
        criteria_priority: "HIGH",
        criteria_format: "STEP_BASED",
        criteria_folder_id: "44444444-4444-4444-4444-444444444444",
        criteria_text_search: "payment",
      },
      "ground-control",
    );

    const { opts } = fetchCalls[0];
    const body = JSON.parse(opts.body);
    assert.equal(body.populationMode, "QUERY_BASED");
    assert.equal(body.criteriaStatus, "APPROVED");
    assert.equal(body.criteriaType, "AUTOMATED");
    assert.equal(body.criteriaPriority, "HIGH");
    assert.equal(body.criteriaFormat, "STEP_BASED");
    assert.equal(body.criteriaFolderId, "44444444-4444-4444-4444-444444444444");
    assert.equal(body.criteriaTextSearch, "payment");
  });
});

describe("listTestSuites (read routed through gc_query)", () => {
  it("GETs /api/v1/test-suites and forwards the project param", async () => {
    setNextResponse({ body: [{ id: SUITE_ID, uid: "TS-001" }] });

    const result = await listTestSuites("ground-control");

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "GET");
    assert.equal(parsed.pathname, "/api/v1/test-suites");
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    assert.equal(Array.isArray(result), true);
    assert.equal(result[0].uid, "TS-001");
  });
});

describe("getTestSuite / getTestSuiteByUid", () => {
  it("GETs /api/v1/test-suites/{id}", async () => {
    setNextResponse({ body: { id: SUITE_ID, uid: "TS-001" } });

    const result = await getTestSuite(SUITE_ID, "ground-control");

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "GET");
    assert.equal(parsed.pathname, `/api/v1/test-suites/${SUITE_ID}`);
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    assert.equal(result.id, SUITE_ID);
    assert.equal(result.uid, "TS-001");
  });

  it("GETs /api/v1/test-suites/uid/{uid}", async () => {
    setNextResponse({ body: { id: SUITE_ID, uid: "TS-001" } });

    const result = await getTestSuiteByUid("TS-001", "ground-control");

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "GET");
    assert.equal(parsed.pathname, "/api/v1/test-suites/uid/TS-001");
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    assert.equal(result.id, SUITE_ID);
  });
});

describe("updateTestSuite (gc_test_suite action=update)", () => {
  it("PUTs /api/v1/test-suites/{id} forwarding every clear flag in camelCase", async () => {
    setNextResponse({ body: { id: SUITE_ID, uid: "TS-001" } });

    const result = await updateTestSuite(
      SUITE_ID,
      {
        name: "Renamed",
        description: "updated",
        criteria_status: "APPROVED",
        clear_description: false,
        clear_criteria_status: false,
        clear_criteria_type: false,
        clear_criteria_priority: false,
        clear_criteria_format: false,
        clear_criteria_folder_id: false,
        clear_criteria_text_search: true,
      },
      "ground-control",
    );

    assert.equal(result.id, SUITE_ID);

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "PUT");
    assert.equal(parsed.pathname, `/api/v1/test-suites/${SUITE_ID}`);
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    const body = JSON.parse(opts.body);
    assert.equal(body.name, "Renamed");
    assert.equal(body.description, "updated");
    assert.equal(body.criteriaStatus, "APPROVED");
    // Every clear flag must round-trip to its camelCase backend name.
    // Omitting any of these would let a silent TO_CAMEL drop-through ship
    // (test-quality cycle 1 F2).
    assert.equal(body.clearDescription, false);
    assert.equal(body.clearCriteriaStatus, false);
    assert.equal(body.clearCriteriaType, false);
    assert.equal(body.clearCriteriaPriority, false);
    assert.equal(body.clearCriteriaFormat, false);
    assert.equal(body.clearCriteriaFolderId, false);
    assert.equal(body.clearCriteriaTextSearch, true);
  });
});

describe("deleteTestSuite (gc_test_suite action=delete)", () => {
  it("DELETEs /api/v1/test-suites/{id}", async () => {
    setNextResponse({ status: 204 });

    await deleteTestSuite(SUITE_ID, "ground-control");

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "DELETE");
    assert.equal(parsed.pathname, `/api/v1/test-suites/${SUITE_ID}`);
    // Project param must reach the API — without it a multi-project
    // deployment could match across project boundaries (test-quality
    // cycle 1 F4).
    assert.equal(parsed.searchParams.get("project"), "ground-control");
  });
});

describe("resolveTestSuiteTestCases (gc_test_suite action=resolve)", () => {
  it("GETs /api/v1/test-suites/{id}/test-cases", async () => {
    setNextResponse({ body: [{ id: TC_ID, uid: "TC-001" }] });

    const result = await resolveTestSuiteTestCases(SUITE_ID, "ground-control");

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "GET");
    assert.equal(parsed.pathname, `/api/v1/test-suites/${SUITE_ID}/test-cases`);
    assert.equal(parsed.searchParams.get("project"), "ground-control");
    assert.equal(Array.isArray(result), true);
    assert.equal(result[0].uid, "TC-001");
  });
});

describe("addTestSuiteMember / removeTestSuiteMember / reorderTestSuiteMembers", () => {
  it("POSTs /api/v1/test-suites/{id}/members with camelCase body", async () => {
    setNextResponse({ body: { id: "55555555-5555-5555-5555-555555555555", testCaseId: TC_ID } });

    const result = await addTestSuiteMember(SUITE_ID, { test_case_id: TC_ID, position: 2 }, "ground-control");

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "POST");
    assert.equal(parsed.pathname, `/api/v1/test-suites/${SUITE_ID}/members`);
    const body = JSON.parse(opts.body);
    assert.equal(body.testCaseId, TC_ID);
    assert.equal(body.position, 2);
    // Responses round-trip through toSnakeCase before reaching the caller.
    assert.equal(result.test_case_id, TC_ID);
  });

  it("DELETEs a member by test_case_id", async () => {
    setNextResponse({ status: 204 });

    await removeTestSuiteMember(SUITE_ID, TC_ID, "ground-control");

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "DELETE");
    assert.equal(parsed.pathname, `/api/v1/test-suites/${SUITE_ID}/members/${TC_ID}`);
    assert.equal(parsed.searchParams.get("project"), "ground-control");
  });

  it("PUTs /members/reorder with a camelCase orderedTestCaseIds list", async () => {
    setNextResponse({ body: [] });

    const ids = [TC_ID, "66666666-6666-6666-6666-666666666666"];
    await reorderTestSuiteMembers(SUITE_ID, ids, "ground-control");

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "PUT");
    assert.equal(parsed.pathname, `/api/v1/test-suites/${SUITE_ID}/members/reorder`);
    const body = JSON.parse(opts.body);
    assert.deepEqual(body.orderedTestCaseIds, ids);
  });
});

describe("addTestSuiteSourceRequirement / removeTestSuiteSourceRequirement", () => {
  it("POSTs /source-requirements with camelCase requirementId", async () => {
    setNextResponse({ body: { id: "77777777-7777-7777-7777-777777777777", requirementId: REQ_ID } });

    await addTestSuiteSourceRequirement(SUITE_ID, { requirement_id: REQ_ID }, "ground-control");

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "POST");
    assert.equal(parsed.pathname, `/api/v1/test-suites/${SUITE_ID}/source-requirements`);
    const body = JSON.parse(opts.body);
    assert.equal(body.requirementId, REQ_ID);
  });

  it("DELETEs /source-requirements/{requirementId}", async () => {
    setNextResponse({ status: 204 });

    await removeTestSuiteSourceRequirement(SUITE_ID, REQ_ID, "ground-control");

    const { url, opts } = fetchCalls[0];
    const parsed = new URL(url);
    assert.equal(opts.method, "DELETE");
    assert.equal(parsed.pathname, `/api/v1/test-suites/${SUITE_ID}/source-requirements/${REQ_ID}`);
    assert.equal(parsed.searchParams.get("project"), "ground-control");
  });
});
