import { StatusBadge } from "@/components/status-badge";
import { useWorkspace } from "@/contexts/workspace-context";
import { useExecutions } from "@/hooks/use-executions";
import { useWorkflows } from "@/hooks/use-workflows";
import { cn } from "@/lib/utils";
import {
  Activity,
  ArrowRight,
  Clock,
  Play,
  Plus,
  Rocket,
  Workflow,
} from "lucide-react";
import { Link, useNavigate } from "react-router-dom";

export function Dashboard() {
  const { workspace, isLoading } = useWorkspace();
  const navigate = useNavigate();

  if (isLoading) return <LoadingSkeleton />;

  if (!workspace) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20 text-center">
        <Rocket className="h-12 w-12 text-muted-foreground" />
        <h1 className="text-2xl font-semibold">Welcome to Ground Control</h1>
        <p className="text-muted-foreground">
          Select a workspace from the header to get started.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold">{workspace.name}</h1>
          {workspace.description && (
            <p className="mt-1 text-muted-foreground">
              {workspace.description}
            </p>
          )}
        </div>
        <div className="flex gap-2">
          <Link
            to={`/w/${workspace.identifier}/workflows`}
            className="inline-flex items-center gap-1.5 rounded-md border border-border bg-card px-3 py-1.5 text-sm font-medium hover:bg-accent/50"
          >
            <Workflow className="h-4 w-4" />
            View Workflows
          </Link>
          <Link
            to={`/w/${workspace.identifier}/executions`}
            className="inline-flex items-center gap-1.5 rounded-md border border-border bg-card px-3 py-1.5 text-sm font-medium hover:bg-accent/50"
          >
            <Play className="h-4 w-4" />
            View Executions
          </Link>
        </div>
      </div>

      <DashboardContent
        workspaceId={workspace.identifier}
        navigate={navigate}
      />
    </div>
  );
}

