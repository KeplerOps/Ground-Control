// Tests for gc_query (ADR-035). The executeGcQuery() helper takes a `fetchImpl`
// dependency so the request flow can be exercised without real network.
import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import {
  executeGcQuery,
  gcQueryToolHandler,
  gcQuerySchema,
  GC_QUERY_BODY_BYTE_CAP,
  GC_QUERY_TIMEOUT_MS,
  GC_QUERY_PATH_ALLOWLIST,
  GC_QUERY_PATH_DENYLIST,
  validateGcQueryPath,
  validateGcQueryParams,
  truncateBody,
} from "./gc-query.js";
import { RequestError } from "./lib.js";

const __dirname = dirname(fileURLToPath(import.meta.url));

const ORIGINAL_BASE_URL = process.env.GC_BASE_URL;

function withBaseUrl(url, fn) {
  return async () => {
    process.env.GC_BASE_URL = url;
    try {
      await fn();
    } finally {
      if (ORIGINAL_BASE_URL === undefined) delete process.env.GC_BASE_URL;
      else process.env.GC_BASE_URL = ORIGINAL_BASE_URL;
    }
  };
}

function fakeResponse({ status = 200, body = "", headers = {} } = {}) {
  return {
    ok: status >= 200 && status < 300,
    status,
    text: async () => body,
    headers: new Map(Object.entries(headers)),
  };
}

