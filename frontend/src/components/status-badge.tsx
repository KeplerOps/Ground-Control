import { cn } from "@/lib/utils";
import type { ExecutionStatus, WorkflowStatus } from "@/types/api";

const workflowStatusColors: Record<WorkflowStatus, string> = {
  DRAFT: "bg-gray-500/15 text-gray-400",
  ACTIVE: "bg-green-500/15 text-green-400",
  PAUSED: "bg-yellow-500/15 text-yellow-400",
  ARCHIVED: "bg-gray-500/15 text-gray-500",
};

const executionStatusColors: Record<ExecutionStatus, string> = {
  PENDING: "bg-gray-500/15 text-gray-400",
  QUEUED: "bg-blue-500/15 text-blue-400",
  RUNNING: "bg-blue-500/15 text-blue-300 animate-pulse",
  SUCCESS: "bg-green-500/15 text-green-400",
  FAILED: "bg-red-500/15 text-red-400",
  CANCELLED: "bg-gray-500/15 text-gray-500",
  SKIPPED: "bg-gray-500/15 text-gray-400",
  TIMED_OUT: "bg-orange-500/15 text-orange-400",
};

interface StatusBadgeProps {
  status: WorkflowStatus | ExecutionStatus;
  className?: string;
}

export function StatusBadge({ status, className }: StatusBadgeProps) {
  const color =
    workflowStatusColors[status as WorkflowStatus] ??
    executionStatusColors[status as ExecutionStatus] ??
    "bg-gray-500/15 text-gray-400";

  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium",
        color,
        className,
      )}
    >
      {status}
    </span>
  );
}
