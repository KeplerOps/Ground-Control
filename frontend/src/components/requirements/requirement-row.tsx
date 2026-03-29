import { StatusBadgeDropdown } from "@/components/status-badge";
import { PriorityBadge, TypeBadge } from "@/components/ui/badge";
import { useToast } from "@/components/ui/toast";
import { useTransitionStatus } from "@/hooks/use-requirements";
import { cn } from "@/lib/utils";
import type { Priority, RequirementType, Status } from "@/types/api";
import * as Checkbox from "@radix-ui/react-checkbox";
import { Check } from "lucide-react";

export function RequirementRow({
  req,
  selected,
  active,
  onToggle,
  onClick,
}: {
  req: {
    id: string;
    uid: string;
    title: string;
    requirementType: RequirementType;
    priority: Priority;
    status: Status;
    wave: number;
    updatedAt: string;
  };
  selected: boolean;
  active?: boolean;
  onToggle: () => void;
  onClick: () => void;
}) {
  const { toast } = useToast();
  const transition = useTransitionStatus(req.id);

  function handleRowClick(e: React.MouseEvent) {
    const target = e.target as HTMLElement;
    if (target.closest("[data-no-row-click]")) return;
    onClick();
  }

  function handleRowKeyDown(e: React.KeyboardEvent) {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      onClick();
    }
  }

  return (
    <tr
      className={cn(
        "hover:bg-accent/30 cursor-pointer",
        active && "bg-accent/50",
      )}
      onClick={handleRowClick}
      onKeyDown={handleRowKeyDown}
      tabIndex={0}
    >
      <td className="px-3 py-3" data-no-row-click>
        <Checkbox.Root
          className="flex h-4 w-4 items-center justify-center rounded border border-input bg-background"
          checked={selected}
          onCheckedChange={onToggle}
        >
          <Checkbox.Indicator>
            <Check className="h-3 w-3" />
          </Checkbox.Indicator>
        </Checkbox.Root>
      </td>
      <td className="px-3 py-3 font-mono text-xs text-muted-foreground">
        {req.uid}
      </td>
      <td className="px-3 py-3 font-medium max-w-[300px] truncate">
        {req.title}
      </td>
      <td className="px-3 py-3">
        <TypeBadge type={req.requirementType} />
      </td>
      <td className="px-3 py-3">
        <PriorityBadge priority={req.priority} />
      </td>
      <td className="px-3 py-3" data-no-row-click>
        <StatusBadgeDropdown
          status={req.status}
          onTransition={(s) =>
            transition.mutate(s, {
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
      </td>
      <td className="px-3 py-3 text-muted-foreground">{req.wave}</td>
      <td className="px-3 py-3 text-xs text-muted-foreground">
        {new Date(req.updatedAt).toLocaleDateString()}
      </td>
    </tr>
  );
}
