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
import { useRequirementHistory } from "@/hooks/use-history";
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
  RelationResponse,
  RequirementSummaryResponse,
  TraceabilityLinkResponse,
  UpdateRequirementRequest,
} from "@/types/api";
import * as Tabs from "@radix-ui/react-tabs";
import {
  Archive,
  ArrowLeft,
  Copy,
  ExternalLink,
  Plus,
  Trash2,
} from "lucide-react";
import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

export function RequirementDetail() {
  const { id } = useParams<{ id: string }>();
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
            to="/requirements"
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
        onCloned={(newReq) => navigate(`/requirements/${newReq.id}`)}
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
          to={`/requirements/${otherId}`}
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
        {link.artifactUrl ? (
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

function HistoryTab({ requirementId }: { requirementId: string }) {
  const { data: history = [], isLoading } =
    useRequirementHistory(requirementId);

  if (isLoading) return <div className="animate-pulse h-20 bg-muted rounded" />;

  if (history.length === 0) {
    return <p className="text-sm text-muted-foreground">No history.</p>;
  }

  return (
    <div className="space-y-3">
      {history.map((entry) => (
        <div
          key={entry.revisionNumber}
          className="rounded-lg border border-border bg-card p-4"
        >
          <div className="flex items-center gap-3 mb-2">
            <span className="text-xs font-medium bg-accent px-2 py-0.5 rounded">
              Rev {entry.revisionNumber}
            </span>
            <span className="text-xs font-medium">{entry.revisionType}</span>
            <span className="text-xs text-muted-foreground ml-auto">
              {new Date(entry.timestamp).toLocaleString()}
            </span>
            {entry.actor && (
              <span className="text-xs text-muted-foreground">
                by {entry.actor}
              </span>
            )}
          </div>
          {entry.snapshot && (
            <div className="text-xs text-muted-foreground space-y-1">
              <div className="flex gap-2">
                <StatusBadge status={entry.snapshot.status} />
                <PriorityBadge priority={entry.snapshot.priority} />
                <TypeBadge type={entry.snapshot.requirementType} />
              </div>
              <p className="mt-1">{entry.snapshot.title}</p>
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

function ImpactTab({ requirementId }: { requirementId: string }) {
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
          to={`/requirements/${r.id}`}
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
