import process from "node:process";

import { evaluateQualityGates, runSweep } from "../../mcp/ground-control/lib.js";
import { gateActualValue, gateName, readGateField, readJson, summarizeSweep } from "./common.mjs";

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

if (regressions.length > 0) {
  console.error("Ground Control sweep regressed past the recorded baseline:");
  for (const item of regressions) {
    console.error(`- ${item}`);
  }
  process.exit(1);
}

console.log("Ground Control live policy checks passed.");
