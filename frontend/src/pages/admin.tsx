import {
  FormField,
  inputClass,
  primaryButton,
} from "@/components/ui/form-field";
import { useToast } from "@/components/ui/toast";
import { useProjectContext } from "@/contexts/project-context";
import { apiFetch, apiUpload } from "@/lib/api-client";
import type {
  GitHubIssueResponse,
  ImportResultResponse,
  SyncResultResponse,
} from "@/types/api";
import { Database, Download, GitBranch, Settings, Upload } from "lucide-react";
import { useRef, useState } from "react";

export function Admin() {
  const { activeProject } = useProjectContext();

  if (!activeProject) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20 text-center">
        <Settings className="h-12 w-12 text-muted-foreground" />
        <h1 className="text-2xl font-semibold">Admin</h1>
        <p className="text-muted-foreground">
          Select a project to access admin tools.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Admin</h1>
      <div className="grid gap-6 lg:grid-cols-2">
        <StrictDocImport />
        <GitHubSync />
        <GitHubIssueCreation />
        <GraphMaterialization />
      </div>
    </div>
  );
}

function StrictDocImport() {
  const { activeProject } = useProjectContext();
  const { toast } = useToast();
  const fileRef = useRef<HTMLInputElement>(null);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<ImportResultResponse | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const file = fileRef.current?.files?.[0];
    if (!file) return;

    setLoading(true);
    setResult(null);
    try {
      const formData = new FormData();
      formData.append("file", file);
      const data = await apiUpload<ImportResultResponse>(
        "/admin/import/strictdoc",
        formData,
        { params: { project: activeProject?.identifier } },
      );
      setResult(data);
      toast({ title: "Import complete", variant: "success" });
    } catch (err) {
      toast({
        title: "Import failed",
        description: err instanceof Error ? err.message : "Unknown error",
        variant: "error",
      });
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="rounded-lg border border-border bg-card p-5">
      <h3 className="flex items-center gap-2 text-base font-medium mb-4">
        <Upload className="h-5 w-5 text-primary" /> StrictDoc Import
      </h3>
      <form onSubmit={handleSubmit} className="space-y-3">
        <FormField label="StrictDoc File (.sdoc)">
          <input
            ref={fileRef}
            type="file"
            accept=".sdoc,.xml"
            className={inputClass}
          />
        </FormField>
        <button type="submit" className={primaryButton} disabled={loading}>
          {loading ? "Importing..." : "Import"}
        </button>
      </form>
      {result && (
        <div className="mt-4 rounded bg-accent p-3 text-xs space-y-1">
          <p>Parsed: {result.requirementsParsed}</p>
          <p>
            Created: {result.requirementsCreated} | Updated:{" "}
            {result.requirementsUpdated}
          </p>
          <p>
            Relations: {result.relationsCreated} created,{" "}
            {result.relationsSkipped} skipped
          </p>
          <p>
            Links: {result.traceabilityLinksCreated} created,{" "}
            {result.traceabilityLinksSkipped} skipped
          </p>
          {result.errors.length > 0 && (
            <div className="text-destructive mt-2">
              {result.errors.map((e) => (
                <p key={e}>{e}</p>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function GitHubSync() {
  const { toast } = useToast();
  const [owner, setOwner] = useState("");
  const [repo, setRepo] = useState("");
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<SyncResultResponse | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setResult(null);
    try {
      const data = await apiFetch<SyncResultResponse>("/admin/sync/github", {
        method: "POST",
        params: { owner, repo },
      });
      setResult(data);
      toast({ title: "Sync complete", variant: "success" });
    } catch (err) {
      toast({
        title: "Sync failed",
        description: err instanceof Error ? err.message : "Unknown error",
        variant: "error",
      });
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="rounded-lg border border-border bg-card p-5">
      <h3 className="flex items-center gap-2 text-base font-medium mb-4">
        <Download className="h-5 w-5 text-primary" /> GitHub Sync
      </h3>
      <form onSubmit={handleSubmit} className="space-y-3">
        <div className="grid grid-cols-2 gap-3">
          <FormField label="Owner">
            <input
              className={inputClass}
              value={owner}
              onChange={(e) => setOwner(e.target.value)}
              placeholder="KeplerOps"
              required
            />
          </FormField>
          <FormField label="Repository">
            <input
              className={inputClass}
              value={repo}
              onChange={(e) => setRepo(e.target.value)}
              placeholder="Ground-Control"
              required
            />
          </FormField>
        </div>
        <button type="submit" className={primaryButton} disabled={loading}>
          {loading ? "Syncing..." : "Sync"}
        </button>
      </form>
      {result && (
        <div className="mt-4 rounded bg-accent p-3 text-xs space-y-1">
          <p>Fetched: {result.issuesFetched}</p>
          <p>
            Created: {result.issuesCreated} | Updated: {result.issuesUpdated}
          </p>
          <p>Links updated: {result.linksUpdated}</p>
          {result.errors.length > 0 && (
            <div className="text-destructive mt-2">
              {result.errors.map((e) => (
                <p key={e}>{e}</p>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function GitHubIssueCreation() {
  const { activeProject } = useProjectContext();
  const { toast } = useToast();
  const [uid, setUid] = useState("");
  const [repo, setRepo] = useState("");
  const [labels, setLabels] = useState("");
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<GitHubIssueResponse | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setResult(null);
    try {
      const data = await apiFetch<GitHubIssueResponse>("/admin/github/issues", {
        method: "POST",
        params: { project: activeProject?.identifier },
        body: {
          requirementUid: uid,
          repo: repo || undefined,
          labels: labels ? labels.split(",").map((l) => l.trim()) : undefined,
        },
      });
      setResult(data);
      toast({ title: "GitHub issue created", variant: "success" });
    } catch (err) {
      toast({
        title: "Failed to create issue",
        description: err instanceof Error ? err.message : "Unknown error",
        variant: "error",
      });
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="rounded-lg border border-border bg-card p-5">
      <h3 className="flex items-center gap-2 text-base font-medium mb-4">
        <GitBranch className="h-5 w-5 text-primary" /> Create GitHub Issue
      </h3>
      <form onSubmit={handleSubmit} className="space-y-3">
        <FormField label="Requirement UID">
          <input
            className={inputClass}
            value={uid}
            onChange={(e) => setUid(e.target.value)}
            placeholder="GC-A001"
            required
          />
        </FormField>
        <div className="grid grid-cols-2 gap-3">
          <FormField label="Repository (optional)">
            <input
              className={inputClass}
              value={repo}
              onChange={(e) => setRepo(e.target.value)}
              placeholder="owner/repo"
            />
          </FormField>
          <FormField label="Labels (comma-separated)">
            <input
              className={inputClass}
              value={labels}
              onChange={(e) => setLabels(e.target.value)}
              placeholder="requirement, wave-1"
            />
          </FormField>
        </div>
        <button type="submit" className={primaryButton} disabled={loading}>
          {loading ? "Creating..." : "Create Issue"}
        </button>
      </form>
      {result && (
        <div className="mt-4 rounded bg-accent p-3 text-xs space-y-1">
          <p>
            Issue #{result.issueNumber}:{" "}
            <a
              href={result.issueUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-primary hover:underline"
            >
              {result.issueUrl}
            </a>
          </p>
          {result.warning && (
            <p className="text-yellow-400">{result.warning}</p>
          )}
        </div>
      )}
    </div>
  );
}

function GraphMaterialization() {
  const { toast } = useToast();
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);

  async function handleMaterialize() {
    setLoading(true);
    setDone(false);
    try {
      await apiFetch<void>("/admin/graph/materialize", { method: "POST" });
      setDone(true);
      toast({ title: "Graph materialized", variant: "success" });
    } catch (err) {
      toast({
        title: "Materialization failed",
        description: err instanceof Error ? err.message : "Unknown error",
        variant: "error",
      });
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="rounded-lg border border-border bg-card p-5">
      <h3 className="flex items-center gap-2 text-base font-medium mb-4">
        <Database className="h-5 w-5 text-primary" /> Graph Materialization
      </h3>
      <p className="text-sm text-muted-foreground mb-4">
        Rebuild the materialized graph for ancestor/descendant queries.
      </p>
      <button
        type="button"
        className={primaryButton}
        onClick={handleMaterialize}
        disabled={loading}
      >
        {loading ? "Materializing..." : "Materialize Graph"}
      </button>
      {done && (
        <p className="mt-3 text-xs text-green-400">
          Graph materialized successfully.
        </p>
      )}
    </div>
  );
}
