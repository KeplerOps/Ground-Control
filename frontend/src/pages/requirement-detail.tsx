import { RelationForm } from "@/components/relation-form";
import { RequirementForm } from "@/components/requirement-form";
import { StatusBadgeDropdown } from "@/components/status-badge";
import { TraceabilityForm } from "@/components/traceability-form";
import { PriorityBadge, StatusBadge, TypeBadge } from "@/components/ui/badge";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import {
  inputClass,
  primaryButton,
  secondaryButton,
} from "@/components/ui/form-field";
import { Modal } from "@/components/ui/modal";
import { useToast } from "@/components/ui/toast";
import { useImpact } from "@/hooks/use-analysis";
import {
  type TimelineFilters,
  useRequirementTimeline,
} from "@/hooks/use-history";
import {
  useCreateRelation,
  useDeleteRelation,
  useRelations,
} from "@/hooks/use-relations";
import {
  useArchiveRequirement,
  useCloneRequirement,
  useRequirement,
  useTransitionStatus,
  useUpdateRequirement,
} from "@/hooks/use-requirements";
import {
  useCreateTraceabilityLink,
  useDeleteTraceabilityLink,
  useTraceabilityLinks,
} from "@/hooks/use-traceability";
import type {
  ChangeCategory,
  FieldChangeResponse,
  RelationResponse,
  RequirementSummaryResponse,
  TimelineEntryResponse,
  TraceabilityLinkResponse,
  UpdateRequirementRequest,
} from "@/types/api";
import * as Tabs from "@radix-ui/react-tabs";
import {
  Archive,
  ArrowLeft,
  ChevronDown,
  ChevronRight,
  Copy,
  ExternalLink,
  FileText,
  GitBranch,
  Link2,
  Plus,
  Trash2,
} from "lucide-react";
import { useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

export function RequirementDetail() {
  const { id, projectId } = useParams<{ id: string; projectId: string }>();
  const navigate = useNavigate();
  const { toast } = useToast();
  const { data: req, isLoading } = useRequirement(id);

  const [editOpen, setEditOpen] = useState(false);
  const [cloneOpen, setCloneOpen] = useState(false);

  const safeId = id ?? "";
  const updateMutation = useUpdateRequirement(safeId);
  const transitionMutation = useTransitionStatus(safeId);
  const archiveMutation = useArchiveRequirement(safeId);

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="h-8 w-64 animate-pulse rounded bg-muted" />
        <div className="h-48 animate-pulse rounded-lg bg-muted" />
      </div>
    );
  }

  if (!req) {
    return (
      <div className="py-20 text-center text-muted-foreground">
        Requirement not found.
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <Link
            to={`/p/${projectId}/requirements`}
            className="mb-2 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
          >
            <ArrowLeft className="h-4 w-4" /> Back to Requirements
          </Link>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-semibold">{req.title}</h1>
            <span className="font-mono text-sm text-muted-foreground">
              {req.uid}
            </span>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            className={secondaryButton}
            onClick={() => setEditOpen(true)}
          >
            Edit
          </button>
          <button
            type="button"
            className={secondaryButton}
            onClick={() => setCloneOpen(true)}
          >
            <Copy className="mr-1 h-4 w-4" /> Clone
          </button>
          {req.status !== "ARCHIVED" && (
            <button
              type="button"
              className={secondaryButton}
              onClick={() =>
                archiveMutation.mutate(undefined, {
                  onSuccess: () =>
                    toast({
                      title: "Requirement archived",
                      variant: "success",
                    }),
                  onError: (err) =>
                    toast({
                      title: "Archive failed",
                      description: err.message,
                      variant: "error",
                    }),
                })
              }
            >
              <Archive className="mr-1 h-4 w-4" /> Archive
            </button>
          )}
        </div>
      </div>

      {/* Metadata cards */}
      <div className="flex flex-wrap items-center gap-3">
        <StatusBadgeDropdown
          status={req.status}
          onTransition={(s) =>
            transitionMutation.mutate(s, {
              onSuccess: () =>
                toast({ title: `Status changed to ${s}`, variant: "success" }),
              onError: (err) =>
                toast({
                  title: "Transition failed",
                  description: err.message,
                  variant: "error",
                }),
            })
          }
        />
        <PriorityBadge priority={req.priority} />
        <TypeBadge type={req.requirementType} />
        <span className="text-sm text-muted-foreground">Wave {req.wave}</span>
      </div>

      {/* Tabs */}
      <Tabs.Root defaultValue="details">
        <Tabs.List className="flex gap-1 border-b border-border">
          {["details", "relations", "traceability", "history", "impact"].map(
            (tab) => (
              <Tabs.Trigger
                key={tab}
                value={tab}
                className="border-b-2 border-transparent px-4 py-2 text-sm font-medium text-muted-foreground hover:text-foreground data-[state=active]:border-primary data-[state=active]:text-foreground"
              >
                {tab.charAt(0).toUpperCase() + tab.slice(1)}
              </Tabs.Trigger>
            ),
          )}
        </Tabs.List>

        <Tabs.Content value="details" className="pt-4">
          <DetailsTab req={req} />
        </Tabs.Content>

        <Tabs.Content value="relations" className="pt-4">
          <RelationsTab requirementId={req.id} />
        </Tabs.Content>

        <Tabs.Content value="traceability" className="pt-4">
          <TraceabilityTab requirementId={req.id} />
        </Tabs.Content>

        <Tabs.Content value="history" className="pt-4">
          <HistoryTab requirementId={req.id} />
        </Tabs.Content>

        <Tabs.Content value="impact" className="pt-4">
          <ImpactTab requirementId={req.id} />
        </Tabs.Content>
      </Tabs.Root>

      {/* Edit modal */}
      <Modal
        open={editOpen}
        onOpenChange={setEditOpen}
        title="Edit Requirement"
        className="max-w-2xl"
      >
        <RequirementForm
          mode="edit"
          initial={req}
          onSubmit={(data) =>
            updateMutation.mutate(data as UpdateRequirementRequest, {
              onSuccess: () => {
                setEditOpen(false);
                toast({ title: "Requirement updated", variant: "success" });
              },
              onError: (err) =>
                toast({
                  title: "Update failed",
                  description: err.message,
                  variant: "error",
                }),
            })
          }
          onCancel={() => setEditOpen(false)}
          loading={updateMutation.isPending}
        />
      </Modal>

      {/* Clone modal */}
      <CloneModal
        open={cloneOpen}
        onOpenChange={setCloneOpen}
        requirementId={req.id}
        onCloned={(newReq) =>
          navigate(`/p/${projectId}/requirements/${newReq.id}`)
        }
      />
    </div>
  );
}

