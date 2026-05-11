#!/usr/bin/env node
// Test fixture entrypoint for `mcp/ground-control/lib.js::buildPrBody`.
//
// Reads structured input as JSON on stdin and prints the rendered PR body to
// stdout. Used by `tools/tests/test_policy.py` to lock the renderer-vs-policy
// compose contract: the test calls this script with the same shape inputs the
// JS-side renderer tests use, captures stdout, and passes it through
// `check_pr_body`. If the JS renderer drops a section, changes a checklist
// line, or smuggles deferral language out via a caller-provided field, the
// Python policy gate rejects the output and the test fails — drift cannot
// escape both check layers simultaneously.
//
// Exit codes:
//   0  — rendered body written to stdout
//   2  — input was rejected by `runRenderPrBody` (validation, deferral, or
//        policy-shape failure); structured envelope written to stderr
//   3  — unexpected error (stack on stderr)

import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";

const __dirname = dirname(fileURLToPath(import.meta.url));
const libPath = resolve(__dirname, "..", "mcp", "ground-control", "lib.js");

const lib = await import(libPath);
const { runRenderPrBody } = lib;

async function main() {
  let raw;
  try {
    raw = readFileSync(0, "utf8"); // stdin
  } catch (e) {
    process.stderr.write(`fixture: stdin read failed: ${e.message}\n`);
    process.exit(3);
  }
  let input;
  try {
    input = JSON.parse(raw);
  } catch (e) {
    process.stderr.write(`fixture: stdin is not valid JSON: ${e.message}\n`);
    process.exit(3);
  }
  // Provide a default repoPath of the current working directory if the
  // caller didn't supply one; the renderer doesn't actually read the repo
  // (no git side effects), so this is just to satisfy the runner shape.
  if (input.repoPath == null) input.repoPath = process.cwd();
  let result;
  try {
    result = await runRenderPrBody(input);
  } catch (e) {
    process.stderr.write(`fixture: runRenderPrBody threw: ${e.message}\n`);
    process.exit(3);
  }
  if (!result.ok) {
    process.stderr.write(JSON.stringify(result, null, 2) + "\n");
    process.exit(2);
  }
  process.stdout.write(result.body);
}

main().catch((e) => {
  process.stderr.write(`fixture: unexpected failure: ${e.stack || e.message}\n`);
  process.exit(3);
});