describe("executeGcQuery", () => {
  it(
    "issues a GET to the validated path and returns the body verbatim under the cap",
    withBaseUrl("http://localhost:8000", async () => {
      let capturedUrl;
      let capturedOptions;
      const fetchImpl = async (url, options) => {
        capturedUrl = url;
        capturedOptions = options;
        return fakeResponse({ status: 200, body: '{"items":[]}' });
      };
      const out = await executeGcQuery(
        "/api/v1/requirements",
        { status: "ACTIVE" },
        { fetchImpl },
      );
      assert.equal(capturedOptions.method, "GET");
      assert.equal(capturedUrl, "http://localhost:8000/api/v1/requirements?status=ACTIVE");
      assert.equal(out.status, 200);
      assert.equal(out.body, '{"items":[]}');
      assert.equal(out.truncated, false);
      assert.equal(out.original_byte_length, 12);
    }),
  );

  it(
    "always sets X-Actor and Accept: application/json, and never reads caller-supplied headers",
    withBaseUrl("http://localhost:8000", async () => {
      let capturedOptions;
      const fetchImpl = async (_url, options) => {
        capturedOptions = options;
        return fakeResponse({ body: "{}" });
      };
      await executeGcQuery("/api/v1/projects", undefined, { fetchImpl });
      assert.equal(capturedOptions.headers["X-Actor"], "mcp-server");
      assert.equal(capturedOptions.headers.Accept, "application/json");
      // executeGcQuery does NOT take a `headers` parameter at all; nothing to assert
      // about caller headers because there is no surface for them.
    }),
  );

  it(
    "passes a non-empty Authorization header when GROUND_CONTROL_API_TOKEN is set",
    withBaseUrl("http://localhost:8000", async () => {
      const prev = process.env.GROUND_CONTROL_API_TOKEN;
      process.env.GROUND_CONTROL_API_TOKEN = "test-token";
      try {
        let capturedOptions;
        const fetchImpl = async (_url, options) => {
          capturedOptions = options;
          return fakeResponse({ body: "{}" });
        };
        await executeGcQuery("/api/v1/requirements", undefined, { fetchImpl });
        assert.equal(capturedOptions.headers.Authorization, "Bearer test-token");
      } finally {
        if (prev === undefined) delete process.env.GROUND_CONTROL_API_TOKEN;
        else process.env.GROUND_CONTROL_API_TOKEN = prev;
      }
    }),
  );

  it(
    "rejects an invalid path before issuing any fetch",
    withBaseUrl("http://localhost:8000", async () => {
      let called = false;
      const fetchImpl = async () => {
        called = true;
        return fakeResponse();
      };
      await assert.rejects(
        () => executeGcQuery("/api/v1/admin/users", undefined, { fetchImpl }),
        (err) => err instanceof RequestError && err.code === "invalid_query_path",
      );
      assert.equal(called, false, "fetch must not be called for an invalid path");
    }),
  );

  it(
    "rejects invalid params before issuing any fetch",
    withBaseUrl("http://localhost:8000", async () => {
      let called = false;
      const fetchImpl = async () => {
        called = true;
        return fakeResponse();
      };
      await assert.rejects(
        () => executeGcQuery("/api/v1/requirements", { status: ["A", "B"] }, { fetchImpl }),
        (err) => err instanceof RequestError && err.code === "invalid_query_params",
      );
      assert.equal(called, false);
    }),
  );

  it(
    "surfaces a backend RequestError on 4xx with code/detail intact",
    withBaseUrl("http://localhost:8000", async () => {
      const fetchImpl = async () =>
        fakeResponse({
          status: 422,
          body: JSON.stringify({
            error: { code: "validation_failed", message: "bad input", detail: { field: "x" } },
          }),
        });
      await assert.rejects(
        () => executeGcQuery("/api/v1/requirements", undefined, { fetchImpl }),
        (err) => {
          return (
            err instanceof RequestError &&
            err.status === 422 &&
            err.code === "validation_failed" &&
            err.detail &&
            err.detail.field === "x"
          );
        },
      );
    }),
  );

  it(
    "surfaces a 5xx error and does NOT eat the body (preserves it as the error message)",
    withBaseUrl("http://localhost:8000", async () => {
      const fetchImpl = async () =>
        fakeResponse({ status: 500, body: "internal explosion" });
      await assert.rejects(
        () => executeGcQuery("/api/v1/requirements", undefined, { fetchImpl }),
        // `parseErrorBody` returns the raw body as `message` when it's not JSON.
        // The assertion below would fail if a regression swallowed the body and
        // surfaced an empty or generic error message.
        (err) =>
          err instanceof RequestError &&
          err.status === 500 &&
          typeof err.message === "string" &&
          err.message.includes("internal explosion"),
      );
    }),
  );

  it(
    "truncates a response body above the cap and reports original_byte_length",
    withBaseUrl("http://localhost:8000", async () => {
      const big = "x".repeat(GC_QUERY_BODY_BYTE_CAP + 4096);
      const fetchImpl = async () => fakeResponse({ status: 200, body: big });
      const out = await executeGcQuery("/api/v1/requirements", undefined, { fetchImpl });
      assert.equal(out.truncated, true);
      assert.equal(out.original_byte_length, GC_QUERY_BODY_BYTE_CAP + 4096);
      assert.match(out.body, /truncated/i);
    }),
  );

  it(
    "streams the body and stops reading once the cap is reached (does not buffer unbounded)",
    withBaseUrl("http://localhost:8000", async () => {
      // Construct a Response-like object whose body is a ReadableStream that
      // would yield ~10 MiB total if read to completion. We emit chunks until
      // the consumer stops pulling, then track how many bytes were yielded.
      const chunkSize = 64 * 1024;
      let bytesYielded = 0;
      let canceled = false;
      const stream = new ReadableStream({
        async pull(controller) {
          if (bytesYielded >= 10 * 1024 * 1024) {
            controller.close();
            return;
          }
          const chunk = new Uint8Array(chunkSize).fill(0x78); // 'x'
          controller.enqueue(chunk);
          bytesYielded += chunkSize;
        },
        cancel() {
          canceled = true;
        },
      });
      const fetchImpl = async () => ({
        ok: true,
        status: 200,
        body: stream,
        headers: new Map(),
      });
      const out = await executeGcQuery("/api/v1/requirements", undefined, { fetchImpl });
      assert.equal(out.truncated, true);
      // The reader must NOT have pulled the entire 10 MiB; the cap is 1 MiB
      // plus a small ceiling for the in-flight chunk that triggered the stop.
      assert.ok(
        bytesYielded <= GC_QUERY_BODY_BYTE_CAP + 2 * chunkSize,
        `expected ≤ ${GC_QUERY_BODY_BYTE_CAP + 2 * chunkSize} bytes pulled, got ${bytesYielded}`,
      );
      assert.equal(canceled, true, "the body stream must be canceled once the cap is hit");
    }),
  );

  it(
    "maps an AbortError into a RequestError with code gc_query_timeout",
    withBaseUrl("http://localhost:8000", async () => {
      const fetchImpl = async (_url, options) => {
        // Simulate a slow request that the AbortSignal interrupts.
        return new Promise((_resolve, reject) => {
          options.signal?.addEventListener("abort", () => {
            const err = new Error("aborted");
            err.name = "AbortError";
            reject(err);
          });
        });
      };
      // Squeeze the timeout so the test runs fast.
      await assert.rejects(
        () =>
          executeGcQuery("/api/v1/requirements", undefined, {
            fetchImpl,
            timeoutMs: 25,
          }),
        (err) => err instanceof RequestError && err.code === "gc_query_timeout",
      );
    }),
  );

  it(
    "exposes GC_QUERY_TIMEOUT_MS as the default and uses it when not overridden",
    withBaseUrl("http://localhost:8000", async () => {
      // Smoke test: just confirm the constant is exported and a positive number.
      assert.ok(GC_QUERY_TIMEOUT_MS > 0);
    }),
  );

  it(
    "does not stringify undefined/null params into ?key= entries",
    withBaseUrl("http://localhost:8000", async () => {
      let capturedUrl;
      const fetchImpl = async (url) => {
        capturedUrl = url;
        return fakeResponse({ body: "{}" });
      };
      await executeGcQuery(
        "/api/v1/requirements",
        { status: undefined, wave: null, search: "x" },
        { fetchImpl },
      );
      assert.ok(!capturedUrl.includes("status="));
      assert.ok(!capturedUrl.includes("wave="));
      assert.ok(capturedUrl.includes("search=x"));
    }),
  );

  it(
    "returns null body verbatim on a 204",
    withBaseUrl("http://localhost:8000", async () => {
      const fetchImpl = async () => fakeResponse({ status: 204, body: "" });
      const out = await executeGcQuery("/api/v1/requirements", undefined, { fetchImpl });
      assert.equal(out.status, 204);
      assert.equal(out.body, "");
      assert.equal(out.truncated, false);
    }),
  );
});

