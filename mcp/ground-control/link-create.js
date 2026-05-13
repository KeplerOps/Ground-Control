// Shared link_create surface for every consolidated MCP tool that exposes a
// link_create action (gc_asset, gc_threat_model, gc_risk_scenario, gc_control).
//
// All four backend link DTOs share an identical shape:
//   @NotNull TargetType targetType,
//   UUID targetEntityId,                       (optional on the wire)
//   @Size(max = 500) String targetIdentifier,  (optional on the wire)
//   @NotNull LinkType linkType,
//   @Size(max = 2000) String targetUrl,        (optional on the wire)
//   @Size(max = 255) String targetTitle        (optional on the wire)
//
// GraphTargetResolverService then enforces per-target-type semantics:
//   - internal target types (REQUIREMENT, ASSET, CONTROL, RISK_SCENARIO,
//     OBSERVATION, THREAT_MODEL, RISK_ASSESSMENT_RESULT, VERIFICATION_RESULT,
//     FINDING, etc.) → targetEntityId required
//   - external target types (EXTERNAL, EVIDENCE, ISSUE, CODE, AUDIT,
//     CONFIGURATION, OPERATIONAL_ARTIFACT, REMEDIATION_PLAN, VULNERABILITY,
//     AUDIT_RECORD, etc.)                          → targetIdentifier required
//
// The MCP layer does NOT mirror that dispatch (the preflight for #875 ruled out
// duplicating backend validators). It exposes both target_entity_id and
// target_identifier as optional and forwards whichever the caller supplies; the
// backend surfaces a stable DomainValidationException envelope when the caller
// picks the wrong one for the chosen target type.

import { z } from "zod";
import { pick, reqArg } from "./lib.js";

// Snake_case body fields forwarded to the backend on every link_create.
export const LINK_CREATE_BODY_FIELDS = [
  "target_type",
  "target_entity_id",
  "target_identifier",
  "link_type",
  "target_url",
  "target_title",
];

// MCP-side preconditions: target_type + link_type are @NotNull on every link
// DTO. target_entity_id vs target_identifier is the backend's choice.
export const LINK_CREATE_REQUIRED_FIELDS = ["target_type", "link_type"];

// Shared optional Zod field shapes. Each tool's Zod object spreads these in
// alongside its own action-discriminated fields; target_type / link_type stay
// tool-local because each consolidated tool has a different enum vocabulary.
export const linkCreateOptionalSharedZodFields = {
  target_entity_id: z.string().uuid().optional(),
  target_identifier: z.string().optional(),
  target_url: z.string().optional(),
  target_title: z.string().optional(),
};

/**
 * Run a consolidated tool's link_create action: validate parent-id +
 * target_type + link_type, then forward the link body fields to `createFn`.
 *
 * Centralizing this guarantees every link_create surface uses the same
 * allowlist and the same required-field semantics. A regression that drops
 * target_entity_id, target_url, or target_title from the wire — or skips the
 * target_type / link_type preconditions — fails this module's tests for every
 * caller at once.
 *
 * @param {object}  args            The Zod-parsed tool args.
 * @param {string}  parentIdField   Snake_case key of the parent entity id
 *                                  (asset_id, threat_model_id, scenario_id,
 *                                  control_id).
 * @param {function} createFn      lib.js createXLink(parentId, body, project).
 * @returns {Promise<any>}          The created link payload from the backend.
 */
export async function performLinkCreate(args, parentIdField, createFn) {
  reqArg(args, parentIdField, "link_create");
  for (const k of LINK_CREATE_REQUIRED_FIELDS) reqArg(args, k, "link_create");
  return createFn(args[parentIdField], pick(args, LINK_CREATE_BODY_FIELDS), args.project);
}
