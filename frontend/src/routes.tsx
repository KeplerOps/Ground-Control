import { AppLayout } from "@/components/layout/app-layout";
import { WorkspaceProvider } from "@/contexts/workspace-context";
import { useWorkspaces } from "@/hooks/use-workspaces";
import { Dashboard } from "@/pages/dashboard";
import { ExecutionDetail } from "@/pages/execution-detail";
import { Executions } from "@/pages/executions";
import { Settings } from "@/pages/settings";
import { WorkflowDetail } from "@/pages/workflow-detail";
import { Workflows } from "@/pages/workflows";
import { Workspaces } from "@/pages/workspaces";
import { Link, Navigate, Route, Routes } from "react-router-dom";

function NotFound() {
  return (
    <div className="py-20 text-center text-muted-foreground">
      <p>Page not found.</p>
      <Link
        to="/workspaces"
        className="mt-4 inline-block text-primary underline"
      >
        Back to workspaces
      </Link>
    </div>
  );
}

function RootRedirect() {
  const { data: workspaces = [], isLoading } = useWorkspaces();

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-muted border-t-primary" />
      </div>
    );
  }

  const first = workspaces[0];
  if (first) {
    return <Navigate to={`/w/${first.identifier}/`} replace />;
  }

  return <Navigate to="/workspaces" replace />;
}

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<RootRedirect />} />
      <Route element={<AppLayout />}>
        <Route path="workspaces" element={<Workspaces />} />
      </Route>
      <Route
        path="w/:workspaceId"
        element={
          <WorkspaceProvider>
            <AppLayout />
          </WorkspaceProvider>
        }
      >
        <Route index element={<Dashboard />} />
        <Route path="workflows" element={<Workflows />} />
        <Route path="workflows/:id" element={<WorkflowDetail />} />
        <Route path="executions" element={<Executions />} />
        <Route path="executions/:id" element={<ExecutionDetail />} />
        <Route path="settings" element={<Settings />} />
        <Route path="*" element={<NotFound />} />
      </Route>
      <Route element={<AppLayout />}>
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
