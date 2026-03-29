import { cn } from "@/lib/utils";

export interface LegendItem {
  key: string;
  count: number;
  color: string;
}

export interface RelationLegendItem {
  type: string;
  color: string;
  style: string;
  label: string;
  count: number;
}

export function GraphLegend({
  legendItems,
  relationLegend,
  onLegendClick,
}: {
  legendItems: LegendItem[];
  relationLegend: RelationLegendItem[];
  onLegendClick: (key: string) => void;
}) {
  return (
    <div className="flex flex-wrap gap-3 border-b border-border bg-card px-4 py-1.5">
      {legendItems.map((item) => (
        <button
          type="button"
          key={item.key}
          onClick={() => onLegendClick(item.key)}
          className="flex cursor-pointer items-center gap-1 text-[11px] text-muted-foreground hover:text-foreground"
        >
          <span
            className="inline-block h-2.5 w-2.5 rounded-sm"
            style={{ background: item.color }}
          />
          {item.key} ({item.count})
        </button>
      ))}
      {relationLegend.length > 0 && (
        <>
          <span className="ml-2 text-[11px] text-muted-foreground">Edges:</span>
          {relationLegend.map((item) => (
            <span
              key={item.type}
              className="flex items-center gap-1 text-[11px] text-muted-foreground"
            >
              <span
                className={cn(
                  "inline-block w-6 border-t-2",
                  item.style === "dashed" && "border-dashed",
                  item.style === "dotted" && "border-dotted",
                )}
                style={{ borderTopColor: item.color }}
              />
              {item.label} ({item.count})
            </span>
          ))}
        </>
      )}
    </div>
  );
}