function DetailsTab({
  req,
}: {
  req: {
    statement: string;
    rationale: string;
    createdAt: string;
    updatedAt: string;
    archivedAt: string | null;
  };
}) {
  return (
    <div className="space-y-4">
      {req.statement && (
        <div>
          <h3 className="mb-1 text-sm font-medium text-muted-foreground">
            Statement
          </h3>
          <p className="whitespace-pre-wrap text-sm">{req.statement}</p>
        </div>
      )}
      {req.rationale && (
        <div>
          <h3 className="mb-1 text-sm font-medium text-muted-foreground">
            Rationale
          </h3>
          <p className="whitespace-pre-wrap text-sm">{req.rationale}</p>
        </div>
      )}
      <div className="flex gap-6 text-xs text-muted-foreground pt-4 border-t border-border">
        <span>Created: {new Date(req.createdAt).toLocaleString()}</span>
        <span>Updated: {new Date(req.updatedAt).toLocaleString()}</span>
        {req.archivedAt && (
          <span>Archived: {new Date(req.archivedAt).toLocaleString()}</span>
        )}
      </div>
    </div>
  );
}

function RelationsTab({ requirementId }: { requirementId: string }) {
  const { toast } = useToast();
  const { data: relations = [], isLoading } = useRelations(requirementId);
  const createMutation = useCreateRelation(requirementId);
  const deleteMutation = useDeleteRelation(requirementId);
  const [addOpen, setAddOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);

  if (isLoading) return <div className="animate-pulse h-20 bg-muted rounded" />;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-medium">Relations ({relations.length})</h3>
        <button
          type="button"
          className={secondaryButton}
          onClick={() => setAddOpen(true)}
        >
          <Plus className="mr-1 h-4 w-4" /> Add Relation
        </button>
      </div>

      {relations.length > 0 ? (
        <div className="rounded-lg border border-border overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-card border-b border-border">
              <tr>
                <th className="px-3 py-2 text-left text-xs font-medium text-muted-foreground">
                  Direction
                </th>
                <th className="px-3 py-2 text-left text-xs font-medium text-muted-foreground">
                  Requirement
                </th>
                <th className="px-3 py-2 text-left text-xs font-medium text-muted-foreground">
                  Type
                </th>
                <th className="px-3 py-2 text-left text-xs font-medium text-muted-foreground">
                  Created
                </th>
                <th className="px-3 py-2 w-10" />
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {relations.map((rel) => (
                <RelationRow
                  key={rel.id}
                  rel={rel}
                  currentId={requirementId}
                  onDelete={() => setDeleteTarget(rel.id)}
                />
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p className="text-sm text-muted-foreground">No relations.</p>
      )}

      <Modal open={addOpen} onOpenChange={setAddOpen} title="Add Relation">
        <RelationForm
          sourceId={requirementId}
          onSubmit={(data) =>
            createMutation.mutate(data, {
              onSuccess: () => {
                setAddOpen(false);
                toast({ title: "Relation added", variant: "success" });
              },
              onError: (err) =>
                toast({
                  title: "Failed to add relation",
                  description: err.message,
                  variant: "error",
                }),
            })
          }
          onCancel={() => setAddOpen(false)}
          loading={createMutation.isPending}
        />
      </Modal>

      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null);
        }}
        title="Delete Relation"
        description="Are you sure you want to delete this relation?"
        onConfirm={() => {
          if (!deleteTarget) return;
          deleteMutation.mutate(deleteTarget, {
            onSuccess: () => {
              setDeleteTarget(null);
              toast({ title: "Relation deleted", variant: "success" });
            },
            onError: (err) =>
              toast({
                title: "Delete failed",
                description: err.message,
                variant: "error",
              }),
          });
        }}
        loading={deleteMutation.isPending}
      />
    </div>
  );
}

