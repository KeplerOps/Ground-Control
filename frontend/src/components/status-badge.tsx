import { cn } from "@/lib/utils";
import type { Status } from "@/types/api";
import * as DropdownMenu from "@radix-ui/react-dropdown-menu";
import { ChevronDown } from "lucide-react";

const statusColors: Record<Status, string> = {
  DRAFT: "bg-gray-500/15 text-gray-400",
  ACTIVE: "bg-green-500/15 text-green-400",
  DEPRECATED: "bg-orange-500/15 text-orange-400",
  ARCHIVED: "bg-gray-500/15 text-gray-500",
};

const validTransitions: Record<Status, Status[]> = {
  DRAFT: ["ACTIVE"],
  ACTIVE: ["DEPRECATED"],
  DEPRECATED: ["ACTIVE", "ARCHIVED"],
  ARCHIVED: [],
};

interface StatusBadgeDropdownProps {
  status: Status;
  onTransition: (newStatus: Status) => void;
  disabled?: boolean;
}

export function StatusBadgeDropdown({
  status,
  onTransition,
  disabled,
}: StatusBadgeDropdownProps) {
  const transitions = validTransitions[status];

  if (transitions.length === 0 || disabled) {
    return (
      <span
        className={cn(
          "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium",
          statusColors[status],
        )}
      >
        {status}
      </span>
    );
  }

  return (
    <DropdownMenu.Root>
      <DropdownMenu.Trigger
        className={cn(
          "inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium cursor-pointer",
          "hover:ring-1 hover:ring-primary/50",
          statusColors[status],
        )}
      >
        {status}
        <ChevronDown className="h-3 w-3" />
      </DropdownMenu.Trigger>

      <DropdownMenu.Portal>
        <DropdownMenu.Content
          className="min-w-[120px] rounded-md border border-border bg-card p-1 shadow-lg"
          sideOffset={4}
        >
          {transitions.map((s) => (
            <DropdownMenu.Item
              key={s}
              className="flex cursor-pointer items-center rounded-sm px-2 py-1.5 text-sm outline-none hover:bg-accent"
              onSelect={() => onTransition(s)}
            >
              <span
                className={cn(
                  "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium",
                  statusColors[s],
                )}
              >
                {s}
              </span>
            </DropdownMenu.Item>
          ))}
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  );
}
