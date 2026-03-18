import { useProjectContext } from "@/contexts/project-context";
import {
  useCoverageGaps,
  useCrossWave,
  useCycles,
  useOrphans,
} from "@/hooks/use-analysis";
import { useRequirements } from "@/hooks/use-requirements";
import { cn } from "@/lib/utils";
import {
  AlertTriangle,
  ArrowRight,
  GitFork,
  Layers,
  Link2Off,
  Rocket,
  Unlink,
} from "lucide-react";
import { useNavigate } from "react-router-dom";

export function Dashboard() {
  const { activeProject, isLoading } = useProjectContext();
  const navigate = useNavigate();

  if (isLoading) return <LoadingSkeleton />;

  if (!activeProject) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20 text-center">
        <Rocket className="h-12 w-12 text-muted-foreground" />
        <h1 className="text-2xl font-semibold">Welcome to Ground Control</h1>
        <p className="text-muted-foreground">
          Select a project from the header to get started.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">{activeProject.name}</h1>
        {activeProject.description && (
          <p className="mt-1 text-muted-foreground">
            {activeProject.description}
          </p>
        )}
      </div>

      <RequirementStats />
      <AnalysisAlerts navigate={navigate} />
    </div>
  );
}

function RequirementStats() {
  const draftQ = useRequirements({ status: "DRAFT", size: 0 });
  const activeQ = useRequirements({ status: "ACTIVE", size: 0 });
  const deprecatedQ = useRequirements({ status: "DEPRECATED", size: 0 });
  const archivedQ = useRequirements({ status: "ARCHIVED", size: 0 });
  const allQ = useRequirements({ size: 0 });

  const stats = [
    {
      label: "Total",
      value: allQ.data?.totalElements ?? "-",
      color: "text-foreground",
    },
    {
      label: "Draft",
      value: draftQ.data?.totalElements ?? "-",
      color: "text-gray-400",
    },
    {
      label: "Active",
      value: activeQ.data?.totalElements ?? "-",
      color: "text-green-400",
    },
    {
      label: "Deprecated",
      value: deprecatedQ.data?.totalElements ?? "-",
      color: "text-orange-400",
    },
    {
      label: "Archived",
      value: archivedQ.data?.totalElements ?? "-",
      color: "text-gray-500",
    },
  ];

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
      {stats.map((s) => (
        <div
          key={s.label}
          className="rounded-lg border border-border bg-card p-4"
        >
          <p className="text-sm text-muted-foreground">{s.label}</p>
          <p className={cn("mt-1 text-2xl font-semibold", s.color)}>
            {s.value}
          </p>
        </div>
      ))}
    </div>
  );
}

function AnalysisAlerts({
  navigate,
}: {
  navigate: (path: string) => void;
}) {
  const { data: cycles } = useCycles();
  const { data: orphans } = useOrphans();
  const { data: coverageGaps } = useCoverageGaps("IMPLEMENTS");
  const { data: crossWave } = useCrossWave();

  const alerts = [
    {
      icon: GitFork,
      label: "Dependency Cycles",
      count: cycles?.length ?? 0,
      color: "text-red-400",
      bgColor: "border-red-500/20 bg-red-500/5",
      path: "/analysis",
    },
    {
      icon: Unlink,
      label: "Orphan Requirements",
      count: orphans?.length ?? 0,
      color: "text-yellow-400",
      bgColor: "border-yellow-500/20 bg-yellow-500/5",
      path: "/analysis",
    },
    {
      icon: Link2Off,
      label: "Missing IMPLEMENTS Links",
      count: coverageGaps?.length ?? 0,
      color: "text-orange-400",
      bgColor: "border-orange-500/20 bg-orange-500/5",
      path: "/analysis",
    },
    {
      icon: Layers,
      label: "Cross-Wave Violations",
      count: crossWave?.length ?? 0,
      color: "text-violet-400",
      bgColor: "border-violet-500/20 bg-violet-500/5",
      path: "/analysis",
    },
  ];

  const hasAlerts = alerts.some((a) => a.count > 0);

  if (!hasAlerts) {
    return (
      <div className="rounded-lg border border-green-500/20 bg-green-500/5 p-6 text-center">
        <p className="text-green-400 font-medium">
          All clear — no analysis issues detected.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <h2 className="text-lg font-medium flex items-center gap-2">
        <AlertTriangle className="h-5 w-5 text-yellow-400" />
        Analysis Alerts
      </h2>
      <div className="grid gap-3 sm:grid-cols-2">
        {alerts
          .filter((a) => a.count > 0)
          .map((alert) => (
            <button
              key={alert.label}
              type="button"
              className={cn(
                "flex items-center gap-4 rounded-lg border p-4 text-left transition-colors hover:bg-accent/30",
                alert.bgColor,
              )}
              onClick={() => navigate(alert.path)}
            >
              <alert.icon className={cn("h-8 w-8", alert.color)} />
              <div className="flex-1">
                <p className="text-sm font-medium">{alert.label}</p>
                <p className={cn("text-2xl font-semibold", alert.color)}>
                  {alert.count}
                </p>
              </div>
              <ArrowRight className="h-4 w-4 text-muted-foreground" />
            </button>
          ))}
      </div>
    </div>
  );
}

function LoadingSkeleton() {
  return (
    <div className="space-y-6">
      <div className="h-8 w-48 animate-pulse rounded bg-muted" />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        {["s1", "s2", "s3", "s4", "s5"].map((key) => (
          <div
            key={key}
            className="h-20 animate-pulse rounded-lg border border-border bg-card"
          />
        ))}
      </div>
    </div>
  );
}
