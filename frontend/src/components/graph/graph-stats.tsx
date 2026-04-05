export interface GraphStatsData {
  requirements: number;
  relations: number;
  series: number;
  waves: number;
  waveStr: string;
}

export function GraphStats({
  stats,
  hasFilters,
  filteredRequirementsCount,
  filteredRelationsCount,
}: {
  stats: GraphStatsData;
  hasFilters: boolean;
  filteredRequirementsCount: number;
  filteredRelationsCount: number;
}) {
  return (
    <div className="flex gap-4 border-b border-border bg-card px-4 py-1.5 text-[11px]">
      <span className="text-muted-foreground">
        <strong className="text-foreground text-[13px] mr-0.5">
          {hasFilters
            ? `${filteredRequirementsCount} of ${stats.requirements}`
            : stats.requirements}
        </strong>
        requirements
      </span>
      <span className="text-muted-foreground">
        <strong className="text-foreground text-[13px] mr-0.5">
          {hasFilters
            ? `${filteredRelationsCount} of ${stats.relations}`
            : stats.relations}
        </strong>
        relations
      </span>
      <span className="text-muted-foreground">
        <strong className="text-foreground text-[13px] mr-0.5">
          {stats.series}
        </strong>
        series
      </span>
      <span className="text-muted-foreground">
        <strong className="text-foreground text-[13px] mr-0.5">
          {stats.waves}
        </strong>
        waves
      </span>
      <span className="text-muted-foreground">{stats.waveStr}</span>
    </div>
  );
}
