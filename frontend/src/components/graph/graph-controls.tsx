import type { ColorScheme, LayoutId } from "@/lib/graph-constants";
import { Maximize, RotateCcw } from "lucide-react";

export function GraphControls({
  colorScheme,
  setColorScheme,
  layoutId,
  setLayoutId,
  onFit,
  onReset,
}: {
  colorScheme: ColorScheme;
  setColorScheme: (v: ColorScheme) => void;
  layoutId: LayoutId;
  setLayoutId: (v: LayoutId) => void;
  onFit: () => void;
  onReset: () => void;
}) {
  return (
    <div className="flex items-center gap-4 border-b border-border bg-card px-4 py-2">
      <div className="flex items-center gap-2">
        <label
          htmlFor="graph-color-by"
          className="text-xs text-muted-foreground"
        >
          Color by
        </label>
        <select
          id="graph-color-by"
          value={colorScheme}
          onChange={(e) => setColorScheme(e.target.value as ColorScheme)}
          className="rounded border border-input bg-background px-2 py-1 text-xs text-foreground"
        >
          <option value="series">Series</option>
          <option value="priority">Priority</option>
          <option value="status">Status</option>
          <option value="wave">Wave</option>
        </select>
      </div>
      <div className="flex items-center gap-2">
        <label htmlFor="graph-layout" className="text-xs text-muted-foreground">
          Layout
        </label>
        <select
          id="graph-layout"
          value={layoutId}
          onChange={(e) => setLayoutId(e.target.value as LayoutId)}
          className="rounded border border-input bg-background px-2 py-1 text-xs text-foreground"
        >
          <option value="dagre-lr">DAG (left to right)</option>
          <option value="dagre-tb">DAG (top to bottom)</option>
          <option value="dagre-wave-lr">Wave-ordered (L-R)</option>
          <option value="dagre-wave-tb">Wave-ordered (T-B)</option>
        </select>
      </div>
      <button
        type="button"
        onClick={onFit}
        className="flex items-center gap-1 rounded border border-input bg-background px-2 py-1 text-xs hover:border-primary"
        title="Fit to screen"
      >
        <Maximize className="h-3 w-3" /> Fit
      </button>
      <button
        type="button"
        onClick={onReset}
        className="flex items-center gap-1 rounded border border-input bg-background px-2 py-1 text-xs hover:border-primary"
        title="Reset filters"
      >
        <RotateCcw className="h-3 w-3" /> Reset
      </button>
    </div>
  );
}
