import { destructiveButton, secondaryButton } from "@/components/ui/form-field";
import { Modal } from "@/components/ui/modal";

interface ConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: string;
  confirmLabel?: string;
  onConfirm: () => void;
  loading?: boolean;
}

export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel = "Delete",
  onConfirm,
  loading,
}: ConfirmDialogProps) {
  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title={title}
      description={description}
    >
      <div className="flex justify-end gap-3 mt-4">
        <button
          type="button"
          className={secondaryButton}
          onClick={() => onOpenChange(false)}
        >
          Cancel
        </button>
        <button
          type="button"
          className={destructiveButton}
          onClick={onConfirm}
          disabled={loading}
        >
          {loading ? "..." : confirmLabel}
        </button>
      </div>
    </Modal>
  );
}