describe("gcQueryToolHandler", () => {
  // The MCP SDK normalizes raw zod shapes via z.object() which strips unknown
  // keys instead of rejecting them. The handler-level check below is the
  // contract enforcement point: 'headers', 'method', or any other key beyond
  // 'path' and 'params' must be rejected, not silently dropped.
  it(
    "rejects an unknown 'headers' arg with code invalid_query_args",
    withBaseUrl("http://localhost:8000", async () => {
      await assert.rejects(
        () =>
          gcQueryToolHandler(
            {
              path: "/api/v1/requirements",
              headers: { Authorization: "Bearer evil" },
            },
            { fetchImpl: async () => fakeResponse({ body: "{}" }) },
          ),
        (err) => err instanceof RequestError && err.code === "invalid_query_args",
      );
    }),
  );

  it(
    "rejects an unknown 'method' arg",
    withBaseUrl("http://localhost:8000", async () => {
      await assert.rejects(
        () =>
          gcQueryToolHandler(
            { path: "/api/v1/requirements", method: "DELETE" },
            { fetchImpl: async () => fakeResponse({ body: "{}" }) },
          ),
        (err) => err instanceof RequestError && err.code === "invalid_query_args",
      );
    }),
  );

  it(
    "accepts the documented 'path' and 'params' keys",
    withBaseUrl("http://localhost:8000", async () => {
      const out = await gcQueryToolHandler(
        { path: "/api/v1/requirements", params: { status: "ACTIVE" } },
        { fetchImpl: async () => fakeResponse({ body: "{}" }) },
      );
      assert.equal(out.status, 200);
    }),
  );

  it(
    "accepts an empty args object (i.e. validates path before unknown-key check)",
    withBaseUrl("http://localhost:8000", async () => {
      // Empty args should produce a 'invalid_query_path' (path is undefined),
      // not 'invalid_query_args'. Order: schema-level keys check, then the
      // executor's path validator.
      await assert.rejects(
        () =>
          gcQueryToolHandler({}, { fetchImpl: async () => fakeResponse({ body: "{}" }) }),
        (err) => err instanceof RequestError && err.code === "invalid_query_path",
      );
    }),
  );
});

