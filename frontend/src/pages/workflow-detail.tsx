import { StatusBadge } from "@/components/status-badge";
import { Modal } from "@/components/ui/modal";
import { useWorkspace } from "@/contexts/workspace-context";
import {
  useExecuteWorkflow,
  usePublishWorkflow,
  useWorkflow,
  useWorkflowEdges,
  useWorkflowNodes,
} from "@/hooks/use-workflows";
import { api } from "@/lib/api-client";
import { cn } from "@/lib/utils";
import type { NodeType, WorkflowEdge, WorkflowNode } from "@/types/api";
import {
  ArrowLeft,
  Circle,
  GitBranch,
  Loader2,
  Play,
  Plus,
  Rocket,
  Trash2,
  Upload,
} from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";

const NODE_TYPE_OPTIONS: NodeType[] = [
  "SHELL",
  "HTTP",
  "DOCKER",
  "SCRIPT",
  "CONDITIONAL",
  "DELAY",
  "SUB_WORKFLOW",
  "TRANSFORM",
  "NOTIFICATION",
  "NOOP",
];

const NODE_TYPE_COLORS: Record<NodeType, string> = {
  SHELL: "#f59e0b",
  HTTP: "#3b82f6",
  DOCKER: "#06b6d4",
  SCRIPT: "#8b5cf6",
  CONDITIONAL: "#ec4899",
  DELAY: "#6b7280",
  SUB_WORKFLOW: "#10b981",
  TRANSFORM: "#f97316",
  NOTIFICATION: "#14b8a6",
  NOOP: "#9ca3af",
};

