// gc_finding: action-discriminated MCP adapter for the backend finding REST
// surface (GC-V001, ADR-038). Mirrors gc-threat-model.js — handler logic stays
// testable in isolation, while index.js registers the tool and wraps the
// return value in the MCP `ok()` envelope.

import { z } from "zod";
import {
  FINDING_TYPES, FINDING_SEVERITIES, FINDING_STATUSES,
  FINDING_LINK_TARGET_TYPES, FINDING_LINK_TYPES,
  createFinding, updateFinding, deleteFinding,
  transitionFindingStatus, createFindingLink, deleteFindingLink,
  pick, reqArg,
} from "./lib.js";
import {
  linkCreateOptionalSharedZodFields,
  performLinkCreate,
} from "./link-create.js";

export const GC_FINDING_ACTIONS = [
  "create", "update", "delete", "transition", "link_create", "link_delete",
];

// Snake_case body fields accepted by gc_finding.create — mirrors backend
// FindingRequest. uid is create-only; the update DTO has no uid.
export const GC_FINDING_CREATE_BODY_FIELDS = [
  "uid", "title", "finding_type", "severity", "description",
  "root_cause_analysis", "owner", "due_date",
];

// Snake_case body fields accepted by gc_finding.update — mirrors backend
// UpdateFindingRequest. clear_* flags are update-only.
export const GC_FINDING_UPDATE_BODY_FIELDS = [
  "title", "finding_type", "severity", "description",
  "root_cause_analysis", "owner", "due_date",
  "clear_root_cause_analysis", "clear_owner", "clear_due_date",
];

// @NotBlank / @NotNull fields on backend FindingRequest. Enforced at the
// adapter boundary so callers get a clear `'X' is required` error instead of
// waiting for a backend 422.
export const GC_FINDING_CREATE_REQUIRED_FIELDS = [
  "uid", "title", "finding_type", "severity", "description",
];

export const gcFindingZodShape = {
  action: z.enum(GC_FINDING_ACTIONS),
  id: z.string().uuid().optional(),
  uid: z.string().optional(),
  project: z.string().optional(),
  title: z.string().optional(),
  finding_type: z.enum(FINDING_TYPES).optional(),
  severity: z.enum(FINDING_SEVERITIES).optional(),
  description: z.string().optional(),
  root_cause_analysis: z.string().optional(),
  owner: z.string().optional(),
  due_date: z.string().optional(),
  clear_root_cause_analysis: z.boolean().optional(),
  clear_owner: z.boolean().optional(),
  clear_due_date: z.boolean().optional(),
  status: z.enum(FINDING_STATUSES).optional(),
  finding_id: z.string().uuid().optional(),
  target_type: z.enum(FINDING_LINK_TARGET_TYPES).optional(),
  link_type: z.enum(FINDING_LINK_TYPES).optional(),
  ...linkCreateOptionalSharedZodFields,
  link_id: z.string().uuid().optional(),
};

export const GC_FINDING_DESCRIPTION =
  `GRC findings + their links (audits, control-deficiencies, policy-violations, ` +
  `vulnerabilities, exception-escalations). Actions: ${GC_FINDING_ACTIONS.join(", ")}. ` +
  `create requires: ${GC_FINDING_CREATE_REQUIRED_FIELDS.join(", ")} (and accepts optional ` +
  `root_cause_analysis, owner, due_date). update accepts the same fields minus uid plus ` +
  `clear_root_cause_analysis / clear_owner / clear_due_date to explicitly null optional fields. ` +
  `status is consumed only by the transition action (creation defaults to OPEN). ` +
  `link_create requires target_type + link_type; for internal target types ` +
  `(CONTROL / RISK_SCENARIO / ASSET / OBSERVATION / AUDIT) pass target_entity_id, for external ` +
  `types (EVIDENCE / REMEDIATION_PLAN / OPERATIONAL_ARTIFACT / EXTERNAL) pass ` +
  `target_identifier. target_url and target_title are optional. ` +
  `Reads (list, get, links_list) route through gc_query.`;

/**
 * Pure adapter handler for gc_finding. Validates required fields, picks
 * action-scoped body fields, and dispatches to the corresponding lib.js call.
 * Returns the raw value the lib call produces (or null for delete-style 204s);
 * the index.js registration wraps the return in the MCP `ok()` envelope.
 */
export async function gcFindingToolHandler(args) {
  switch (args.action) {
    case "create": {
      for (const key of GC_FINDING_CREATE_REQUIRED_FIELDS) reqArg(args, key, "create");
      return createFinding(pick(args, GC_FINDING_CREATE_BODY_FIELDS), args.project);
    }
    case "update": {
      reqArg(args, "id", "update");
      return updateFinding(args.id, pick(args, GC_FINDING_UPDATE_BODY_FIELDS), args.project);
    }
    case "delete": {
      reqArg(args, "id", "delete");
      await deleteFinding(args.id, args.project);
      return null;
    }
    case "transition": {
      reqArg(args, "id", "transition");
      reqArg(args, "status", "transition");
      return transitionFindingStatus(args.id, args.status, args.project);
    }
    case "link_create": {
      return performLinkCreate(args, "finding_id", createFindingLink);
    }
    case "link_delete": {
      reqArg(args, "finding_id", "link_delete");
      reqArg(args, "link_id", "link_delete");
      await deleteFindingLink(args.finding_id, args.link_id, args.project);
      return null;
    }
    default:
      throw new Error(`Unknown action: ${args.action}`);
  }
}
