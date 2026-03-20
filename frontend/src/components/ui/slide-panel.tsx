import { cn } from "@/lib/utils";
import * as Dialog from "@radix-ui/react-dialog";
import { X } from "lucide-react";

interface SlidePanelProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description?: string;
  children: React.ReactNode;
  className?: string;
}

export function SlidePanel({
  open,
  onOpenChange,
  title,
  description,
  children,
  className,
}: SlidePanelProps) {
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-black/50 data-[state=open]:animate-[fade-in_150ms_ease-out] data-[state=closed]:animate-[fade-out_150ms_ease-in]" />
        <Dialog.Content
          className={cn(
            "fixed inset-y-0 right-0 z-50 w-full max-w-xl",
            "border-l border-border bg-card shadow-lg",
            "overflow-y-auto",
            "data-[state=open]:animate-[slide-in-right_200ms_ease-out] data-[state=closed]:animate-[slide-out-right_150ms_ease-in]",
            className,
          )}
        >
          <div className="sticky top-0 z-10 flex items-start justify-between border-b border-border bg-card px-6 py-4">
            <div>
              <Dialog.Title className="text-lg font-semibold">
                {title}
              </Dialog.Title>
              {description && (
                <Dialog.Description className="mt-1 text-sm text-muted-foreground">
                  {description}
                </Dialog.Description>
              )}
            </div>
            <Dialog.Close className="rounded-md p-1 text-muted-foreground hover:bg-accent hover:text-foreground">
              <X className="h-4 w-4" />
            </Dialog.Close>
          </div>
          <div className="p-6">{children}</div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
