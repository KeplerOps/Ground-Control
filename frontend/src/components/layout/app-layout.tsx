import { ProjectSwitcher } from "@/components/project-switcher";
import { cn } from "@/lib/utils";
import { Rocket } from "lucide-react";
import {
  Link,
  NavLink,
  Outlet,
  useLocation,
  useParams,
} from "react-router-dom";

function NavItem({
  to,
  children,
  end,
}: {
  to: string;
  children: React.ReactNode;
  end?: boolean;
}) {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) =>
        cn(
          "px-3 py-1.5 rounded-md text-sm font-medium transition-colors",
          isActive
            ? "bg-accent text-accent-foreground"
            : "text-muted-foreground hover:text-foreground hover:bg-accent/50",
        )
      }
    >
      {children}
    </NavLink>
  );
}

export function AppLayout() {
  const { projectId } = useParams<{ projectId: string }>();
  const location = useLocation();
  const isFullBleed = location.pathname.endsWith("/graph");

  const base = projectId ? `/p/${projectId}` : "";

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b border-border bg-card">
        <div className="mx-auto flex h-14 max-w-7xl items-center gap-6 px-4">
          <Link
            to={projectId ? `${base}/` : "/"}
            className="flex items-center gap-2 font-semibold"
          >
            <Rocket className="h-5 w-5 text-primary" />
            <span>Ground Control</span>
          </Link>

          <nav className="flex items-center gap-1">
            {projectId && (
              <>
                <NavItem to={`${base}/`} end>
                  Dashboard
                </NavItem>
                <NavItem to={`${base}/requirements`}>Requirements</NavItem>
                <NavItem to={`${base}/graph`}>Graph</NavItem>
                <NavItem to={`${base}/analysis`}>Analysis</NavItem>
              </>
            )}
            <NavItem to="/projects">Projects</NavItem>
            {projectId && <NavItem to={`${base}/admin`}>Admin</NavItem>}
          </nav>

          <div className="ml-auto">{projectId && <ProjectSwitcher />}</div>
        </div>
      </header>

      <main className={cn(isFullBleed ? "" : "mx-auto max-w-7xl px-4 py-6")}>
        <Outlet />
      </main>
    </div>
  );
}
