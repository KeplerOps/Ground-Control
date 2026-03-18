import { cn } from "@/lib/utils";
import * as Toast from "@radix-ui/react-toast";
import { X } from "lucide-react";
import { createContext, useCallback, useContext, useState } from "react";

type ToastVariant = "success" | "error" | "info";

interface ToastItem {
  id: number;
  title: string;
  description?: string;
  variant: ToastVariant;
}

interface ToastContextValue {
  toast: (item: Omit<ToastItem, "id">) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

let nextId = 0;

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const addToast = useCallback((item: Omit<ToastItem, "id">) => {
    const id = nextId++;
    setToasts((prev) => [...prev, { ...item, id }]);
  }, []);

  const removeToast = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  return (
    <ToastContext.Provider value={{ toast: addToast }}>
      <Toast.Provider swipeDirection="right" duration={4000}>
        {children}
        {toasts.map((t) => (
          <Toast.Root
            key={t.id}
            className={cn(
              "rounded-lg border bg-card p-4 shadow-lg",
              t.variant === "error" && "border-destructive",
              t.variant === "success" && "border-green-600",
              t.variant === "info" && "border-primary",
            )}
            onOpenChange={(open) => {
              if (!open) removeToast(t.id);
            }}
          >
            <div className="flex items-start gap-3">
              <div className="flex-1">
                <Toast.Title className="text-sm font-medium">
                  {t.title}
                </Toast.Title>
                {t.description && (
                  <Toast.Description className="mt-1 text-xs text-muted-foreground">
                    {t.description}
                  </Toast.Description>
                )}
              </div>
              <Toast.Close className="rounded-md p-1 text-muted-foreground hover:text-foreground">
                <X className="h-3 w-3" />
              </Toast.Close>
            </div>
          </Toast.Root>
        ))}
        <Toast.Viewport className="fixed bottom-4 right-4 z-[200] flex max-w-sm flex-col gap-2" />
      </Toast.Provider>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error("useToast must be within ToastProvider");
  return ctx;
}