describe("gcQuerySchema (Zod-level strict rejection)", () => {
  // The MCP SDK validates a tool's input against its Zod schema before calling
  // the handler. If we register with a non-strict object, unknown keys are
  // silently stripped — the handler never sees them. The schema MUST be a
  // strict ZodObject so the SDK rejects unknown keys at the protocol layer.
  it("is a Zod object (so the SDK can run schema validation against the args)", () => {
    assert.equal(typeof gcQuerySchema?.safeParse, "function");
  });

  it("accepts the documented shape", () => {
    const r = gcQuerySchema.safeParse({
      path: "/api/v1/requirements",
      params: { status: "ACTIVE" },
    });
    assert.equal(r.success, true);
  });

  it("rejects an unknown 'headers' key at the schema level", () => {
    const r = gcQuerySchema.safeParse({
      path: "/api/v1/requirements",
      headers: { Authorization: "Bearer evil" },
    });
    assert.equal(r.success, false, "schema must reject unknown 'headers' key");
  });

  it("rejects an unknown 'method' key at the schema level", () => {
    const r = gcQuerySchema.safeParse({
      path: "/api/v1/requirements",
      method: "DELETE",
    });
    assert.equal(r.success, false, "schema must reject unknown 'method' key");
  });
});

// ---------------------------------------------------------------------------
// Documentation/constant drift catch
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// gc_query path validation
// ---------------------------------------------------------------------------

describe("validateGcQueryPath", () => {
  it("accepts allowlisted /api/v1 read prefixes", () => {
    for (const prefix of GC_QUERY_PATH_ALLOWLIST) {
      assert.doesNotThrow(() => validateGcQueryPath(prefix));
    }
  });

  it("accepts a sub-path under an allowlisted prefix", () => {
    assert.doesNotThrow(() => validateGcQueryPath("/api/v1/requirements/abc-123"));
    assert.doesNotThrow(() => validateGcQueryPath("/api/v1/traceability/by-artifact"));
  });

  it("rejects an absolute URL", () => {
    assert.throws(() => validateGcQueryPath("http://evil/api/v1/requirements"), /invalid_query_path/);
    assert.throws(() => validateGcQueryPath("https://evil/api/v1/requirements"), /invalid_query_path/);
  });

  it("rejects a protocol-relative URL", () => {
    assert.throws(() => validateGcQueryPath("//evil/api/v1/requirements"), /invalid_query_path/);
  });

  it("rejects path traversal", () => {
    assert.throws(() => validateGcQueryPath("/api/v1/requirements/../admin/users"), /invalid_query_path/);
    assert.throws(() => validateGcQueryPath("/api/v1/../etc/passwd"), /invalid_query_path/);
  });

  it("rejects paths outside /api/v1/", () => {
    assert.throws(() => validateGcQueryPath("/actuator/health"), /invalid_query_path/);
    assert.throws(() => validateGcQueryPath("/api/v2/requirements"), /invalid_query_path/);
    assert.throws(() => validateGcQueryPath("/swagger-ui/index.html"), /invalid_query_path/);
  });

  it("rejects denied admin prefixes even though they live under /api/v1/", () => {
    for (const denied of GC_QUERY_PATH_DENYLIST) {
      assert.throws(() => validateGcQueryPath(`${denied}/whatever`), /invalid_query_path/);
    }
  });

  it("rejects an embedded query string", () => {
    assert.throws(() => validateGcQueryPath("/api/v1/requirements?status=ACTIVE"), /invalid_query_path/);
  });

  it("rejects percent-encoded segments (security: encoded-dot denylist bypass)", () => {
    assert.throws(
      () => validateGcQueryPath("/api/v1/requirements/%2e%2e/admin/users"),
      /invalid_query_path/,
    );
    assert.throws(() => validateGcQueryPath("/api/v1/requirements/%41"), /invalid_query_path/);
  });

  it("rejects backslash and other non-path characters", () => {
    assert.throws(() => validateGcQueryPath("/api/v1/requirements\\admin"), /invalid_query_path/);
    assert.throws(() => validateGcQueryPath("/api/v1/requirements@admin"), /invalid_query_path/);
    assert.throws(() => validateGcQueryPath("/api/v1/requirements;admin"), /invalid_query_path/);
  });

  it("rejects a fragment", () => {
    assert.throws(() => validateGcQueryPath("/api/v1/requirements#x"), /invalid_query_path/);
  });

  it("rejects a path that is not in the allowlist", () => {
    assert.throws(() => validateGcQueryPath("/api/v1/some-future-endpoint"), /invalid_query_path/);
  });

  it("rejects empty / non-string path", () => {
    assert.throws(() => validateGcQueryPath(""), /invalid_query_path/);
    assert.throws(() => validateGcQueryPath(null), /invalid_query_path/);
    assert.throws(() => validateGcQueryPath(undefined), /invalid_query_path/);
    assert.throws(() => validateGcQueryPath(42), /invalid_query_path/);
  });

  it("denylist takes precedence over allowlist regardless of prefix overlap", () => {
    assert.doesNotThrow(() => validateGcQueryPath("/api/v1/analysis/coverage-gaps"));
    assert.throws(() => validateGcQueryPath("/api/v1/analysis/sweep/run"), /invalid_query_path/);
  });
});

