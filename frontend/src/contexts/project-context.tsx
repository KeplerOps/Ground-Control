import type { ProjectResponse } from "@/hooks/use-projects";
import { useProjects } from "@/hooks/use-projects";
import { queryClient } from "@/lib/query-client";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

const STORAGE_KEY = "gc-active-project";

interface ProjectContextValue {
  projects: ProjectResponse[];
  activeProject: ProjectResponse | null;
  setActiveProject: (project: ProjectResponse) => void;
  isLoading: boolean;
}

const ProjectContext = createContext<ProjectContextValue | null>(null);

export function ProjectProvider({ children }: { children: React.ReactNode }) {
  const { data: projects = [], isLoading } = useProjects();
  const [activeProject, setActiveProjectState] =
    useState<ProjectResponse | null>(null);

  const setActiveProject = useCallback((project: ProjectResponse) => {
    setActiveProjectState(project);
    localStorage.setItem(STORAGE_KEY, project.identifier);
    queryClient.invalidateQueries({
      predicate: (query) => {
        const key = query.queryKey[0];
        return key !== "projects";
      },
    });
  }, []);

  useEffect(() => {
    if (projects.length === 0) return;

    const stored = localStorage.getItem(STORAGE_KEY);
    const match = projects.find((p) => p.identifier === stored);

    if (match) {
      setActiveProjectState(match);
    } else if (projects.length === 1) {
      const only = projects[0];
      if (only) {
        setActiveProjectState(only);
        localStorage.setItem(STORAGE_KEY, only.identifier);
      }
    }
  }, [projects]);

  const value = useMemo(
    () => ({
      projects,
      activeProject,
      setActiveProject,
      isLoading,
    }),
    [projects, activeProject, setActiveProject, isLoading],
  );

  return (
    <ProjectContext.Provider value={value}>{children}</ProjectContext.Provider>
  );
}

export function useProjectContext() {
  const context = useContext(ProjectContext);
  if (!context) {
    throw new Error("useProjectContext must be used within a ProjectProvider");
  }
  return context;
}
