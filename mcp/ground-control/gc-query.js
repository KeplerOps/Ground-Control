// gc_query: read-only REST escape hatch for ad-hoc agent queries (ADR-035).
//
// The MCP-side surface is intentionally narrow:
//   - GET only (no method parameter exists)
//   - /api/v1/** paths only, allowlisted prefixes only, denylist takes precedence
//   - flat params object, primitive values only
//   - 30s timeout via AbortController, kept alive through body consumption
//   - 1 MiB response-body cap, enforced by streaming the body and stopping the
//     reader once the cap is reached (does NOT buffer unbounded before the cap)
//   - no caller-supplied headers (the MCP schema does not expose any)
//   - reuses lib.js's buildUrl, addAuthorizationHeader, RequestError, and
//     parseErrorBody so error semantics match the rest of the adapter.
//
// Validation lives in catalogs.js so the same allowlist that hides admin
// catalogs from the default surface also rejects admin paths from gc_query.

import { z } from "zod";
import {
  buildUrl,
  parseErrorBody,
  RequestError,
  addAuthorizationHeader,
} from "./lib.js";
import {
  GC_QUERY_BODY_BYTE_CAP,
  GC_QUERY_TIMEOUT_MS,
  truncateBody,
  validateGcQueryParams,
  validateGcQueryPath,
} from "./catalogs.js";

/**
 * Stream-read a Response.body up to byteCap bytes. Cancels the underlying
 * stream (and therefore stops the network read) once the cap is reached, so
 * the request does not buffer unbounded data into memory.
 *
 * Returns the bytes actually read as a Buffer, plus a flag indicating whether
 * truncation happened during reading. The cap covers the full request lifetime
 * (the AbortController must remain armed for the duration of this call so the
 * timeout still bites if the body never finishes).
 *
 * @param {ReadableStream<Uint8Array>} body
 * @param {number} byteCap
 * @returns {Promise<{ buffer: Buffer, hitCap: boolean }>}
 */
async function readBoundedBody(body, byteCap) {
  if (!body || typeof body.getReader !== "function") {
    // Fallback for stub responses in tests that override `.text()` instead of
    // exposing a stream — read the text and treat it as the bytes.
    return { buffer: null, hitCap: false };
  }
  const reader = body.getReader();
  const chunks = [];
  let received = 0;
  let hitCap = false;
  try {
    // eslint-disable-next-line no-constant-condition
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      if (!value) continue;
      received += value.byteLength;
      chunks.push(value);
      if (received >= byteCap) {
        hitCap = true;
        break;
      }
    }
  } finally {
    try {
      await reader.cancel();
    } catch {
      // The stream may already be closed; cancellation is best-effort.
    }
  }
  const buffer = Buffer.concat(chunks.map((c) => Buffer.from(c)));
  return { buffer, hitCap };
}

/**
 * Issue a validated, allowlisted GET against /api/v1/** and return the raw
 * response body, capped (with a clear marker) if it exceeded
 * GC_QUERY_BODY_BYTE_CAP. Body consumption shares the request's
 * AbortController so the 30s timeout covers the entire request lifetime, not
 * just header arrival.
 *
 * Validation throws are surfaced as RequestError(status=0, code) so the MCP
 * `err()` renderer reports them with the same envelope shape as backend
 * errors.
 *
 * @param {string} path        validated /api/v1/** prefix path
 * @param {object} [params]    flat object of primitive query params
 * @param {object} [opts]
 * @param {function} [opts.fetchImpl]  injection point for tests (default: globalThis.fetch)
 * @param {number}   [opts.timeoutMs]  injection point for tests (default: GC_QUERY_TIMEOUT_MS)
 * @returns {Promise<{status: number, body: string, truncated: boolean, original_byte_length: number}>}
 */