function RelationRow({
  rel,
  currentId,
  onDelete,
}: {
  rel: RelationResponse;
  currentId: string;
  onDelete: () => void;
}) {
  const { projectId } = useParams<{ projectId: string }>();
  const isSource = rel.sourceId === currentId;
  const otherUid = isSource ? rel.targetUid : rel.sourceUid;
  const otherId = isSource ? rel.targetId : rel.sourceId;

  return (
    <tr className="hover:bg-accent/30">
      <td className="px-3 py-2 text-xs">
        {isSource ? "Outgoing" : "Incoming"}
      </td>
      <td className="px-3 py-2">
        <Link
          to={`/p/${projectId}/requirements/${otherId}`}
          className="font-mono text-xs text-primary hover:underline"
        >
          {otherUid}
        </Link>
      </td>
      <td className="px-3 py-2 text-xs">
        {rel.relationType.replace(/_/g, " ")}
      </td>
      <td className="px-3 py-2 text-xs text-muted-foreground">
        {new Date(rel.createdAt).toLocaleDateString()}
      </td>
      <td className="px-3 py-2">
        <button
          type="button"
          className="rounded p-1 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
          onClick={onDelete}
        >
          <Trash2 className="h-3.5 w-3.5" />
        </button>
      </td>
    </tr>
  );
}

