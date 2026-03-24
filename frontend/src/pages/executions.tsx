import { StatusBadge } from "@/components/status-badge";
import { useWorkspace } from "@/contexts/workspace-context";
import { useExecutions } from "@/hooks/use-executions";
import { Activity, Search } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

export function Executions() {
  const { workspace } = useWorkspace();
  const navigate = useNavigate();
  const { data: page, isLoading } = useExecutions(workspace?.identifier);
  const [search, setSearch] = useState("");

  const executions = page?.content ?? [];
  const filtered = search
    ? executions.filter(
        (e) =>
          e.workflowName.toLowerCase().includes(search.toLowerCase()) ||
          e.status.toLowerCase().includes(search.toLowerCase()) ||
          e.triggerType.toLowerCase().includes(search.toLowerCase()),
      )
    : executions;

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">Executions</h1>

      <div className="relative">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <input
          type="text"
          placeholder="Search by workflow name, status, trigger..."
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
              className="h-14 animate-pulse rounded-lg border border-border bg-card"
            />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="rounded-lg border border-border bg-card p-12 text-center">
          <Activity className="mx-auto h-10 w-10 text-muted-foreground" />
          <p className="mt-3 text-muted-foreground">
            {search ? "No executions match your search." : "No executions yet."}
          </p>
        </div>
      ) : (
        <>
          {/* Header row */}
          <div className="hidden sm:flex items-center gap-4 px-4 py-2 text-xs font-medium uppercase tracking-wider text-muted-foreground">
            <span className="min-w-0 flex-1">Workflow</span>
            <span className="w-24">Status</span>
            <span className="w-20">Trigger</span>
            <span className="w-20 text-right">Duration</span>
            <span className="w-28 text-right">Started</span>
            <span className="w-28 text-right">Created</span>
          </div>

          <div className="space-y-1">
            {filtered.map((ex) => (
              <button
                key={ex.id}
                type="button"
                className="flex w-full items-center gap-4 rounded-lg border border-border bg-card px-4 py-3 text-left transition-colors hover:bg-accent/30"
                onClick={() =>
                  navigate(`/w/${workspace?.identifier}/executions/${ex.id}`)
                }
              >
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium">
                    {ex.workflowName}
                  </p>
                  <p className="truncate text-xs text-muted-foreground font-mono">
                    {ex.id.slice(0, 8)}...
                  </p>
                </div>
                <div className="w-24">
                  <StatusBadge status={ex.status} />
                </div>
                <span className="w-20 text-xs text-muted-foreground">
                  {ex.triggerType}
                </span>
                <span className="w-20 text-right text-xs text-muted-foreground">
                  {ex.durationMs > 0 ? formatDuration(ex.durationMs) : "--"}
                </span>
                <span className="w-28 text-right text-xs text-muted-foreground">
                  {ex.startedAt
                    ? new Date(ex.startedAt).toLocaleString()
                    : "--"}
                </span>
                <span className="w-28 text-right text-xs text-muted-foreground">
                  {new Date(ex.createdAt).toLocaleString()}
                </span>
              </button>
            ))}
          </div>
        </>
      )}

      {page && page.totalPages > 1 && (
        <p className="text-center text-sm text-muted-foreground">
          Showing {filtered.length} of {page.totalElements} executions
        </p>
      )}
    </div>
  );
}

function formatDuration(ms: number): string {
  if (ms < 1000) return `${ms}ms`;
  const s = ms / 1000;
  if (s < 60) return `${s.toFixed(1)}s`;
  const m = Math.floor(s / 60);
  const remaining = (s % 60).toFixed(0);
  return `${m}m ${remaining}s`;
}