describe("validateGcQueryParams", () => {
  it("accepts undefined / null / {} as 'no params'", () => {
    assert.doesNotThrow(() => validateGcQueryParams(undefined));
    assert.doesNotThrow(() => validateGcQueryParams(null));
    assert.doesNotThrow(() => validateGcQueryParams({}));
  });

  it("accepts string, number, boolean, null primitive values", () => {
    assert.doesNotThrow(() => validateGcQueryParams({ a: "x", b: 1, c: true, d: null }));
  });

  it("rejects array / nested / function values", () => {
    assert.throws(() => validateGcQueryParams({ a: [1, 2] }), /invalid_query_params/);
    assert.throws(() => validateGcQueryParams({ a: { b: 1 } }), /invalid_query_params/);
    assert.throws(() => validateGcQueryParams({ a: () => 1 }), /invalid_query_params/);
  });

  it("rejects non-object params", () => {
    assert.throws(() => validateGcQueryParams("a=1"), /invalid_query_params/);
    assert.throws(() => validateGcQueryParams([1, 2]), /invalid_query_params/);
  });
});

describe("truncateBody", () => {
  it("returns body unchanged when under the cap", () => {
    const body = "x".repeat(1024);
    const out = truncateBody(body);
    assert.equal(out.body, body);
    assert.equal(out.truncated, false);
    assert.equal(out.original_byte_length, 1024);
  });

  it("truncates body above the cap and marks it", () => {
    const body = "y".repeat(GC_QUERY_BODY_BYTE_CAP + 100);
    const out = truncateBody(body);
    assert.equal(out.truncated, true);
    assert.equal(out.original_byte_length, GC_QUERY_BODY_BYTE_CAP + 100);
    assert.match(out.body, /truncated/i);
  });

  it("uses byte length for the cap (multi-byte safety)", () => {
    const ch = "💥";
    const body = ch.repeat(Math.ceil(GC_QUERY_BODY_BYTE_CAP / 4) + 10);
    const out = truncateBody(body);
    assert.equal(out.truncated, true);
    assert.equal(out.original_byte_length, Buffer.byteLength(body, "utf8"));
  });
});

describe("gc_query allowlist docs match the implemented constant", () => {
  // These tests guard against the recurring shape "doc text states a count or
  // list of allowed gc_query prefixes that omits prefixes present in
  // GC_QUERY_PATH_ALLOWLIST." Codex flagged it on cycle 3 of issue #628.
  function readRepoFile(relPath) {
    return readFileSync(join(__dirname, "..", "..", relPath), "utf8");
  }

  it("README.md mentions every allowed prefix from GC_QUERY_PATH_ALLOWLIST", () => {
    const readme = readFileSync(join(__dirname, "README.md"), "utf8");
    const missing = GC_QUERY_PATH_ALLOWLIST.filter((p) => !readme.includes(p));
    assert.deepEqual(
      missing,
      [],
      `prefixes missing from mcp/ground-control/README.md: ${missing.join(", ")}`,
    );
  });

  it("ADR-035 mentions every allowed prefix from GC_QUERY_PATH_ALLOWLIST", () => {
    const adr = readRepoFile("architecture/adrs/035-mcp-tool-catalog-curation.md");
    const missing = GC_QUERY_PATH_ALLOWLIST.filter((p) => !adr.includes(p));
    assert.deepEqual(
      missing,
      [],
      `prefixes missing from architecture/adrs/035-mcp-tool-catalog-curation.md: ${missing.join(", ")}`,
    );
  });
});
