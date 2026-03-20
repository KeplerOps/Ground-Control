import { AppLayout } from "@/components/layout/app-layout";
import { ProjectProvider } from "@/contexts/project-context";
import { useProjects } from "@/hooks/use-projects";
import { Admin } from "@/pages/admin";
import { Analysis } from "@/pages/analysis";
import { Dashboard } from "@/pages/dashboard";
import { Graph } from "@/pages/graph";
import { Projects } from "@/pages/projects";
import { RequirementDetail } from "@/pages/requirement-detail";
import { Requirements } from "@/pages/requirements";
import { Link, Navigate, Route, Routes } from "react-router-dom";

function NotFound() {
  return (
    <div className="py-20 text-center text-muted-foreground">
      <p>Page not found.</p>
      <Link to="/projects" className="mt-4 inline-block text-primary underline">
        Back to projects
      </Link>
    </div>
  );
}

function RootRedirect() {
  const { data: projects = [], isLoading } = useProjects();

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-muted border-t-primary" />
      </div>
    );
  }

  const first = projects[0];
  if (first) {
    return <Navigate to={`/p/${first.identifier}/`} replace />;
  }

  return <Navigate to="/projects" replace />;
}

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<RootRedirect />} />
      <Route element={<AppLayout />}>
        <Route path="projects" element={<Projects />} />
      </Route>
      <Route
        path="p/:projectId"
        element={
          <ProjectProvider>
            <AppLayout />
          </ProjectProvider>
        }
      >
        <Route index element={<Dashboard />} />
        <Route path="requirements" element={<Requirements />} />
        <Route path="requirements/:id" element={<RequirementDetail />} />
        <Route path="graph" element={<Graph />} />
        <Route path="analysis" element={<Analysis />} />
        <Route path="admin" element={<Admin />} />
        <Route path="*" element={<NotFound />} />
      </Route>
      <Route element={<AppLayout />}>
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
