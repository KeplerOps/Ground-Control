import { useWorkspace } from "@/contexts/workspace-context";
import { useWorkspaces } from "@/hooks/use-workspaces";
import { cn } from "@/lib/utils";
import * as DropdownMenu from "@radix-ui/react-dropdown-menu";
import {
  ChevronDown,
  Cog,
  LayoutDashboard,
  ListChecks,
  Play,
  Rocket,
  Workflow,
} from "lucide-react";
import { Link, NavLink, Outlet, useNavigate, useParams } from "react-router-dom";

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
          "flex items-center gap-1.5 px-3 py-1.5 rounded-md text-sm font-medium transition-colors",
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

function WorkspaceSwitcher() {
  const { data: workspaces = [] } = useWorkspaces();
  const { workspace } = useWorkspace();
  const navigate = useNavigate();

  if (!workspace) return null;

  return (
    <DropdownMenu.Root>
      <DropdownMenu.Trigger className="flex items-center gap-1.5 rounded-md border border-border bg-card px-3 py-1.5 text-sm font-medium hover:bg-accent/50">
        {workspace.name}
        <ChevronDown className="h-3.5 w-3.5 text-muted-foreground" />
      </DropdownMenu.Trigger>
      <DropdownMenu.Portal>
        <DropdownMenu.Content
          className="min-w-[180px] rounded-md border border-border bg-card p-1 shadow-lg"
          sideOffset={4}
          align="end"
        >
          {workspaces.map((w) => (
            <DropdownMenu.Item
              key={w.id}
              className={cn(
                "flex cursor-pointer items-center rounded-sm px-2 py-1.5 text-sm outline-none hover:bg-accent",
                w.id === workspace.id && "font-semibold",
              )}
              onSelect={() => navigate(`/w/${w.identifier}/`)}
            >
              {w.name}
            </DropdownMenu.Item>
          ))}
          <DropdownMenu.Separator className="my-1 h-px bg-border" />
          <DropdownMenu.Item
            className="flex cursor-pointer items-center rounded-sm px-2 py-1.5 text-sm outline-none hover:bg-accent"
            onSelect={() => navigate("/workspaces")}
          >
            Manage workspaces
          </DropdownMenu.Item>
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  );
}

export function AppLayout() {
  const { workspaceId } = useParams<{ workspaceId: string }>();

  const base = workspaceId ? `/w/${workspaceId}` : "";

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b border-border bg-card">
        <div className="mx-auto flex h-14 max-w-7xl items-center gap-6 px-4">
          <Link
            to={workspaceId ? `${base}/` : "/"}
            className="flex items-center gap-2 font-semibold"
          >
            <Rocket className="h-5 w-5 text-primary" />
            <span>Ground Control</span>
          </Link>

          <nav className="flex items-center gap-1">
            {workspaceId && (
              <>
                <NavItem to={`${base}/`} end>
                  <LayoutDashboard className="h-4 w-4" />
                  Dashboard
                </NavItem>
                <NavItem to={`${base}/workflows`}>
                  <Workflow className="h-4 w-4" />
                  Workflows
                </NavItem>
                <NavItem to={`${base}/executions`}>
                  <Play className="h-4 w-4" />
                  Executions
                </NavItem>
                <NavItem to={`${base}/settings`}>
                  <Cog className="h-4 w-4" />
                  Settings
                </NavItem>
              </>
            )}
            <NavItem to="/workspaces">
              <ListChecks className="h-4 w-4" />
              Workspaces
            </NavItem>
          </nav>

          <div className="ml-auto">
            {workspaceId && <WorkspaceSwitcher />}
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}
