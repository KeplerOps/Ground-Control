import { ToastProvider } from "@/components/ui/toast";
import { ProjectProvider } from "@/contexts/project-context";
import { queryClient } from "@/lib/query-client";
import { AppRoutes } from "@/routes";
import { QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ProjectProvider>
        <ToastProvider>
          <BrowserRouter>
            <AppRoutes />
          </BrowserRouter>
        </ToastProvider>
      </ProjectProvider>
    </QueryClientProvider>
  );
}