function TraceabilityTab({ requirementId }: { requirementId: string }) {
  const { toast } = useToast();
  const { data: links = [], isLoading } = useTraceabilityLinks(requirementId);
  const createMutation = useCreateTraceabilityLink(requirementId);
  const deleteMutation = useDeleteTraceabilityLink(requirementId);
  const [addOpen, setAddOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);

  if (isLoading) return <div className="animate-pulse h-20 bg-muted rounded" />;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-medium">
          Traceability Links ({links.length})
        </h3>
        <button
          type="button"
          className={secondaryButton}
          onClick={() => setAddOpen(true)}
        >
          <Plus className="mr-1 h-4 w-4" /> Add Link
        </button>
      </div>

      {links.length > 0 ? (
        <div className="rounded-lg border border-border overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-card border-b border-border">
              <tr>
                <th className="px-3 py-2 text-left text-xs font-medium text-muted-foreground">
                  Artifact
                </th>
                <th className="px-3 py-2 text-left text-xs font-medium text-muted-foreground">
                  Identifier
                </th>
                <th className="px-3 py-2 text-left text-xs font-medium text-muted-foreground">
                  Link Type
                </th>
                <th className="px-3 py-2 text-left text-xs font-medium text-muted-foreground">
                  Sync
                </th>
                <th className="px-3 py-2 text-left text-xs font-medium text-muted-foreground">
                  Created
                </th>
                <th className="px-3 py-2 w-10" />
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {links.map((link) => (
                <TraceabilityRow
                  key={link.id}
                  link={link}
                  onDelete={() => setDeleteTarget(link.id)}
                />
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p className="text-sm text-muted-foreground">No traceability links.</p>
      )}

      <Modal
        open={addOpen}
        onOpenChange={setAddOpen}
        title="Add Traceability Link"
      >
        <TraceabilityForm
          onSubmit={(data) =>
            createMutation.mutate(data, {
              onSuccess: () => {
                setAddOpen(false);
                toast({ title: "Link added", variant: "success" });
              },
              onError: (err) =>
                toast({
                  title: "Failed to add link",
                  description: err.message,
                  variant: "error",
                }),
            })
          }
          onCancel={() => setAddOpen(false)}
          loading={createMutation.isPending}
        />
      </Modal>

      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null);
        }}
        title="Delete Traceability Link"
        description="Are you sure you want to delete this traceability link?"
        onConfirm={() => {
          if (!deleteTarget) return;
          deleteMutation.mutate(deleteTarget, {
            onSuccess: () => {
              setDeleteTarget(null);
              toast({ title: "Link deleted", variant: "success" });
            },
            onError: (err) =>
              toast({
                title: "Delete failed",
                description: err.message,
                variant: "error",
              }),
          });
        }}
        loading={deleteMutation.isPending}
      />
    </div>
  );
}

