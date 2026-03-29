import { Filter, X } from "lucide-react";

export interface FilterOptions {
  statuses: string[];
  priorities: string[];
  series: string[];
  waves: number[];
}

export function GraphFilters({
  filterStatus,
  setFilterStatus,
  filterPriority,
  setFilterPriority,
  filterSeries,
  setFilterSeries,
  filterWave,
  setFilterWave,
  hasFilters,
  onClearFilters,
  filterOptions,
}: {
  filterStatus: string;
  setFilterStatus: (v: string) => void;
  filterPriority: string;
  setFilterPriority: (v: string) => void;
  filterSeries: string;
  setFilterSeries: (v: string) => void;
  filterWave: string;
  setFilterWave: (v: string) => void;
  hasFilters: boolean;
  onClearFilters: () => void;
  filterOptions: FilterOptions;
}) {
  return (
    <div className="flex items-center gap-4 border-b border-border bg-card px-4 py-1.5">
      <Filter className="h-3.5 w-3.5 text-muted-foreground" />
      <div className="flex items-center gap-2">
        <label
          htmlFor="graph-filter-status"
          className="text-xs text-muted-foreground"
        >
          Status
        </label>
        <select
          id="graph-filter-status"
          value={filterStatus}
          onChange={(e) => setFilterStatus(e.target.value)}
          className="rounded border border-input bg-background px-2 py-0.5 text-xs text-foreground"
        >
          <option value="">All</option>
          {filterOptions.statuses.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>
      <div className="flex items-center gap-2">
        <label
          htmlFor="graph-filter-priority"
          className="text-xs text-muted-foreground"
        >
          Priority
        </label>
        <select
          id="graph-filter-priority"
          value={filterPriority}
          onChange={(e) => setFilterPriority(e.target.value)}
          className="rounded border border-input bg-background px-2 py-0.5 text-xs text-foreground"
        >
          <option value="">All</option>
          {filterOptions.priorities.map((p) => (
            <option key={p} value={p}>
              {p}
            </option>
          ))}
        </select>
      </div>
      <div className="flex items-center gap-2">
        <label
          htmlFor="graph-filter-series"
          className="text-xs text-muted-foreground"
        >
          Series
        </label>
        <select
          id="graph-filter-series"
          value={filterSeries}
          onChange={(e) => setFilterSeries(e.target.value)}
          className="rounded border border-input bg-background px-2 py-0.5 text-xs text-foreground"
        >
          <option value="">All</option>
          {filterOptions.series.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>
      <div className="flex items-center gap-2">
        <label
          htmlFor="graph-filter-wave"
          className="text-xs text-muted-foreground"
        >
          Wave
        </label>
        <select
          id="graph-filter-wave"
          value={filterWave}
          onChange={(e) => setFilterWave(e.target.value)}
          className="rounded border border-input bg-background px-2 py-0.5 text-xs text-foreground"
        >
          <option value="">All</option>
          {filterOptions.waves.map((w) => (
            <option key={w} value={String(w)}>
              {w}
            </option>
          ))}
        </select>
      </div>
      {hasFilters && (
        <button
          type="button"
          onClick={onClearFilters}
          className="flex items-center gap-1 rounded border border-input bg-background px-2 py-0.5 text-xs text-muted-foreground hover:border-primary hover:text-foreground"
        >
          <X className="h-3 w-3" /> Clear
        </button>
      )}
    </div>
  );
}