export function WorkflowDetail() {
  const { id } = useParams<{ id: string }>();
  const { workspace } = useWorkspace();
  const navigate = useNavigate();
  const qc = useQueryClient();

  const { data: workflow, isLoading } = useWorkflow(id ?? "");
  const { data: nodes = [] } = useWorkflowNodes(id ?? "");
  const { data: edges = [] } = useWorkflowEdges(id ?? "");
  const publishWorkflow = usePublishWorkflow();
  const executeWorkflow = useExecuteWorkflow();

  const [showAddNode, setShowAddNode] = useState(false);
  const [showAddEdge, setShowAddEdge] = useState(false);
  const [selectedNode, setSelectedNode] = useState<WorkflowNode | null>(null);

  // Add node form state
  const [nodeName, setNodeName] = useState("");
  const [nodeLabel, setNodeLabel] = useState("");
  const [nodeType, setNodeType] = useState<NodeType>("SHELL");
  const [nodeConfig, setNodeConfig] = useState("");

  // Add edge form state
  const [edgeSource, setEdgeSource] = useState("");
  const [edgeTarget, setEdgeTarget] = useState("");
  const [edgeLabel, setEdgeLabel] = useState("");
  const [edgeCondition, setEdgeCondition] = useState("");

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!workflow) {
    return (
      <div className="py-20 text-center text-muted-foreground">
        Workflow not found.
      </div>
    );
  }

  const base = `/w/${workspace?.identifier}`;

  async function handleAddNode(e: React.FormEvent) {
    e.preventDefault();
    if (!id || !nodeName.trim()) return;
    await api.addNode(id, {
      name: nodeName.trim(),
      label: nodeLabel.trim() || nodeName.trim(),
      nodeType,
      config: nodeConfig.trim() || "{}",
      positionX: 100 + nodes.length * 160,
      positionY: 200,
    });
    qc.invalidateQueries({ queryKey: ["workflow-nodes", id] });
    setShowAddNode(false);
    setNodeName("");
    setNodeLabel("");
    setNodeType("SHELL");
    setNodeConfig("");
  }

  async function handleAddEdge(e: React.FormEvent) {
    e.preventDefault();
    if (!id || !edgeSource || !edgeTarget) return;
    await api.addEdge(id, {
      sourceNodeId: edgeSource,
      targetNodeId: edgeTarget,
      label: edgeLabel.trim(),
      conditionExpr: edgeCondition.trim(),
    });
    qc.invalidateQueries({ queryKey: ["workflow-edges", id] });
    setShowAddEdge(false);
    setEdgeSource("");
    setEdgeTarget("");
    setEdgeLabel("");
    setEdgeCondition("");
  }

  async function handleDeleteNode(nodeId: string) {
    if (!id) return;
    if (!confirm("Delete this node?")) return;
    await api.deleteNode(id, nodeId);
    qc.invalidateQueries({ queryKey: ["workflow-nodes", id] });
    qc.invalidateQueries({ queryKey: ["workflow-edges", id] });
    setSelectedNode(null);
  }

  async function handleDeleteEdge(edgeId: string) {
    if (!id) return;
    if (!confirm("Delete this edge?")) return;
    await api.deleteEdge(id, edgeId);
    qc.invalidateQueries({ queryKey: ["workflow-edges", id] });
  }

  function handlePublish() {
    if (!id) return;
    publishWorkflow.mutate(id);
  }

  function handleExecute() {
    if (!id) return;
    executeWorkflow.mutate(
      { workflowId: id },
      {
        onSuccess: (execution) => {
          navigate(`${base}/executions/${execution.id}`);
        },
      },
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-center gap-3">
          <Link
            to={`${base}/workflows`}
            className="rounded-md p-1 text-muted-foreground hover:bg-accent hover:text-foreground"
          >
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-2xl font-semibold">{workflow.name}</h1>
              <StatusBadge status={workflow.status} />
            </div>
            {workflow.description && (
              <p className="mt-1 text-sm text-muted-foreground">
                {workflow.description}
              </p>
            )}
          </div>
        </div>

        <div className="flex items-center gap-2">
          {workflow.status === "DRAFT" && (
            <button
              type="button"
              onClick={handlePublish}
              disabled={publishWorkflow.isPending}
              className="inline-flex items-center gap-1.5 rounded-md bg-green-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
            >
              <Upload className="h-4 w-4" />
              {publishWorkflow.isPending ? "Publishing..." : "Publish"}
            </button>
          )}
          {(workflow.status === "ACTIVE" || workflow.status === "DRAFT") && (
            <button
              type="button"
              onClick={handleExecute}
              disabled={executeWorkflow.isPending}
              className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            >
              <Play className="h-4 w-4" />
              {executeWorkflow.isPending ? "Starting..." : "Execute"}
            </button>
          )}
        </div>
      </div>

      {/* Metadata */}
      <div className="grid gap-4 sm:grid-cols-4">
        <div className="rounded-lg border border-border bg-card p-3">
          <p className="text-xs text-muted-foreground">Version</p>
          <p className="mt-0.5 text-sm font-medium">v{workflow.currentVersion}</p>
        </div>
        <div className="rounded-lg border border-border bg-card p-3">
          <p className="text-xs text-muted-foreground">Timeout</p>
          <p className="mt-0.5 text-sm font-medium">{workflow.timeoutSeconds}s</p>
        </div>
        <div className="rounded-lg border border-border bg-card p-3">
          <p className="text-xs text-muted-foreground">Max Retries</p>
          <p className="mt-0.5 text-sm font-medium">{workflow.maxRetries}</p>
        </div>
        <div className="rounded-lg border border-border bg-card p-3">
          <p className="text-xs text-muted-foreground">Tags</p>
          <p className="mt-0.5 truncate text-sm font-medium">
            {workflow.tags || "--"}
          </p>
        </div>
      </div>

      {/* Graph Visualization */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-medium">Workflow Graph</h2>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => setShowAddNode(true)}
              className="inline-flex items-center gap-1.5 rounded-md border border-border px-3 py-1.5 text-sm hover:bg-accent/50"
            >
              <Plus className="h-4 w-4" />
              Add Node
            </button>
            <button
              type="button"
              onClick={() => setShowAddEdge(true)}
              disabled={nodes.length < 2}
              className="inline-flex items-center gap-1.5 rounded-md border border-border px-3 py-1.5 text-sm hover:bg-accent/50 disabled:opacity-50"
            >
              <GitBranch className="h-4 w-4" />
              Add Edge
            </button>
          </div>
        </div>

        {nodes.length === 0 ? (
          <div className="rounded-lg border border-dashed border-border bg-card p-12 text-center">
            <Circle className="mx-auto h-10 w-10 text-muted-foreground" />
            <p className="mt-3 text-muted-foreground">
              No nodes yet. Add nodes to build your workflow.
            </p>
          </div>
        ) : (
          <WorkflowGraph
            nodes={nodes}
            edges={edges}
            selectedNode={selectedNode}
            onSelectNode={setSelectedNode}
          />
        )}
      </div>

      {/* Node List */}
      {nodes.length > 0 && (
        <div className="space-y-3">
          <h2 className="text-lg font-medium">
            Nodes ({nodes.length})
          </h2>
          <div className="space-y-1">
            {nodes.map((node) => (
              <div
                key={node.id}
                className={cn(
                  "flex items-center gap-3 rounded-lg border border-border bg-card px-4 py-2 transition-colors",
                  selectedNode?.id === node.id && "ring-2 ring-primary",
                )}
              >
                <span
                  className="h-3 w-3 rounded-full shrink-0"
                  style={{ backgroundColor: NODE_TYPE_COLORS[node.nodeType] }}
                />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium">{node.label || node.name}</p>
                  <p className="text-xs text-muted-foreground">
                    {node.nodeType} &middot; {node.name}
                  </p>
                </div>
                {node.timeoutSeconds != null && (
                  <span className="text-xs text-muted-foreground">
                    {node.timeoutSeconds}s timeout
                  </span>
                )}
                <button
                  type="button"
                  onClick={() => handleDeleteNode(node.id)}
                  className="rounded p-1 text-muted-foreground hover:bg-destructive/20 hover:text-destructive"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Edge List */}
      {edges.length > 0 && (
        <div className="space-y-3">
          <h2 className="text-lg font-medium">
            Edges ({edges.length})
          </h2>
          <div className="space-y-1">
            {edges.map((edge) => (
              <div
                key={edge.id}
                className="flex items-center gap-3 rounded-lg border border-border bg-card px-4 py-2"
              >
                <GitBranch className="h-4 w-4 shrink-0 text-muted-foreground" />
                <div className="min-w-0 flex-1 text-sm">
                  <span className="font-medium">{edge.sourceNodeName}</span>
                  <span className="mx-2 text-muted-foreground">&rarr;</span>
                  <span className="font-medium">{edge.targetNodeName}</span>
                  {edge.label && (
                    <span className="ml-2 text-muted-foreground">
                      ({edge.label})
                    </span>
                  )}
                </div>
                {edge.conditionExpr && (
                  <span className="shrink-0 text-xs text-muted-foreground font-mono">
                    {edge.conditionExpr}
                  </span>
                )}
                <button
                  type="button"
                  onClick={() => handleDeleteEdge(edge.id)}
                  className="rounded p-1 text-muted-foreground hover:bg-destructive/20 hover:text-destructive"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Executions link */}
      <div>
        <Link
          to={`${base}/executions`}
          className="text-sm text-primary hover:underline"
        >
          View executions for this workflow &rarr;
        </Link>
      </div>

      {/* Add Node Modal */}
      <Modal
        open={showAddNode}
        onOpenChange={setShowAddNode}
        title="Add Node"
        description="Add a new node to this workflow."
      >
        <form onSubmit={handleAddNode} className="space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium">Name</label>
            <input
              type="text"
              value={nodeName}
              onChange={(e) => setNodeName(e.target.value)}
              placeholder="e.g. build-step"
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              required
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Label</label>
            <input
              type="text"
              value={nodeLabel}
              onChange={(e) => setNodeLabel(e.target.value)}
              placeholder="Display label (optional)"
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Type</label>
            <select
              value={nodeType}
              onChange={(e) => setNodeType(e.target.value as NodeType)}
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            >
              {NODE_TYPE_OPTIONS.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">
              Config (JSON)
            </label>
            <textarea
              value={nodeConfig}
              onChange={(e) => setNodeConfig(e.target.value)}
              placeholder='{"command": "echo hello"}'
              rows={3}
              className="w-full rounded-md border border-border bg-background px-3 py-2 font-mono text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setShowAddNode(false)}
              className="rounded-md border border-border px-3 py-1.5 text-sm hover:bg-accent/50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={!nodeName.trim()}
              className="rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            >
              Add Node
            </button>
          </div>
        </form>
      </Modal>

      {/* Add Edge Modal */}
      <Modal
        open={showAddEdge}
        onOpenChange={setShowAddEdge}
        title="Add Edge"
        description="Connect two nodes in this workflow."
      >
        <form onSubmit={handleAddEdge} className="space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium">
              Source Node
            </label>
            <select
              value={edgeSource}
              onChange={(e) => setEdgeSource(e.target.value)}
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              required
            >
              <option value="">Select source...</option>
              {nodes.map((n) => (
                <option key={n.id} value={n.id}>
                  {n.label || n.name} ({n.nodeType})
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">
              Target Node
            </label>
            <select
              value={edgeTarget}
              onChange={(e) => setEdgeTarget(e.target.value)}
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              required
            >
              <option value="">Select target...</option>
              {nodes
                .filter((n) => n.id !== edgeSource)
                .map((n) => (
                  <option key={n.id} value={n.id}>
                    {n.label || n.name} ({n.nodeType})
                  </option>
                ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">
              Label (optional)
            </label>
            <input
              type="text"
              value={edgeLabel}
              onChange={(e) => setEdgeLabel(e.target.value)}
              placeholder="e.g. on-success"
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">
              Condition Expression (optional)
            </label>
            <input
              type="text"
              value={edgeCondition}
              onChange={(e) => setEdgeCondition(e.target.value)}
              placeholder="e.g. result.exitCode == 0"
              className="w-full rounded-md border border-border bg-background px-3 py-2 font-mono text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setShowAddEdge(false)}
              className="rounded-md border border-border px-3 py-1.5 text-sm hover:bg-accent/50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={!edgeSource || !edgeTarget}
              className="rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            >
              Add Edge
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

function WorkflowGraph({
  nodes,
  edges,
  selectedNode,
  onSelectNode,
}: {
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
  selectedNode: WorkflowNode | null;
  onSelectNode: (node: WorkflowNode | null) => void;
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const cyRef = useRef<import("cytoscape").Core | null>(null);

  const initGraph = useCallback(async () => {
    if (!containerRef.current) return;

    const cytoscape = (await import("cytoscape")).default;
    const dagre = (await import("cytoscape-dagre")).default;

    if (!cyRef.current) {
      cytoscape.use(dagre);
    }

    const elements: import("cytoscape").ElementDefinition[] = [
      ...nodes.map((n) => ({
        data: {
          id: n.id,
          label: n.label || n.name,
          nodeType: n.nodeType,
          color: NODE_TYPE_COLORS[n.nodeType],
        },
        position: { x: n.positionX, y: n.positionY },
      })),
      ...edges.map((e) => ({
        data: {
          id: e.id,
          source: e.sourceNodeId,
          target: e.targetNodeId,
          label: e.label || "",
        },
      })),
    ];

    if (cyRef.current) {
      cyRef.current.destroy();
    }

    const cy = cytoscape({
      container: containerRef.current,
      elements,
      style: [
        {
          selector: "node",
          style: {
            label: "data(label)",
            "background-color": "data(color)",
            color: "#e5e7eb",
            "text-valign": "bottom",
            "text-margin-y": 8,
            "font-size": "12px",
            width: 40,
            height: 40,
            "border-width": 2,
            "border-color": "#374151",
          },
        },
        {
          selector: "node:selected",
          style: {
            "border-color": "#6366f1",
            "border-width": 3,
          },
        },
        {
          selector: "edge",
          style: {
            width: 2,
            "line-color": "#4b5563",
            "target-arrow-color": "#4b5563",
            "target-arrow-shape": "triangle",
            "curve-style": "bezier",
            label: "data(label)",
            "font-size": "10px",
            color: "#9ca3af",
            "text-margin-y": -10,
          },
        },
      ],
      layout: {
        name: "dagre",
        rankDir: "LR",
        nodeSep: 60,
        rankSep: 100,
      } as import("cytoscape").LayoutOptions,
    });

    cy.on("tap", "node", (evt) => {
      const nodeId = evt.target.id();
      const node = nodes.find((n) => n.id === nodeId);
      onSelectNode(node ?? null);
    });

    cy.on("tap", (evt) => {
      if (evt.target === cy) {
        onSelectNode(null);
      }
    });

    cyRef.current = cy;
  }, [nodes, edges, onSelectNode]);

  useEffect(() => {
    initGraph();
    return () => {
      cyRef.current?.destroy();
      cyRef.current = null;
    };
  }, [initGraph]);

  return (
    <div
      ref={containerRef}
      className="h-[400px] rounded-lg border border-border bg-card"
    />
  );
}