function TraceabilityRow({
  link,
  onDelete,
}: {
  link: TraceabilityLinkResponse;
  onDelete: () => void;
}) {
  return (
    <tr className="hover:bg-accent/30">
      <td className="px-3 py-2 text-xs">
        {link.artifactType.replace(/_/g, " ")}
      </td>
      <td className="px-3 py-2">
        {link.artifactUrl &&
        (link.artifactUrl.startsWith("https://") ||
          link.artifactUrl.startsWith("http://")) ? (
          <a
            href={link.artifactUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1 text-xs text-primary hover:underline"
          >
            {link.artifactTitle || link.artifactIdentifier}
            <ExternalLink className="h-3 w-3" />
          </a>
        ) : (
          <span className="text-xs">
            {link.artifactTitle || link.artifactIdentifier}
          </span>
        )}
      </td>
      <td className="px-3 py-2 text-xs">{link.linkType.replace(/_/g, " ")}</td>
      <td className="px-3 py-2 text-xs text-muted-foreground">
        {link.syncStatus}
      </td>
      <td className="px-3 py-2 text-xs text-muted-foreground">
        {new Date(link.createdAt).toLocaleDateString()}
      </td>
      <td className="px-3 py-2">
        <button
          type="button"
          className="rounded p-1 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
          onClick={onDelete}
        >
          <Trash2 className="h-3.5 w-3.5" />
        </button>
      </td>
    </tr>
  );
}

const CATEGORY_LABELS: Record<ChangeCategory, string> = {
  REQUIREMENT: "Requirement",
  RELATION: "Relation",
  TRACEABILITY_LINK: "Traceability",
};

const CATEGORY_ICONS: Record<ChangeCategory, typeof FileText> = {
  REQUIREMENT: FileText,
  RELATION: GitBranch,
  TRACEABILITY_LINK: Link2,
};

const REVISION_COLORS: Record<string, string> = {
  ADD: "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200",
  MOD: "bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200",
  DEL: "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200",
};

const DOT_COLORS: Record<string, string> = {
  ADD: "bg-green-500",
  MOD: "bg-blue-500",
  DEL: "bg-red-500",
};

function formatFieldName(key: string): string {
  return key
    .replace(/([A-Z])/g, " $1")
    .replace(/^./, (s) => s.toUpperCase())
    .trim();
}

function formatFieldValue(value: unknown): string {
  if (value === null || value === undefined) return "(empty)";
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}

function FieldDiff({
  changes,
}: {
  changes: Record<string, FieldChangeResponse>;
}) {
  return (
    <div className="mt-2 space-y-1.5">
      {Object.entries(changes).map(([key, change]) => (
        <div key={key} className="text-xs">
          <span className="font-medium text-foreground">
            {formatFieldName(key)}:
          </span>
          <div className="ml-4 flex flex-col gap-0.5">
            <span className="text-red-600 dark:text-red-400 line-through">
              {formatFieldValue(change.oldValue)}
            </span>
            <span className="text-green-600 dark:text-green-400">
              {formatFieldValue(change.newValue)}
            </span>
          </div>
        </div>
      ))}
    </div>
  );
}

function TimelineEntryCard({ entry }: { entry: TimelineEntryResponse }) {
  const [expanded, setExpanded] = useState(false);
  const Icon = CATEGORY_ICONS[entry.changeCategory];
  const hasChanges = entry.changes && Object.keys(entry.changes).length > 0;

  return (
    <div className="relative pl-8">
      {/* Timeline dot */}
      <div
        className={`absolute left-0 top-2 w-3 h-3 rounded-full border-2 border-background ${DOT_COLORS[entry.revisionType] ?? "bg-gray-400"}`}
      />

      <div className="rounded-lg border border-border bg-card p-3">
        {/* Header */}
        <div className="flex items-center gap-2 flex-wrap">
          <Icon className="h-3.5 w-3.5 text-muted-foreground" />
          <span className="text-xs font-medium text-muted-foreground">
            {CATEGORY_LABELS[entry.changeCategory]}
          </span>
          <span
            className={`text-xs font-medium px-1.5 py-0.5 rounded ${REVISION_COLORS[entry.revisionType] ?? ""}`}
          >
            {entry.revisionType}
          </span>
          <span className="text-xs text-muted-foreground ml-auto">
            {new Date(entry.timestamp).toLocaleString()}
          </span>
          {entry.actor && (
            <span className="text-xs text-muted-foreground">
              by {entry.actor}
            </span>
          )}
        </div>

        {/* Snapshot summary */}
        {entry.snapshot && (
          <div className="mt-1.5 text-xs text-muted-foreground">
            {entry.changeCategory === "REQUIREMENT" && (
              <span>{entry.snapshot.title as string}</span>
            )}
            {entry.changeCategory === "RELATION" && (
              <span>{entry.snapshot.relationType as string} relation</span>
            )}
            {entry.changeCategory === "TRACEABILITY_LINK" && (
              <span>
                {entry.snapshot.linkType as string}{" "}
                {entry.snapshot.artifactType as string}:{" "}
                {entry.snapshot.artifactIdentifier as string}
              </span>
            )}
          </div>
        )}

        {/* Expandable diff */}
        {hasChanges && (
          <>
            <button
              type="button"
              onClick={() => setExpanded(!expanded)}
              className="mt-2 flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors"
            >
              {expanded ? (
                <ChevronDown className="h-3 w-3" />
              ) : (
                <ChevronRight className="h-3 w-3" />
              )}
              {Object.keys(entry.changes).length} field(s) changed
            </button>
            {expanded && <FieldDiff changes={entry.changes} />}
          </>
        )}
      </div>
    </div>
  );
}

function HistoryTab({ requirementId }: { requirementId: string }) {
  const [filters, setFilters] = useState<TimelineFilters>({});
  const { data: timeline = [], isLoading } = useRequirementTimeline(
    requirementId,
    filters,
  );

  const activeCategories = useMemo(() => {
    const cats = new Set<ChangeCategory>();
    for (const e of timeline) {
      cats.add(e.changeCategory);
    }
    return cats;
  }, [timeline]);

  if (isLoading) return <div className="animate-pulse h-20 bg-muted rounded" />;

  return (
    <div className="space-y-4">
      {/* Filter bar */}
      <div className="flex flex-wrap items-center gap-4 rounded-lg border border-border bg-card p-3">
        <span className="text-xs font-medium text-muted-foreground">
          Filter:
        </span>
        <div className="flex items-center gap-3">
          {(
            ["REQUIREMENT", "RELATION", "TRACEABILITY_LINK"] as ChangeCategory[]
          ).map((cat) => (
            <label key={cat} className="flex items-center gap-1.5 text-xs">
              <input
                type="radio"
                name="changeCategory"
                checked={filters.changeCategory === cat}
                onChange={() =>
                  setFilters((f) => ({
                    ...f,
                    changeCategory: f.changeCategory === cat ? undefined : cat,
                  }))
                }
                className="accent-primary"
              />
              {CATEGORY_LABELS[cat]}
            </label>
          ))}
          {filters.changeCategory && (
            <button
              type="button"
              onClick={() =>
                setFilters((f) => ({ ...f, changeCategory: undefined }))
              }
              className="text-xs text-muted-foreground hover:text-foreground"
            >
              Clear
            </button>
          )}
        </div>
        <div className="flex items-center gap-2 ml-auto">
          <label className="text-xs text-muted-foreground">From</label>
          <input
            type="date"
            value={
              filters.from
                ? new Date(filters.from).toISOString().split("T")[0]
                : ""
            }
            onChange={(e) =>
              setFilters((f) => ({
                ...f,
                from: e.target.value
                  ? new Date(e.target.value).toISOString()
                  : undefined,
              }))
            }
            className={`${inputClass} text-xs w-32`}
          />
          <label className="text-xs text-muted-foreground">To</label>
          <input
            type="date"
            value={
              filters.to ? new Date(filters.to).toISOString().split("T")[0] : ""
            }
            onChange={(e) =>
              setFilters((f) => ({
                ...f,
                to: e.target.value
                  ? new Date(`${e.target.value}T23:59:59.999Z`).toISOString()
                  : undefined,
              }))
            }
            className={`${inputClass} text-xs w-32`}
          />
        </div>
      </div>

      {/* Timeline */}
      {timeline.length === 0 ? (
        <p className="text-sm text-muted-foreground">No history entries.</p>
      ) : (
        <div className="relative">
          {/* Vertical line */}
          <div className="absolute left-[5px] top-2 bottom-2 w-0.5 bg-border" />
          <div className="space-y-3">
            {timeline.map((entry, idx) => (
              <TimelineEntryCard
                key={`${entry.changeCategory}-${entry.entityId}-${entry.revisionNumber}-${idx}`}
                entry={entry}
              />
            ))}
          </div>
        </div>
      )}

      {/* Legend */}
      {timeline.length > 0 && (
        <div className="flex items-center gap-4 text-xs text-muted-foreground pt-2 border-t border-border">
          <span>Legend:</span>
          {activeCategories.has("REQUIREMENT") && (
            <span className="flex items-center gap-1">
              <FileText className="h-3 w-3" /> Requirement
            </span>
          )}
          {activeCategories.has("RELATION") && (
            <span className="flex items-center gap-1">
              <GitBranch className="h-3 w-3" /> Relation
            </span>
          )}
          {activeCategories.has("TRACEABILITY_LINK") && (
            <span className="flex items-center gap-1">
              <Link2 className="h-3 w-3" /> Traceability
            </span>
          )}
          <span className="ml-4 flex items-center gap-1">
            <span className="w-2 h-2 rounded-full bg-green-500" /> Added
          </span>
          <span className="flex items-center gap-1">
            <span className="w-2 h-2 rounded-full bg-blue-500" /> Modified
          </span>
          <span className="flex items-center gap-1">
            <span className="w-2 h-2 rounded-full bg-red-500" /> Deleted
          </span>
        </div>
      )}
    </div>
  );
}

function ImpactTab({ requirementId }: { requirementId: string }) {
  const { projectId } = useParams<{ projectId: string }>();
  const { data: impacted = [], isLoading } = useImpact(requirementId);

  if (isLoading) return <div className="animate-pulse h-20 bg-muted rounded" />;

  if (impacted.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        No transitively impacted requirements.
      </p>
    );
  }

  return (
    <div className="space-y-2">
      <p className="text-sm text-muted-foreground mb-3">
        {impacted.length} requirement(s) would be impacted by changes to this
        requirement.
      </p>
      {impacted.map((r: RequirementSummaryResponse) => (
        <Link
          key={r.id}
          to={`/p/${projectId}/requirements/${r.id}`}
          className="flex items-center gap-3 rounded-lg border border-border bg-card p-3 hover:bg-accent/30"
        >
          <span className="font-mono text-xs text-muted-foreground">
            {r.uid}
          </span>
          <span className="text-sm font-medium">{r.title}</span>
          <StatusBadge status={r.status} />
          <span className="ml-auto text-xs text-muted-foreground">
            Wave {r.wave}
          </span>
        </Link>
      ))}
    </div>
  );
}

