// gc_audit: action-discriminated MCP adapter for the backend audit REST
// surface (GC-U001, ADR-047). Mirrors gc-finding.js — handler logic stays
// testable in isolation, while index.js registers the tool and wraps the
// return value in the MCP `ok()` envelope.

import { z } from "zod";
import {
  AUDIT_TYPES, AUDIT_STATUSES, AUDIT_PHASE_KINDS,
  AUDIT_LINK_TARGET_TYPES, AUDIT_LINK_TYPES,
  createAudit, updateAudit, deleteAudit,
  transitionAuditStatus, createAuditLink, deleteAuditLink,
  pick, reqArg,
} from "./lib.js";
import {
  linkCreateOptionalSharedZodFields,
  performLinkCreate,
} from "./link-create.js";

export const GC_AUDIT_ACTIONS = [
  "create", "update", "delete", "transition", "link_create", "link_delete",
];

// Snake_case body fields accepted by gc_audit.create — mirrors backend AuditRequest.
export const GC_AUDIT_CREATE_BODY_FIELDS = [
  "uid", "title", "audit_type", "scope_description",
  "objectives", "phases", "team_members",
];

// Snake_case body fields accepted by gc_audit.update — mirrors backend UpdateAuditRequest.
export const GC_AUDIT_UPDATE_BODY_FIELDS = [
  "title", "audit_type", "scope_description",
  "objectives", "phases", "team_members",
  "clear_objectives", "clear_phases", "clear_team_members",
];

// @NotBlank / @NotNull fields on backend AuditRequest.
export const GC_AUDIT_CREATE_REQUIRED_FIELDS = [
  "uid", "title", "audit_type", "scope_description",
];

export const gcAuditZodShape = {
  action: z.enum(GC_AUDIT_ACTIONS),
  id: z.string().uuid().optional(),
  uid: z.string().optional(),
  project: z.string().optional(),
  title: z.string().optional(),
  audit_type: z.enum(AUDIT_TYPES).optional(),
  scope_description: z.string().optional(),
  objectives: z.array(z.string()).optional(),
  phases: z.array(z.object({
    kind: z.enum(AUDIT_PHASE_KINDS),
    planned_start: z.string().optional(),
    planned_end: z.string().optional(),
    actual_start: z.string().optional(),
    actual_end: z.string().optional(),
  })).optional(),
  team_members: z.array(z.string()).optional(),
  clear_objectives: z.boolean().optional(),
  clear_phases: z.boolean().optional(),
  clear_team_members: z.boolean().optional(),
  status: z.enum(AUDIT_STATUSES).optional(),
  audit_id: z.string().uuid().optional(),
  target_type: z.enum(AUDIT_LINK_TARGET_TYPES).optional(),
  link_type: z.enum(AUDIT_LINK_TYPES).optional(),
  ...linkCreateOptionalSharedZodFields,
  link_id: z.string().uuid().optional(),
};

export const GC_AUDIT_DESCRIPTION =
  `GRC audits and their links (compliance frameworks, assets, controls, risk records, evidence, findings). ` +
  `Actions: ${GC_AUDIT_ACTIONS.join(", ")}. ` +
  `create requires: ${GC_AUDIT_CREATE_REQUIRED_FIELDS.join(", ")} (and accepts optional ` +
  `objectives, phases, team_members). update accepts the same fields minus uid plus ` +
  `clear_objectives / clear_phases / clear_team_members to explicitly null list fields. ` +
  `status is consumed only by the transition action (creation defaults to PLANNED). ` +
  `link_create requires target_type + link_type; for internal target types ` +
  `(ASSET / CONTROL / RISK_SCENARIO / RISK_REGISTER_RECORD / EVIDENCE / FINDING) pass target_entity_id, ` +
  `for external types (FRAMEWORK / EXTERNAL) pass target_identifier. ` +
  `target_url and target_title are optional. ` +
  `Reads (list, get, links_list) route through gc_query.`;

/**
 * Pure adapter handler for gc_audit. Validates required fields, picks
 * action-scoped body fields, and dispatches to the corresponding lib.js call.
 * Returns the raw value the lib call produces (or null for delete-style 204s);
 * the index.js registration wraps the return in the MCP `ok()` envelope.
 */
export async function gcAuditToolHandler(args) {
  switch (args.action) {
    case "create": {
      for (const key of GC_AUDIT_CREATE_REQUIRED_FIELDS) reqArg(args, key, "create");
      return createAudit(pick(args, GC_AUDIT_CREATE_BODY_FIELDS), args.project);
    }
    case "update": {
      reqArg(args, "id", "update");
      return updateAudit(args.id, pick(args, GC_AUDIT_UPDATE_BODY_FIELDS), args.project);
    }
    case "delete": {
      reqArg(args, "id", "delete");
      await deleteAudit(args.id, args.project);
      return null;
    }
    case "transition": {
      reqArg(args, "id", "transition");
      reqArg(args, "status", "transition");
      return transitionAuditStatus(args.id, args.status, args.project);
    }
    case "link_create": {
      return performLinkCreate(args, "audit_id", createAuditLink);
    }
    case "link_delete": {
      reqArg(args, "audit_id", "link_delete");
      reqArg(args, "link_id", "link_delete");
      await deleteAuditLink(args.audit_id, args.link_id, args.project);
      return null;
    }
    default:
      throw new Error(`Unknown action: ${args.action}`);
  }
}
