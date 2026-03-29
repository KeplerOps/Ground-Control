import {
  inputClass,
  primaryButton,
  secondaryButton,
} from "@/components/ui/form-field";
import { Modal } from "@/components/ui/modal";
import { useToast } from "@/components/ui/toast";
import { useCloneRequirement } from "@/hooks/use-requirements";
import { useState } from "react";

export function CloneModal({
  open,
  onOpenChange,
  requirementId,
  onCloned,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  requirementId: string;
  onCloned: (req: { id: string }) => void;
}) {
  const { toast } = useToast();
  const cloneMutation = useCloneRequirement(requirementId);
  const [newUid, setNewUid] = useState("");
  const [copyRelations, setCopyRelations] = useState(false);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    cloneMutation.mutate(
      { newUid, copyRelations },
      {
        onSuccess: (data) => {
          onOpenChange(false);
          toast({ title: "Requirement cloned", variant: "success" });
          onCloned(data);
        },
        onError: (err) =>
          toast({
            title: "Clone failed",
            description: err.message,
            variant: "error",
          }),
      },
    );
  }

  return (
    <Modal open={open} onOpenChange={onOpenChange} title="Clone Requirement">
      <form onSubmit={handleSubmit} className="space-y-4">
        <label className="block space-y-1.5">
          <span className="text-sm font-medium">New UID</span>
          <input
            className={inputClass}
            value={newUid}
            onChange={(e) => setNewUid(e.target.value)}
            placeholder="GC-A002"
            required
          />
        </label>
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={copyRelations}
            onChange={(e) => setCopyRelations(e.target.checked)}
            className="rounded"
          />
          Copy relations
        </label>
        <div className="flex justify-end gap-3 pt-2">
          <button
            type="button"
            className={secondaryButton}
            onClick={() => onOpenChange(false)}
          >
            Cancel
          </button>
          <button
            type="submit"
            className={primaryButton}
            disabled={cloneMutation.isPending}
          >
            {cloneMutation.isPending ? "Cloning..." : "Clone"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
