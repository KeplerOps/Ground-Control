import { cn } from "@/lib/utils";
import type { Priority, RequirementType, Status } from "@/types/api";

const statusColors: Record<Status, string> = {
  DRAFT: "bg-gray-500/15 text-gray-400",
  ACTIVE: "bg-green-500/15 text-green-400",
  DEPRECATED: "bg-orange-500/15 text-orange-400",
  ARCHIVED: "bg-gray-500/15 text-gray-500",
};

const priorityColors: Record<Priority, string> = {
  MUST: "bg-red-500/15 text-red-400",
  SHOULD: "bg-yellow-500/15 text-yellow-400",
  COULD: "bg-blue-500/15 text-blue-400",
  WONT: "bg-gray-500/15 text-gray-500",
};

const typeColors: Record<RequirementType, string> = {
  FUNCTIONAL: "bg-violet-500/15 text-violet-400",
  NON_FUNCTIONAL: "bg-cyan-500/15 text-cyan-400",
  CONSTRAINT: "bg-amber-500/15 text-amber-400",
  INTERFACE: "bg-teal-500/15 text-teal-400",
  PERFORMANCE: "bg-pink-500/15 text-pink-400",
  SECURITY: "bg-rose-500/15 text-rose-400",
  DATA: "bg-indigo-500/15 text-indigo-400",
};

interface BadgeProps {
  children: React.ReactNode;
  className?: string;
}

export function Badge({ children, className }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium",
        className,
      )}
    >
      {children}
    </span>
  );
}

export function StatusBadge({ status }: { status: Status }) {
  return <Badge className={statusColors[status]}>{status}</Badge>;
}

export function PriorityBadge({ priority }: { priority: Priority }) {
  return <Badge className={priorityColors[priority]}>{priority}</Badge>;
}

export function TypeBadge({ type }: { type: RequirementType }) {
  return <Badge className={typeColors[type]}>{type.replace("_", " ")}</Badge>;
}
