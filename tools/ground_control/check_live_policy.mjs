import { execFileSync } from "node:child_process";
import process from "node:process";

import { evaluateQualityGates, getTraceabilityByArtifact, runSweep } from "../../mcp/ground-control/lib.js";
import { gateActualValue, gateName, readGateField, readJson, repoRoot, summarizeSweep } from "./common.mjs";

const config = await readJson("tools/ground_control/policy.json");
const baselineConfig = await readJson("tools/ground_control/sweep-baseline.json");
const evaluation = await evaluateQualityGates(config.project);

if (!evaluation.passed) {
  console.error("Ground Control quality gates failed:");
  for (const gate of evaluation.gates.filter((item) => !readGateField(item, "passed"))) {
    console.error(
      `- ${gateName(gate)}: actual=${gateActualValue(gate)}, expected ${readGateField(gate, "operator")} ${readGateField(gate, "threshold")}`
    );
  }
  process.exit(1);
}

const report = await runSweep(config.project);
const current = summarizeSweep(report);
const baseline = baselineConfig.baseline;
const regressions = [];

for (const key of [
  "cycles",
  "orphans",
  "crossWaveViolations",
  "consistencyViolations",
  "completenessIssues",
  "qualityGateFailures",
]) {
  if (current[key] > baseline[key]) {
    regressions.push(`${key} regressed: baseline=${baseline[key]} current=${current[key]}`);
  }
}

for (const [linkType, count] of Object.entries(current.coverageGaps)) {
  const allowed = baseline.coverageGaps[linkType] ?? 0;
  if (count > allowed) {
    regressions.push(`coverage gap regressed for ${linkType}: baseline=${allowed} current=${count}`);
  }
}

// ---------------------------------------------------------------------------
// Reverse traceability: verify substantive files are linked to requirements
// ---------------------------------------------------------------------------

const TRACKED_PATTERNS = [
  /^backend\/src\/main\/java\/.*\.java$/,
  /^mcp\/ground-control\/(lib|index)\.js$/,
  /^tools\/policy\/checks\.py$/,
  /^tools\/ground_control\/.*\.mjs$/,
];

try {
  const allFiles = execFileSync("git", ["ls-files"], { cwd: repoRoot, encoding: "utf8" })
    .split("\n")
    .filter(Boolean);
  const substantiveFiles = allFiles.filter((f) => TRACKED_PATTERNS.some((p) => p.test(f)));

  let untracedCount = 0;
  for (const file of substantiveFiles) {
    try {
      const links = await getTraceabilityByArtifact("CODE_FILE", file);
      if (!Array.isArray(links) || links.length === 0) {
        untracedCount += 1;
      }
    } catch {
      untracedCount += 1;
    }
  }

  const untracedBaseline = baseline.untracedFiles;
  if (untracedBaseline !== undefined && untracedCount > untracedBaseline) {
    regressions.push(`untraced files regressed: baseline=${untracedBaseline} current=${untracedCount}`);
  }

  console.log(
    `Reverse traceability: ${substantiveFiles.length - untracedCount}/${substantiveFiles.length} files traced.`,
  );
} catch (e) {
  console.warn(`Reverse traceability check skipped: ${e.message}`);
}

if (regressions.length > 0) {
  console.error("Ground Control sweep regressed past the recorded baseline:");
  for (const item of regressions) {
    console.error(`- ${item}`);
  }
  process.exit(1);
}

console.log("Ground Control live policy checks passed.");
