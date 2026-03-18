import { cn } from "@/lib/utils";

interface FormFieldProps {
  label: string;
  error?: string;
  children: React.ReactNode;
  className?: string;
}

export function FormField({
  label,
  error,
  children,
  className,
}: FormFieldProps) {
  return (
    <label className={cn("block space-y-1.5", className)}>
      <span className="text-sm font-medium text-foreground">{label}</span>
      {children}
      {error && <p className="text-xs text-destructive">{error}</p>}
    </label>
  );
}

export const inputClass =
  "w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-1 focus:ring-offset-background";

export const selectClass =
  "w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-1 focus:ring-offset-background";

export const buttonClass =
  "inline-flex items-center justify-center rounded-md px-4 py-2 text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-1 focus:ring-offset-background disabled:pointer-events-none disabled:opacity-50";

export const primaryButton = cn(
  buttonClass,
  "bg-primary text-primary-foreground hover:bg-primary/90",
);

export const secondaryButton = cn(
  buttonClass,
  "border border-input bg-background text-foreground hover:bg-accent hover:text-accent-foreground",
);

export const destructiveButton = cn(
  buttonClass,
  "bg-destructive text-white hover:bg-destructive/90",
);