export async function executeGcQuery(path, params, opts = {}) {
  const fetchImpl = opts.fetchImpl ?? globalThis.fetch;
  const timeoutMs = opts.timeoutMs ?? GC_QUERY_TIMEOUT_MS;

  // Path / params validation. We map validation errors into RequestError so
  // the calling MCP tool can pass them through the existing err() renderer.
  try {
    validateGcQueryPath(path);
    validateGcQueryParams(params);
  } catch (e) {
    if (e && e.name === "GcQueryValidationError") {
      throw new RequestError({
        status: 0,
        code: e.code,
        message: e.message,
        detail: null,
      });
    }
    throw e;
  }

  const url = buildUrl(path, params ?? undefined);
  const headers = {
    "X-Actor": "mcp-server",
    Accept: "application/json",
  };
  addAuthorizationHeader(path, headers);

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);

  // The timer must NOT be cleared until body consumption is finished —
  // otherwise a slow body read can stall arbitrarily long and the advertised
  // 30s cost cap wouldn't bite.
  try {
    let res;
    try {
      res = await fetchImpl(url, {
        method: "GET",
        headers,
        signal: controller.signal,
      });
    } catch (e) {
      if (e && (e.name === "AbortError" || e.code === "ABORT_ERR")) {
        throw new RequestError({
          status: 0,
          code: "gc_query_timeout",
          message: `gc_query timed out after ${timeoutMs}ms: ${path}`,
          detail: null,
        });
      }
      throw e;
    }

    if (res.status === 204) {
      return { status: 204, body: "", truncated: false, original_byte_length: 0 };
    }

    // Prefer streaming the body via res.body when present. Falls back to
    // res.text() for stub responses in tests that don't expose a stream.
    let bodyText;
    let streamHitCap = false;
    let streamedByteLength = 0;
    try {
      if (res.body && typeof res.body.getReader === "function") {
        const { buffer, hitCap } = await readBoundedBody(
          res.body,
          GC_QUERY_BODY_BYTE_CAP,
        );
        streamHitCap = hitCap;
        streamedByteLength = buffer.byteLength;
        bodyText = buffer.toString("utf8");
      } else {
        bodyText = await res.text();
      }
    } catch (e) {
      if (e && (e.name === "AbortError" || e.code === "ABORT_ERR")) {
        throw new RequestError({
          status: 0,
          code: "gc_query_timeout",
          message: `gc_query timed out after ${timeoutMs}ms while reading body: ${path}`,
          detail: null,
        });
      }
      throw e;
    }

    if (!res.ok) {
      const envelope = parseErrorBody(bodyText);
      throw new RequestError({
        status: res.status,
        code: envelope.code,
        message: envelope.message,
        detail: envelope.detail,
      });
    }

    // If the streaming reader hit the cap mid-body, the original byte length
    // is at least what we read; we can't know the true total without
    // continuing to read. Mark as truncated and report the bytes we got as
    // a lower bound.
    if (streamHitCap) {
      const marker = `\n\n... [gc_query response truncated at ~${GC_QUERY_BODY_BYTE_CAP} bytes (stream stopped to bound cost); original size unknown]`;
      // Trim the body to the cap to keep total response under the cap + marker.
      const buf = Buffer.from(bodyText, "utf8").subarray(0, GC_QUERY_BODY_BYTE_CAP);
      return {
        status: res.status,
        body: `${buf.toString("utf8")}${marker}`,
        truncated: true,
        original_byte_length: streamedByteLength,
      };
    }

    const trunc = truncateBody(bodyText);
    return {
      status: res.status,
      body: trunc.body,
      truncated: trunc.truncated,
      original_byte_length: trunc.original_byte_length,
    };
  } finally {
    clearTimeout(timer);
  }
}

/**
 * MCP tool handler entrypoint for `gc_query`. The MCP SDK normalizes the raw
 * Zod shape via `z.object(...)` which strips unknown keys instead of rejecting
 * them; this handler is where contract enforcement lives. Unknown keys
 * (`headers`, `method`, etc.) are explicitly rejected so silently-dropped
 * extras can't masquerade as accepted.
 *
 * @param {object} args
 * @param {object} [opts] passed through to executeGcQuery (test injection)
 */
export async function gcQueryToolHandler(args, opts = {}) {
  const allowedKeys = new Set(["path", "params"]);
  for (const k of Object.keys(args ?? {})) {
    if (!allowedKeys.has(k)) {
      throw new RequestError({
        status: 0,
        code: "invalid_query_args",
        message: `gc_query: unknown argument '${k}'. Only 'path' and 'params' are accepted.`,
        detail: null,
      });
    }
  }
  return executeGcQuery(args?.path, args?.params, opts);
}

/**
 * Strict Zod schema for `gc_query` MCP arguments. Registering a strict
 * ZodObject (rather than a raw shape) means the MCP SDK's input validation
 * rejects unknown keys (`headers`, `method`, etc.) at the protocol layer
 * before the handler runs. The `gcQueryToolHandler`'s key check below is
 * defense-in-depth in case a future SDK change relaxes this.
 */
export const gcQuerySchema = z
  .object({
    path: z
      .string()
      .describe(
        "Relative API path. Must start with one of the allowlisted /api/v1/* prefixes; absolute URLs, '..' segments, '?', and '#' are rejected.",
      ),
    params: z
      .record(z.union([z.string(), z.number(), z.boolean(), z.null()]))
      .optional()
      .describe(
        "Optional flat object of query parameters. Values must be string|number|boolean|null. undefined/null values are dropped.",
      ),
  })
  .strict();

// Re-export the cap so index.js can advertise it in tool descriptions.
export { GC_QUERY_BODY_BYTE_CAP, GC_QUERY_TIMEOUT_MS };
