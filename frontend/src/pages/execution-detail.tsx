import { StatusBadge } from "@/components/status-badge";
import { useWorkspace } from "@/contexts/workspace-context";
import {
  useCancelExecution,
  useExecution,
  useRetryExecution,
} from "@/hooks/use-executions";
import { cn } from "@/lib/utils";
import type { TaskExecution } from "@/types/api";
import {
  ArrowLeft,
  ChevronDown,
  ChevronRight,
  Loader2,
  RefreshCw,
  Square,
} from "lucide-react";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";

export function ExecutionDetail() {
  const { id } = useParams<{ id: string }>();
  const { workspace } = useWorkspace();
  const { data: execution, isLoading } = useExecution(id ?? "");
  const cancelExecution = useCancelExecution();
  const retryExecution = useRetryExecution();

  const base = `/w/${workspace?.identifier}`;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!execution) {
    return (
      <div className="py-20 text-center text-muted-foreground">
        Execution not found.
      </div>
    );
  }

  const isRunning =
    execution.status === "RUNNING" ||
    execution.status === "PENDING" ||
    execution.status === "QUEUED";
  const isFailed =
    execution.status === "FAILED" || execution.status === "TIMED_OUT";

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-center gap-3">
          <Link
            to={`${base}/executions`}
            className="rounded-md p-1 text-muted-foreground hover:bg-accent hover:text-foreground"
          >
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-2xl font-semibold">
                {execution.workflowName}
              </h1>
              <StatusBadge status={execution.status} />
            </div>
            <p className="mt-1 text-sm text-muted-foreground font-mono">
              {execution.id}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {isRunning && (
            <button
              type="button"
              onClick={() => {
                if (id) cancelExecution.mutate(id);
              }}
              disabled={cancelExecution.isPending}
              className="inline-flex items-center gap-1.5 rounded-md border border-destructive px-3 py-1.5 text-sm font-medium text-destructive hover:bg-destructive/10 disabled:opacity-50"
            >
              <Square className="h-4 w-4" />
              {cancelExecution.isPending ? "Cancelling..." : "Cancel"}
            </button>
          )}
          {isFailed && (
            <button
              type="button"
              onClick={() => {
                if (id) retryExecution.mutate(id);
              }}
              disabled={retryExecution.isPending}
              className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            >
              <RefreshCw className="h-4 w-4" />
              {retryExecution.isPending ? "Retrying..." : "Retry"}
            </button>
          )}
        </div>
      </div>

      {/* Metadata */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <MetaCard label="Trigger" value={execution.triggerType} />
        <MetaCard
          label="Duration"
          value={
            execution.durationMs > 0
              ? formatDuration(execution.durationMs)
              : "--"
          }
        />
        <MetaCard
          label="Started"
          value={
            execution.startedAt
              ? new Date(execution.startedAt).toLocaleString()
              : "--"
          }
        />
        <MetaCard
          label="Finished"
          value={
            execution.finishedAt
              ? new Date(execution.finishedAt).toLocaleString()
              : "--"
          }
        />
      </div>

      {/* Error */}
      {execution.error && (
        <div className="rounded-lg border border-destructive/50 bg-destructive/10 p-4">
          <p className="text-sm font-medium text-destructive">Error</p>
          <pre className="mt-1 whitespace-pre-wrap text-sm text-destructive/80 font-mono">
            {execution.error}
          </pre>
        </div>
      )}

      {/* Inputs/Outputs */}
      {execution.inputs && execution.inputs !== "{}" && (
        <CollapsibleSection title="Inputs">
          <pre className="whitespace-pre-wrap rounded-md bg-muted p-3 text-sm font-mono">
            {tryFormatJson(execution.inputs)}
          </pre>
        </CollapsibleSection>
      )}
      {execution.outputs && execution.outputs !== "{}" && (
        <CollapsibleSection title="Outputs">
          <pre className="whitespace-pre-wrap rounded-md bg-muted p-3 text-sm font-mono">
            {tryFormatJson(execution.outputs)}
          </pre>
        </CollapsibleSection>
      )}

      {/* Task Executions */}
      <div className="space-y-3">
        <h2 className="text-lg font-medium">
          Tasks ({execution.tasks?.length ?? 0})
        </h2>
        {!execution.tasks || execution.tasks.length === 0 ? (
          <div className="rounded-lg border border-border bg-card p-8 text-center text-muted-foreground">
            No task executions recorded.
          </div>
        ) : (
          <div className="space-y-2">
            {execution.tasks.map((task) => (
              <TaskRow key={task.id} task={task} />
            ))}
          </div>
        )}
      </div>

      {/* Link to workflow */}
      <Link
        to={`${base}/workflows/${execution.workflowId}`}
        className="text-sm text-primary hover:underline"
      >
        View workflow definition &rarr;
      </Link>
    </div>
  );
}

function MetaCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-border bg-card p-3">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="mt-0.5 text-sm font-medium">{value}</p>
    </div>
  );
}

function TaskRow({ task }: { task: TaskExecution }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className="rounded-lg border border-border bg-card">
      <button
        type="button"
        className="flex w-full items-center gap-3 px-4 py-3 text-left hover:bg-accent/20"
        onClick={() => setExpanded(!expanded)}
      >
        {expanded ? (
          <ChevronDown className="h-4 w-4 shrink-0 text-muted-foreground" />
        ) : (
          <ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground" />
        )}
        <div className="min-w-0 flex-1">
          <p className="text-sm font-medium">{task.nodeName}</p>
          <p className="text-xs text-muted-foreground">
            {task.nodeType} &middot; Attempt {task.attempt}
          </p>
        </div>
        <StatusBadge status={task.status} />
        <span className="shrink-0 text-xs text-muted-foreground">
          {task.durationMs > 0 ? formatDuration(task.durationMs) : "--"}
        </span>
      </button>

      {expanded && (
        <div className="space-y-3 border-t border-border px-4 py-3">
          {task.error && (
            <div className="rounded-md border border-destructive/50 bg-destructive/10 p-3">
              <p className="text-xs font-medium text-destructive">Error</p>
              <pre className="mt-1 whitespace-pre-wrap text-xs text-destructive/80 font-mono">
                {task.error}
              </pre>
            </div>
          )}

          {task.logs && (
            <div>
              <p className="mb-1 text-xs font-medium text-muted-foreground">
                Logs
              </p>
              <pre className="max-h-64 overflow-auto whitespace-pre-wrap rounded-md bg-muted p-3 text-xs font-mono">
                {task.logs}
              </pre>
            </div>
          )}

          {task.inputs && task.inputs !== "{}" && (
            <div>
              <p className="mb-1 text-xs font-medium text-muted-foreground">
                Inputs
              </p>
              <pre className="whitespace-pre-wrap rounded-md bg-muted p-3 text-xs font-mono">
                {tryFormatJson(task.inputs)}
              </pre>
            </div>
          )}

          {task.outputs && task.outputs !== "{}" && (
            <div>
              <p className="mb-1 text-xs font-medium text-muted-foreground">
                Outputs
              </p>
              <pre className="whitespace-pre-wrap rounded-md bg-muted p-3 text-xs font-mono">
                {tryFormatJson(task.outputs)}
              </pre>
            </div>
          )}

          <div className="flex gap-4 text-xs text-muted-foreground">
            <span>
              Started:{" "}
              {task.startedAt
                ? new Date(task.startedAt).toLocaleString()
                : "--"}
            </span>
            <span>
              Finished:{" "}
              {task.finishedAt
                ? new Date(task.finishedAt).toLocaleString()
                : "--"}
            </span>
          </div>
        </div>
      )}
    </div>
  );
}

function CollapsibleSection({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  const [open, setOpen] = useState(false);
  return (
    <div className="space-y-2">
      <button
        type="button"
        className="flex items-center gap-2 text-sm font-medium text-muted-foreground hover:text-foreground"
        onClick={() => setOpen(!open)}
      >
        {open ? (
          <ChevronDown className="h-4 w-4" />
        ) : (
          <ChevronRight className="h-4 w-4" />
        )}
        {title}
      </button>
      {open && children}
    </div>
  );
}

function tryFormatJson(s: string): string {
  try {
    return JSON.stringify(JSON.parse(s), null, 2);
  } catch {
    return s;
  }
}

function formatDuration(ms: number): string {
  if (ms < 1000) return `${ms}ms`;
  const s = ms / 1000;
  if (s < 60) return `${s.toFixed(1)}s`;
  const m = Math.floor(s / 60);
  const remaining = (s % 60).toFixed(0);
  return `${m}m ${remaining}s`;
}
