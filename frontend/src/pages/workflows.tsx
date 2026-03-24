import { StatusBadge } from "@/components/status-badge";
import { Modal } from "@/components/ui/modal";
import { useWorkspace } from "@/contexts/workspace-context";
import {
  useCreateWorkflow,
  useDeleteWorkflow,
  useWorkflows,
} from "@/hooks/use-workflows";
import { cn } from "@/lib/utils";
import { Plus, Search, Trash2, Workflow } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

export function Workflows() {
  const { workspace } = useWorkspace();
  const navigate = useNavigate();
  const { data: page, isLoading } = useWorkflows(workspace?.identifier);
  const createWorkflow = useCreateWorkflow();
  const deleteWorkflow = useDeleteWorkflow();
  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [search, setSearch] = useState("");

  const workflows = page?.content ?? [];
  const filtered = search
    ? workflows.filter(
        (w) =>
          w.name.toLowerCase().includes(search.toLowerCase()) ||
          w.tags?.toLowerCase().includes(search.toLowerCase()),
      )
    : workflows;

  function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim()) return;
    createWorkflow.mutate(
      {
        name: name.trim(),
        description: description.trim(),
        workspace: workspace?.identifier,
      },
      {
        onSuccess: (wf) => {
          setShowCreate(false);
          setName("");
          setDescription("");
          navigate(`/w/${workspace?.identifier}/workflows/${wf.id}`);
        },
      },
    );
  }

  function handleDelete(e: React.MouseEvent, id: string) {
    e.stopPropagation();
    if (confirm("Delete this workflow? This cannot be undone.")) {
      deleteWorkflow.mutate(id);
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Workflows</h1>
        <button
          type="button"
          onClick={() => setShowCreate(true)}
          className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="h-4 w-4" />
          New Workflow
        </button>
      </div>

      <div className="relative">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <input
          type="text"
          placeholder="Search workflows..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full rounded-md border border-border bg-card py-2 pl-9 pr-3 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary"
        />
      </div>

      {isLoading ? (
        <div className="space-y-2">
          {["s1", "s2", "s3", "s4", "s5"].map((k) => (
            <div
              key={k}
              className="h-16 animate-pulse rounded-lg border border-border bg-card"
            />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="rounded-lg border border-border bg-card p-12 text-center">
          <Workflow className="mx-auto h-10 w-10 text-muted-foreground" />
          <p className="mt-3 text-muted-foreground">
            {search
              ? "No workflows match your search."
              : "No workflows yet. Create one to get started."}
          </p>
        </div>
      ) : (
        <div className="space-y-1">
          {filtered.map((wf) => (
            <button
              key={wf.id}
              type="button"
              className="flex w-full items-center gap-4 rounded-lg border border-border bg-card px-4 py-3 text-left transition-colors hover:bg-accent/30"
              onClick={() =>
                navigate(`/w/${workspace?.identifier}/workflows/${wf.id}`)
              }
            >
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium">{wf.name}</p>
                {wf.description && (
                  <p className="mt-0.5 truncate text-xs text-muted-foreground">
                    {wf.description}
                  </p>
                )}
              </div>
              <StatusBadge status={wf.status} />
              <span className="shrink-0 text-xs text-muted-foreground">
                v{wf.currentVersion}
              </span>
              {wf.tags && (
                <span className="shrink-0 truncate text-xs text-muted-foreground max-w-32">
                  {wf.tags}
                </span>
              )}
              <span className="shrink-0 text-xs text-muted-foreground">
                {new Date(wf.updatedAt).toLocaleDateString()}
              </span>
              <button
                type="button"
                onClick={(e) => handleDelete(e, wf.id)}
                className="shrink-0 rounded p-1 text-muted-foreground hover:bg-destructive/20 hover:text-destructive"
                title="Delete workflow"
              >
                <Trash2 className="h-4 w-4" />
              </button>
            </button>
          ))}
        </div>
      )}

      {page && page.totalPages > 1 && (
        <p className="text-center text-sm text-muted-foreground">
          Showing {filtered.length} of {page.totalElements} workflows
        </p>
      )}

      <Modal
        open={showCreate}
        onOpenChange={setShowCreate}
        title="Create Workflow"
        description="Add a new workflow to this workspace."
      >
        <form onSubmit={handleCreate} className="space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium">Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Deploy Pipeline"
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              required
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">
              Description
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Optional description..."
              rows={3}
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setShowCreate(false)}
              className="rounded-md border border-border px-3 py-1.5 text-sm hover:bg-accent/50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={createWorkflow.isPending || !name.trim()}
              className={cn(
                "rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90",
                createWorkflow.isPending && "opacity-50",
              )}
            >
              {createWorkflow.isPending ? "Creating..." : "Create"}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
