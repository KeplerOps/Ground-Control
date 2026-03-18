import { useProjectContext } from "@/contexts/project-context";
import { apiFetch } from "@/lib/api-client";
import {
  type ColorScheme,
  type LayoutId,
  PRIORITY_COLORS,
  RELATION_STYLES,
  STATUS_COLORS,
  getColorMap,
  getNodeColor,
  getSeries,
} from "@/lib/graph-constants";
import { cn } from "@/lib/utils";
import type cytoscape from "cytoscape";
import { Loader2, Maximize, RotateCcw } from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

interface RequirementData {
  id: string;
  uid: string;
  title: string;
  statement: string;
  priority: string;
  status: string;
  requirementType: string;
  wave: number;
}

interface RelationData {
  id: string;
  sourceId: string;
  targetId: string;
  relationType: string;
}

interface PagedResponse {
  content: RequirementData[];
  totalPages: number;
}

type CytoscapeInstance = cytoscape.Core;

export function Graph() {
  const { activeProject } = useProjectContext();
  const containerRef = useRef<HTMLDivElement>(null);
  const cyRef = useRef<CytoscapeInstance | null>(null);
  const tooltipRef = useRef<HTMLDivElement>(null);

  const [colorScheme, setColorScheme] = useState<ColorScheme>("status");
  const [layoutId, setLayoutId] = useState<LayoutId>("dagre-tb");
  const [loading, setLoading] = useState(true);
  const [loadMsg, setLoadMsg] = useState("Fetching requirements...");
  const [error, setError] = useState<string | null>(null);

  const [requirements, setRequirements] = useState<RequirementData[]>([]);
  const [relations, setRelations] = useState<RelationData[]>([]);

  const fetchData = useCallback(async () => {
    if (!activeProject) return;
    setLoading(true);
    setError(null);

    try {
      const reqs: RequirementData[] = [];
      let page = 0;
      let totalPages = 1;
      while (page < totalPages) {
        const data = await apiFetch<PagedResponse>("/requirements", {
          params: {
            project: activeProject.identifier,
            page: String(page),
            size: "200",
          },
        });
        reqs.push(...data.content);
        totalPages = data.totalPages;
        page++;
      }
      setRequirements(reqs);
      setLoadMsg(`Loaded ${reqs.length} requirements. Fetching relations...`);

      const allRels = new Map<string, RelationData>();
      const batchSize = 20;
      for (let i = 0; i < reqs.length; i += batchSize) {
        const batch = reqs.slice(i, i + batchSize);
        const results = await Promise.all(
          batch.map((r) =>
            apiFetch<RelationData[]>(`/requirements/${r.id}/relations`, {
              params: { project: activeProject.identifier },
            }),
          ),
        );
        for (const rels of results) {
          for (const rel of rels) {
            allRels.set(rel.id, rel);
          }
        }
        setLoadMsg(
          `Fetching relations... ${Math.min(i + batchSize, reqs.length)}/${reqs.length}`,
        );
      }
      setRelations(Array.from(allRels.values()));
      setLoading(false);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load graph data",
      );
      setLoading(false);
    }
  }, [activeProject]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const stats = useMemo(() => {
    const waves: Record<number, number> = {};
    const seriesSet = new Set<string>();
    for (const r of requirements) {
      const w = r.wave || 0;
      waves[w] = (waves[w] ?? 0) + 1;
      seriesSet.add(getSeries(r.uid));
    }
    const waveKeys = Object.keys(waves)
      .map(Number)
      .sort((a, b) => a - b);
    const waveStr = waveKeys.map((w) => `W${w}:${waves[w]}`).join(" ");
    return {
      requirements: requirements.length,
      relations: relations.length,
      series: seriesSet.size,
      waves: waveKeys.length,
      waveStr,
    };
  }, [requirements, relations]);

  const legendItems = useMemo(() => {
    const counts: Record<string, number> = {};
    for (const r of requirements) {
      let key: string;
      switch (colorScheme) {
        case "priority":
          key = r.priority;
          break;
        case "status":
          key = r.status;
          break;
        case "wave":
          key = `Wave ${r.wave || 0}`;
          break;
        default:
          key = getSeries(r.uid);
      }
      counts[key] = (counts[key] ?? 0) + 1;
    }
    const colorMap = getColorMap(colorScheme);
    return Object.keys(counts)
      .sort()
      .map((key) => ({
        key,
        count: counts[key] ?? 0,
        color: colorMap[key] ?? "#555",
      }));
  }, [requirements, colorScheme]);

  const relationLegend = useMemo(() => {
    const counts: Record<string, number> = {};
    for (const r of relations) {
      counts[r.relationType] = (counts[r.relationType] ?? 0) + 1;
    }
    return Object.entries(RELATION_STYLES)
      .filter(([type]) => (counts[type] ?? 0) > 0)
      .map(([type, style]) => ({
        type,
        ...style,
        count: counts[type] ?? 0,
      }));
  }, [relations]);

  // Build tooltip content using safe DOM methods
  const populateTooltip = useCallback(
    (container: HTMLDivElement, d: Record<string, unknown>) => {
      container.replaceChildren();

      const uidDiv = document.createElement("div");
      uidDiv.style.cssText =
        "font-size:11px;color:#6c7ee1;font-weight:600;margin-bottom:4px";
      uidDiv.textContent = String(d.uid ?? "");
      container.appendChild(uidDiv);

      const titleDiv = document.createElement("div");
      titleDiv.style.cssText = "font-weight:600;margin-bottom:6px";
      titleDiv.textContent = String(d.title ?? "");
      container.appendChild(titleDiv);

      const metaDiv = document.createElement("div");
      metaDiv.style.cssText =
        "display:flex;gap:8px;margin-bottom:6px;flex-wrap:wrap";

      const tags = [
        {
          text: String(d.priority ?? ""),
          bg: PRIORITY_COLORS[String(d.priority)] ?? "#555",
        },
        {
          text: String(d.status ?? ""),
          bg: STATUS_COLORS[String(d.status)] ?? "#555",
        },
        { text: `Wave ${Number(d.wave ?? 0)}`, bg: "#6c7ee1" },
        { text: String(d.type ?? ""), bg: "#4ecdc4" },
      ];
      for (const t of tags) {
        const span = document.createElement("span");
        span.style.cssText = `display:inline-block;padding:1px 6px;border-radius:3px;font-size:10px;font-weight:600;background:${t.bg}33;color:${t.bg}`;
        span.textContent = t.text;
        metaDiv.appendChild(span);
      }
      container.appendChild(metaDiv);

      const statement = String(d.statement ?? "");
      if (statement) {
        const stmtDiv = document.createElement("div");
        stmtDiv.style.cssText =
          "color:#a1a1aa;font-size:11px;line-height:1.4;max-height:80px;overflow:hidden";
        stmtDiv.textContent = statement;
        container.appendChild(stmtDiv);
      }
    },
    [],
  );

  useEffect(() => {
    if (loading || !containerRef.current || requirements.length === 0) return;

    let cancelled = false;

    async function initCytoscape() {
      const cytoscapeModule = await import("cytoscape");
      const cytoscape = cytoscapeModule.default;
      const dagreModule = await import("cytoscape-dagre");
      // cytoscape-dagre exports differ between ESM/CJS
      const cytoscapeDagre =
        "default" in dagreModule
          ? (dagreModule.default as (cy: typeof cytoscape) => void)
          : (dagreModule as unknown as (cy: typeof cytoscape) => void);
      cytoscapeDagre(cytoscape);

      if (cancelled) return;

      const nodes = requirements.map((r) => ({
        data: {
          id: r.id,
          uid: r.uid,
          label: r.uid.replace("GC-", ""),
          title: r.title,
          statement: r.statement,
          priority: r.priority,
          status: r.status,
          type: r.requirementType,
          wave: r.wave || 0,
          series: getSeries(r.uid),
          color: getNodeColor(r, colorScheme),
        },
      }));

      const edges = relations.map((rel) => {
        const style =
          RELATION_STYLES[rel.relationType] ?? RELATION_STYLES.RELATED;
        return {
          data: {
            id: `e-${rel.id}`,
            source: rel.sourceId,
            target: rel.targetId,
            relType: rel.relationType,
            color: style?.color ?? "#95a5a6",
            lineStyle: style?.style ?? "dotted",
          },
        };
      });

      if (cyRef.current) {
        cyRef.current.destroy();
      }

      const layoutConfig =
        layoutId === "dagre-tb"
          ? {
              name: "dagre",
              rankDir: "BT",
              nodeSep: 30,
              rankSep: 60,
              edgeSep: 10,
            }
          : {
              name: "dagre",
              rankDir: "RL",
              nodeSep: 30,
              rankSep: 60,
              edgeSep: 10,
            };

      const cy = cytoscape({
        container: containerRef.current,
        elements: [...nodes, ...edges],
        style: [
          {
            selector: "node",
            style: {
              label: "data(label)",
              "background-color": "data(color)",
              color: "#e1e4ed",
              "text-valign": "center",
              "text-halign": "center",
              "font-size": "9px",
              "font-weight": 600,
              width: 50,
              height: 26,
              shape: "round-rectangle",
              "border-width": 1,
              "border-color": "data(color)",
              "text-outline-width": 0,
              "overlay-padding": 3,
            },
          },
          {
            selector: "node:selected",
            style: { "border-width": 2, "border-color": "#fff" },
          },
          {
            selector: "node.highlighted",
            style: {
              "border-width": 2,
              "border-color": "#fff",
              "z-index": 10,
            },
          },
          {
            selector: "node.dimmed",
            style: { opacity: 0.15 },
          },
          {
            selector: "edge",
            style: {
              width: 1.2,
              "line-color": "data(color)",
              "target-arrow-color": "data(color)",
              "target-arrow-shape": "triangle",
              "arrow-scale": 0.7,
              "curve-style": "bezier",
              "line-style": "data(lineStyle)" as unknown as
                | "solid"
                | "dashed"
                | "dotted",
              opacity: 0.6,
            },
          },
          {
            selector: "edge.highlighted",
            style: { width: 2.5, opacity: 1, "z-index": 10 },
          },
          {
            selector: "edge.dimmed",
            style: { opacity: 0.06 },
          },
        ],
        layout: layoutConfig,
        minZoom: 0.15,
        maxZoom: 4,
        wheelSensitivity: 0.3,
      });

      cyRef.current = cy;

      const tooltip = tooltipRef.current;
      if (!tooltip) return;

      cy.on("mouseover", "node", (evt) => {
        populateTooltip(tooltip, evt.target.data());
        tooltip.style.display = "block";
      });

      cy.on("mousemove", "node", (evt) => {
        const x = evt.originalEvent.clientX;
        const y = evt.originalEvent.clientY;
        const pad = 12;
        let left = x + pad;
        let top = y + pad;
        if (left + 360 > window.innerWidth) left = x - 360 - pad;
        if (top + 200 > window.innerHeight) top = y - 200 - pad;
        tooltip.style.left = `${left}px`;
        tooltip.style.top = `${top}px`;
      });

      cy.on("mouseout", "node", () => {
        tooltip.style.display = "none";
      });

      cy.on("tap", "node", (evt) => {
        const node = evt.target;
        if (node.hasClass("highlighted")) {
          cy.elements().removeClass("highlighted dimmed");
          return;
        }
        const neighborhood = node.closedNeighborhood();
        cy.elements().removeClass("highlighted dimmed");
        cy.elements().not(neighborhood).addClass("dimmed");
        neighborhood.addClass("highlighted");
      });

      cy.on("tap", (evt) => {
        if (evt.target === cy) {
          cy.elements().removeClass("highlighted dimmed");
        }
      });
    }

    initCytoscape();

    return () => {
      cancelled = true;
      if (cyRef.current) {
        cyRef.current.destroy();
        cyRef.current = null;
      }
    };
  }, [
    loading,
    requirements,
    relations,
    colorScheme,
    layoutId,
    populateTooltip,
  ]);

  function handleFit() {
    cyRef.current?.fit(undefined, 30);
  }

  function handleReset() {
    cyRef.current?.elements().removeClass("highlighted dimmed");
    cyRef.current?.fit(undefined, 30);
  }

  function handleLegendClick(key: string) {
    const cy = cyRef.current;
    if (!cy) return;

    cy.elements().removeClass("highlighted dimmed");
    const matchNodes = cy.nodes().filter((n) => {
      switch (colorScheme) {
        case "priority":
          return n.data("priority") === key;
        case "status":
          return n.data("status") === key;
        case "wave":
          return `Wave ${n.data("wave")}` === key;
        default:
          return n.data("series") === key;
      }
    });
    if (matchNodes.length === 0) return;
    const neighborhood = matchNodes.union(matchNodes.connectedEdges());
    cy.elements().not(neighborhood).addClass("dimmed");
    neighborhood.addClass("highlighted");
  }

  if (!activeProject) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20 text-center">
        <h1 className="text-2xl font-semibold">Graph View</h1>
        <p className="text-muted-foreground">
          Select a project to view the requirement graph.
        </p>
      </div>
    );
  }

  return (
    <div className="flex h-[calc(100vh-3.5rem)] flex-col">
      {/* Controls */}
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
          <label
            htmlFor="graph-layout"
            className="text-xs text-muted-foreground"
          >
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
          </select>
        </div>
        <button
          type="button"
          onClick={handleFit}
          className="flex items-center gap-1 rounded border border-input bg-background px-2 py-1 text-xs hover:border-primary"
          title="Fit to screen"
        >
          <Maximize className="h-3 w-3" /> Fit
        </button>
        <button
          type="button"
          onClick={handleReset}
          className="flex items-center gap-1 rounded border border-input bg-background px-2 py-1 text-xs hover:border-primary"
          title="Reset filters"
        >
          <RotateCcw className="h-3 w-3" /> Reset
        </button>
      </div>

      {/* Stats */}
      {!loading && (
        <div className="flex gap-4 border-b border-border bg-card px-4 py-1.5 text-[11px]">
          <span className="text-muted-foreground">
            <strong className="text-foreground text-[13px] mr-0.5">
              {stats.requirements}
            </strong>
            requirements
          </span>
          <span className="text-muted-foreground">
            <strong className="text-foreground text-[13px] mr-0.5">
              {stats.relations}
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
      )}

      {/* Legend */}
      {!loading && (
        <div className="flex flex-wrap gap-3 border-b border-border bg-card px-4 py-1.5">
          {legendItems.map((item) => (
            <button
              type="button"
              key={item.key}
              onClick={() => handleLegendClick(item.key)}
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
              <span className="ml-2 text-[11px] text-muted-foreground">
                Edges:
              </span>
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
      )}

      {/* Graph canvas */}
      <div className="relative flex-1">
        {loading && (
          <div className="absolute inset-0 z-10 flex flex-col items-center justify-center gap-3 bg-background">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
            <span className="text-sm text-muted-foreground">{loadMsg}</span>
          </div>
        )}
        {error && (
          <div className="absolute inset-0 z-10 flex flex-col items-center justify-center gap-3 bg-background">
            <span className="text-sm text-destructive">{error}</span>
            <span className="text-xs text-muted-foreground">
              Is the backend running?
            </span>
          </div>
        )}
        <div ref={containerRef} className="h-full w-full" />
      </div>

      {/* Tooltip */}
      <div
        ref={tooltipRef}
        className="pointer-events-none fixed z-[100] hidden max-w-[360px] rounded-md border border-border bg-card p-3 text-xs shadow-lg"
      />
    </div>
  );
}
