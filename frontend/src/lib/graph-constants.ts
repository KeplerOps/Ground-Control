export const SERIES_COLORS: Record<string, string> = {
  A: "#6c7ee1",
  B: "#4ecdc4",
  C: "#f7b731",
  D: "#e056a0",
  E: "#45b7d1",
  F: "#96c93d",
  G: "#ff6b6b",
  H: "#c56cf0",
  I: "#786fa6",
  J: "#f19066",
  K: "#e77f67",
  L: "#574b90",
  M: "#e15f41",
  N: "#3dc1d3",
  O: "#ea8685",
  P: "#778beb",
  Q: "#cf6a87",
  R: "#63cdda",
  S: "#f8a5c2",
  T: "#f3a683",
  U: "#c44569",
  V: "#546de5",
  W: "#e66767",
  X: "#303952",
};

export const PRIORITY_COLORS: Record<string, string> = {
  MUST: "#e74c3c",
  SHOULD: "#f39c12",
  COULD: "#3498db",
  WONT: "#7f8c8d",
};

export const STATUS_COLORS: Record<string, string> = {
  DRAFT: "#7f8c8d",
  ACTIVE: "#2ecc71",
  DEPRECATED: "#e67e22",
  ARCHIVED: "#95a5a6",
};

export const WAVE_COLORS: Record<string, string> = {
  1: "#e74c3c",
  2: "#e67e22",
  3: "#f1c40f",
  4: "#2ecc71",
  5: "#3498db",
  6: "#9b59b6",
  7: "#1abc9c",
  8: "#e91e63",
  9: "#00bcd4",
};

export const RELATION_STYLES: Record<
  string,
  { color: string; style: string; label: string }
> = {
  DEPENDS_ON: { color: "#6c7ee1", style: "solid", label: "depends" },
  PARENT: { color: "#4ecdc4", style: "solid", label: "parent" },
  REFINES: { color: "#f7b731", style: "dashed", label: "refines" },
  CONFLICTS_WITH: { color: "#e74c3c", style: "dotted", label: "conflicts" },
  SUPERSEDES: { color: "#c56cf0", style: "dashed", label: "supersedes" },
  RELATED: { color: "#95a5a6", style: "dotted", label: "related" },
};

export type ColorScheme = "series" | "priority" | "status" | "wave";
export type LayoutId = "dagre-lr" | "dagre-tb";

export function getSeries(uid: string): string {
  const m = uid.match(/^GC-([A-Z])/);
  return m?.[1] ?? "?";
}

export function getNodeColor(
  req: { uid: string; priority: string; status: string; wave: number },
  scheme: ColorScheme,
): string {
  const series = getSeries(req.uid);
  switch (scheme) {
    case "priority":
      return PRIORITY_COLORS[req.priority] ?? "#555";
    case "status":
      return STATUS_COLORS[req.status] ?? "#555";
    case "wave":
      return WAVE_COLORS[String(req.wave)] ?? "#555";
    default:
      return SERIES_COLORS[series] ?? "#555";
  }
}

export function getColorMap(scheme: ColorScheme): Record<string, string> {
  switch (scheme) {
    case "priority":
      return PRIORITY_COLORS;
    case "status":
      return STATUS_COLORS;
    case "wave": {
      const map: Record<string, string> = {};
      for (const [k, v] of Object.entries(WAVE_COLORS)) {
        map[`Wave ${k}`] = v;
      }
      return map;
    }
    default:
      return SERIES_COLORS;
  }
}
