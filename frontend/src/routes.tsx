import { AppLayout } from "@/components/layout/app-layout";
import { Admin } from "@/pages/admin";
import { Analysis } from "@/pages/analysis";
import { Dashboard } from "@/pages/dashboard";
import { Graph } from "@/pages/graph";
import { Projects } from "@/pages/projects";
import { RequirementDetail } from "@/pages/requirement-detail";
import { Requirements } from "@/pages/requirements";
import { Route, Routes } from "react-router-dom";

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<Dashboard />} />
        <Route path="requirements" element={<Requirements />} />
        <Route path="requirements/:id" element={<RequirementDetail />} />
        <Route path="graph" element={<Graph />} />
        <Route path="analysis" element={<Analysis />} />
        <Route path="projects" element={<Projects />} />
        <Route path="admin" element={<Admin />} />
      </Route>
    </Routes>
  );
}
