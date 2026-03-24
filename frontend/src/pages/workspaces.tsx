import { Modal } from "@/components/ui/modal";
import { useCreateWorkspace, useWorkspaces } from "@/hooks/use-workspaces";
import { cn } from "@/lib/utils";
import { Boxes, Plus } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

export function Workspaces() {
  const navigate = useNavigate();
  const { data: workspaces = [], isLoading } = useWorkspaces();
  const createWorkspace = useCreateWorkspace();
  const [showCreate, setShowCreate] = useState(false);
  const [identifier, setIdentifier] = useState("");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");

  function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!identifier.trim() || !name.trim()) return;
    createWorkspace.mutate(
      {
        identifier: identifier.trim(),
        name: name.trim(),
        description: description.trim() || undefined,
      },
      {
        onSuccess: (ws) => {
          setShowCreate(false);
          setIdentifier("");
          setName("");
          setDescription("");
          navigate(`/w/${ws.identifier}/`);
        },
      },
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Workspaces</h1>
        <button
          type="button"
          onClick={() => setShowCreate(true)}
          className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="h-4 w-4" />
          New Workspace
        </button>
      </div>

      {isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 3 }, (_, i) => (
            <div key={i} className="h-28 animate-pulse rounded-lg border border-border bg-card" />
          ))}
        </div>
      ) : workspaces.length === 0 ? (
        <div className="rounded-lg border border-border bg-card p-12 text-center">
          <Boxes className="mx-auto h-10 w-10 text-muted-foreground" />
          <p className="mt-3 text-muted-foreground">
            No workspaces yet. Create one to get started.
          </p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {workspaces.map((ws) => (
            <button
              key={ws.id}
              type="button"
              className="rounded-lg border border-border bg-card p-4 text-left transition-colors hover:bg-accent/30"
              onClick={() => navigate(`/w/${ws.identifier}/`)}
            >
              <p className="text-sm font-medium">{ws.name}</p>
              <p className="mt-0.5 text-xs text-muted-foreground font-mono">
                {ws.identifier}
              </p>
              {ws.description && (
                <p className="mt-2 truncate text-xs text-muted-foreground">
                  {ws.description}
                </p>
              )}
              <p className="mt-2 text-xs text-muted-foreground">
                Created {new Date(ws.createdAt).toLocaleDateString()}
              </p>
            </button>
          ))}
        </div>
      )}

      <Modal
        open={showCreate}
        onOpenChange={setShowCreate}
        title="Create Workspace"
        description="Add a new workspace for organizing your workflows."
      >
        <form onSubmit={handleCreate} className="space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium">Identifier</label>
            <input
              type="text"
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, "-"))}
              placeholder="e.g. my-team"
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-primary"
              required
            />
            <p className="mt-1 text-xs text-muted-foreground">
              URL-safe identifier. Lowercase letters, numbers, and hyphens only.
            </p>
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. My Team"
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
              disabled={createWorkspace.isPending || !identifier.trim() || !name.trim()}
              className={cn(
                "rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90",
                createWorkspace.isPending && "opacity-50",
              )}
            >
              {createWorkspace.isPending ? "Creating..." : "Create"}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