function CloneModal({
  open,
  onOpenChange,
  requirementId,
  onCloned,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  requirementId: string;
  onCloned: (req: { id: string }) => void;
}) {
  const { toast } = useToast();
  const cloneMutation = useCloneRequirement(requirementId);
  const [newUid, setNewUid] = useState("");
  const [copyRelations, setCopyRelations] = useState(false);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    cloneMutation.mutate(
      { newUid, copyRelations },
      {
        onSuccess: (data) => {
          onOpenChange(false);
          toast({ title: "Requirement cloned", variant: "success" });
          onCloned(data);
        },
        onError: (err) =>
          toast({
            title: "Clone failed",
            description: err.message,
            variant: "error",
          }),
      },
    );
  }

  return (
    <Modal open={open} onOpenChange={onOpenChange} title="Clone Requirement">
      <form onSubmit={handleSubmit} className="space-y-4">
        <label className="block space-y-1.5">
          <span className="text-sm font-medium">New UID</span>
          <input
            className={inputClass}
            value={newUid}
            onChange={(e) => setNewUid(e.target.value)}
            placeholder="GC-A002"
            required
          />
        </label>
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={copyRelations}
            onChange={(e) => setCopyRelations(e.target.checked)}
            className="rounded"
          />
          Copy relations
        </label>
        <div className="flex justify-end gap-3 pt-2">
          <button
            type="button"
            className={secondaryButton}
            onClick={() => onOpenChange(false)}
          >
            Cancel
          </button>
          <button
            type="submit"
            className={primaryButton}
            disabled={cloneMutation.isPending}
          >
            {cloneMutation.isPending ? "Cloning..." : "Clone"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