function DashboardContent({
  workspaceId,
  navigate,
}: {
  workspaceId: string;
  navigate: (path: string) => void;
}) {
  const { data: workflowPage, isLoading: wLoading } = useWorkflows(workspaceId);
  const { data: executionPage, isLoading: eLoading } =
    useExecutions(workspaceId);

  if (wLoading || eLoading) return <LoadingSkeleton />;

  const workflows = workflowPage?.content ?? [];
  const executions = executionPage?.content ?? [];

  const statusCounts = {
    DRAFT: workflows.filter((w) => w.status === "DRAFT").length,
    ACTIVE: workflows.filter((w) => w.status === "ACTIVE").length,
    PAUSED: workflows.filter((w) => w.status === "PAUSED").length,
    ARCHIVED: workflows.filter((w) => w.status === "ARCHIVED").length,
  };

  const executionCounts = {
    total: executionPage?.totalElements ?? 0,
    running: executions.filter(
      (e) =>
        e.status === "RUNNING" ||
        e.status === "PENDING" ||
        e.status === "QUEUED",
    ).length,
    success: executions.filter((e) => e.status === "SUCCESS").length,
    failed: executions.filter(
      (e) => e.status === "FAILED" || e.status === "TIMED_OUT",
    ).length,
  };

  const statCards = [
    {
      label: "Total Workflows",
      value: workflows.length,
      color: "text-foreground",
    },
    { label: "Active", value: statusCounts.ACTIVE, color: "text-green-400" },
    { label: "Draft", value: statusCounts.DRAFT, color: "text-gray-400" },
    {
      label: "Running Executions",
      value: executionCounts.running,
      color: "text-blue-400",
    },
    {
      label: "Failed Executions",
      value: executionCounts.failed,
      color: "text-red-400",
    },
  ];

  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        {statCards.map((s) => (
          <div
            key={s.label}
            className="rounded-lg border border-border bg-card p-4"
          >
            <p className="text-sm text-muted-foreground">{s.label}</p>
            <p className={cn("mt-1 text-2xl font-semibold", s.color)}>
              {s.value}
            </p>
          </div>
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Recent Workflows */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="flex items-center gap-2 text-lg font-medium">
              <Workflow className="h-5 w-5 text-muted-foreground" />
              Recent Workflows
            </h2>
            <button
              type="button"
              onClick={() => navigate(`/w/${workspaceId}/workflows`)}
              className="flex items-center gap-1 text-sm text-primary hover:underline"
            >
              View all <ArrowRight className="h-3.5 w-3.5" />
            </button>
          </div>
          {workflows.length === 0 ? (
            <div className="rounded-lg border border-border bg-card p-8 text-center">
              <p className="text-muted-foreground">No workflows yet.</p>
              <button
                type="button"
                onClick={() => navigate(`/w/${workspaceId}/workflows`)}
                className="mt-3 inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90"
              >
                <Plus className="h-4 w-4" />
                Create Workflow
              </button>
            </div>
          ) : (
            <div className="space-y-1">
              {workflows.slice(0, 5).map((wf) => (
                <button
                  key={wf.id}
                  type="button"
                  className="flex w-full items-center gap-3 rounded-lg border border-border bg-card px-3 py-2 text-left transition-colors hover:bg-accent/30"
                  onClick={() =>
                    navigate(`/w/${workspaceId}/workflows/${wf.id}`)
                  }
                >
                  <span className="min-w-0 flex-1 truncate text-sm font-medium">
                    {wf.name}
                  </span>
                  <StatusBadge status={wf.status} />
                  <span className="shrink-0 text-xs text-muted-foreground">
                    v{wf.currentVersion}
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Recent Executions */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="flex items-center gap-2 text-lg font-medium">
              <Activity className="h-5 w-5 text-muted-foreground" />
              Recent Executions
            </h2>
            <button
              type="button"
              onClick={() => navigate(`/w/${workspaceId}/executions`)}
              className="flex items-center gap-1 text-sm text-primary hover:underline"
            >
              View all <ArrowRight className="h-3.5 w-3.5" />
            </button>
          </div>
          {executions.length === 0 ? (
            <div className="rounded-lg border border-border bg-card p-8 text-center">
              <p className="text-muted-foreground">No executions yet.</p>
            </div>
          ) : (
            <div className="space-y-1">
              {executions.slice(0, 5).map((ex) => (
                <button
                  key={ex.id}
                  type="button"
                  className="flex w-full items-center gap-3 rounded-lg border border-border bg-card px-3 py-2 text-left transition-colors hover:bg-accent/30"
                  onClick={() =>
                    navigate(`/w/${workspaceId}/executions/${ex.id}`)
                  }
                >
                  <span className="min-w-0 flex-1 truncate text-sm">
                    {ex.workflowName}
                  </span>
                  <StatusBadge status={ex.status} />
                  <span className="shrink-0 text-xs text-muted-foreground">
                    {ex.durationMs > 0
                      ? `${(ex.durationMs / 1000).toFixed(1)}s`
                      : "--"}
                  </span>
                  <span className="shrink-0 text-xs text-muted-foreground">
                    {formatRelativeTime(ex.createdAt)}
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function formatRelativeTime(timestamp: string): string {
  const now = Date.now();
  const then = new Date(timestamp).getTime();
  const diffMs = now - then;
  const diffSec = Math.floor(diffMs / 1000);
  if (diffSec < 60) return "just now";
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin}m ago`;
  const diffHr = Math.floor(diffMin / 60);
  if (diffHr < 24) return `${diffHr}h ago`;
  const diffDays = Math.floor(diffHr / 24);
  return `${diffDays}d ago`;
}

function LoadingSkeleton() {
  return (
    <div className="space-y-6">
      <div className="h-8 w-48 animate-pulse rounded bg-muted" />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        {["s1", "s2", "s3", "s4", "s5"].map((k) => (
          <div
            key={k}
            className="h-20 animate-pulse rounded-lg border border-border bg-card"
          />
        ))}
      </div>
    </div>
  );
}
