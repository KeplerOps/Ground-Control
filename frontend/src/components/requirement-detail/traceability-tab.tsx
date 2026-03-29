import { TraceabilityForm } from "@/components/traceability-form";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { secondaryButton } from "@/components/ui/form-field";
import { Modal } from "@/components/ui/modal";
import { useToast } from "@/components/ui/toast";
import {
  useCreateTraceabilityLink,
  useDeleteTraceabilityLink,
  useTraceabilityLinks,
} from "@/hooks/use-traceability";
import type { TraceabilityLinkResponse } from "@/types/api";
import { ExternalLink, Plus, Trash2 } from "lucide-react";
import { useState } from "react";

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

export function TraceabilityTab({ requirementId }: { requirementId: string }) {
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
