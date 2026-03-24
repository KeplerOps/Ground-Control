import { useWorkspaces } from "@/hooks/use-workspaces";
import type { Workspace } from "@/types/api";
import { type ReactNode, createContext, useContext, useMemo } from "react";
import { useParams } from "react-router-dom";

interface WorkspaceContextValue {
  workspace: Workspace | undefined;
  isLoading: boolean;
}

const WorkspaceContext = createContext<WorkspaceContextValue>({
  workspace: undefined,
  isLoading: true,
});

export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const { workspaceId } = useParams<{ workspaceId: string }>();
  const { data: workspaces = [], isLoading } = useWorkspaces();
  const workspace = useMemo(
    () => workspaces.find((w) => w.identifier === workspaceId),
    [workspaces, workspaceId],
  );

  return (
    <WorkspaceContext.Provider value={{ workspace, isLoading }}>
      {children}
    </WorkspaceContext.Provider>
  );
}

export function useWorkspace() {
  return useContext(WorkspaceContext);
}
