import { Modal } from "@/components/ui/modal";
import { useWorkspace } from "@/contexts/workspace-context";
import { api } from "@/lib/api-client";
import { cn } from "@/lib/utils";
import type { Credential, Trigger, Variable } from "@/types/api";
import * as Tabs from "@radix-ui/react-tabs";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Clock,
  Key,
  Plus,
  Settings2,
  Trash2,
  Variable as VariableIcon,
} from "lucide-react";
import { useState } from "react";

export function Settings() {
  const { workspace } = useWorkspace();

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">Settings</h1>

      <Tabs.Root defaultValue="triggers" className="space-y-4">
        <Tabs.List className="flex gap-1 border-b border-border">
          <TabTrigger value="triggers">
            <Clock className="h-4 w-4" />
            Triggers
          </TabTrigger>
          <TabTrigger value="credentials">
            <Key className="h-4 w-4" />
            Credentials
          </TabTrigger>
          <TabTrigger value="variables">
            <VariableIcon className="h-4 w-4" />
            Variables
          </TabTrigger>
        </Tabs.List>

        <Tabs.Content value="triggers">
          <TriggersTab />
        </Tabs.Content>
        <Tabs.Content value="credentials">
          <CredentialsTab workspaceId={workspace?.identifier} />
        </Tabs.Content>
        <Tabs.Content value="variables">
          <VariablesTab workspaceId={workspace?.identifier} />
        </Tabs.Content>
      </Tabs.Root>
    </div>
  );
}

function TabTrigger({
  value,
  children,
}: {
  value: string;
  children: React.ReactNode;
}) {
  return (
    <Tabs.Trigger
      value={value}
      className="flex items-center gap-1.5 border-b-2 border-transparent px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground data-[state=active]:border-primary data-[state=active]:text-foreground"
    >
      {children}
    </Tabs.Trigger>
  );
}

// --- Triggers Tab ---

function TriggersTab() {
  const qc = useQueryClient();
  const [showInfo, setShowInfo] = useState(false);

  // Triggers require a workflowId -- display info message
  return (
    <div className="space-y-4">
      <div className="rounded-lg border border-border bg-card p-8 text-center">
        <Clock className="mx-auto h-10 w-10 text-muted-foreground" />
        <p className="mt-3 text-muted-foreground">
          Triggers are managed per-workflow. Open a workflow to configure its
          triggers (CRON, Webhook, Event).
        </p>
        <p className="mt-2 text-xs text-muted-foreground">
          Navigate to a workflow detail page to add and manage triggers.
        </p>
      </div>
    </div>
  );
}

// --- Credentials Tab ---

function CredentialsTab({ workspaceId }: { workspaceId?: string }) {
  const qc = useQueryClient();
  const { data: credentials = [], isLoading } = useQuery({
    queryKey: ["credentials", workspaceId],
    queryFn: () => api.listCredentials(workspaceId),
    enabled: !!workspaceId,
  });
  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState("");
  const [credType, setCredType] = useState("");
  const [credData, setCredData] = useState("");

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim() || !credType.trim()) return;
    await api.createCredential(
      {
        name: name.trim(),
        credentialType: credType.trim(),
        data: credData.trim() || undefined,
      },
      workspaceId,
    );
    qc.invalidateQueries({ queryKey: ["credentials", workspaceId] });
    setShowCreate(false);
    setName("");
    setCredType("");
    setCredData("");
  }

  async function handleDelete(id: string) {
    if (!confirm("Delete this credential?")) return;
    await api.deleteCredential(id);
    qc.invalidateQueries({ queryKey: ["credentials", workspaceId] });
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          Manage credentials for connecting to external services.
        </p>
        <button
          type="button"
          onClick={() => setShowCreate(true)}
          className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="h-4 w-4" />
          Add Credential
        </button>
      </div>

      {isLoading ? (
        <div className="space-y-2">
          {["s1", "s2", "s3"].map((k) => (
            <div
              key={k}
              className="h-14 animate-pulse rounded-lg border border-border bg-card"
            />
          ))}
        </div>
      ) : credentials.length === 0 ? (
        <div className="rounded-lg border border-border bg-card p-8 text-center">
          <Key className="mx-auto h-10 w-10 text-muted-foreground" />
          <p className="mt-3 text-muted-foreground">No credentials yet.</p>
        </div>
      ) : (
        <div className="space-y-1">
          {credentials.map((cred) => (
            <div
              key={cred.id}
              className="flex items-center gap-3 rounded-lg border border-border bg-card px-4 py-3"
            >
              <Key className="h-4 w-4 shrink-0 text-muted-foreground" />
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium">{cred.name}</p>
                <p className="text-xs text-muted-foreground">
                  {cred.credentialType}
                </p>
              </div>
              <span className="text-xs text-muted-foreground">
                {new Date(cred.createdAt).toLocaleDateString()}
              </span>
              <button
                type="button"
                onClick={() => handleDelete(cred.id)}
                className="rounded p-1 text-muted-foreground hover:bg-destructive/20 hover:text-destructive"
              >
                <Trash2 className="h-4 w-4" />
              </button>
            </div>
          ))}
        </div>
      )}

      <Modal
        open={showCreate}
        onOpenChange={setShowCreate}
        title="Add Credential"
      >
        <form onSubmit={handleCreate} className="space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium">Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. AWS Production"
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              required
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Type</label>
            <input
              type="text"
              value={credType}
              onChange={(e) => setCredType(e.target.value)}
              placeholder="e.g. AWS, API_KEY, OAUTH2"
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              required
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">
              Data (JSON, optional)
            </label>
            <textarea
              value={credData}
              onChange={(e) => setCredData(e.target.value)}
              placeholder='{"accessKey": "...", "secretKey": "..."}'
              rows={3}
              className="w-full rounded-md border border-border bg-background px-3 py-2 font-mono text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setShowCreate(false)}
              className="rounded-md border border-border px-3 py-1.5 text-sm hover:bg-accent/50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={!name.trim() || !credType.trim()}
              className="rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            >
              Create
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

// --- Variables Tab ---

function VariablesTab({ workspaceId }: { workspaceId?: string }) {
  const qc = useQueryClient();
  const { data: variables = [], isLoading } = useQuery({
    queryKey: ["variables", workspaceId],
    queryFn: () => api.listVariables(workspaceId),
    enabled: !!workspaceId,
  });
  const [showCreate, setShowCreate] = useState(false);
  const [key, setKey] = useState("");
  const [value, setValue] = useState("");
  const [description, setDescription] = useState("");
  const [secret, setSecret] = useState(false);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!key.trim()) return;
    await api.createVariable(
      {
        key: key.trim(),
        value: value.trim() || undefined,
        description: description.trim() || undefined,
        secret,
      },
      workspaceId,
    );
    qc.invalidateQueries({ queryKey: ["variables", workspaceId] });
    setShowCreate(false);
    setKey("");
    setValue("");
    setDescription("");
    setSecret(false);
  }

  async function handleDelete(id: string) {
    if (!confirm("Delete this variable?")) return;
    await api.deleteVariable(id);
    qc.invalidateQueries({ queryKey: ["variables", workspaceId] });
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          Environment variables available to all workflows in this workspace.
        </p>
        <button
          type="button"
          onClick={() => setShowCreate(true)}
          className="inline-flex items-center gap-1.5 rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="h-4 w-4" />
          Add Variable
        </button>
      </div>

      {isLoading ? (
        <div className="space-y-2">
          {["s1", "s2", "s3"].map((k) => (
            <div
              key={k}
              className="h-14 animate-pulse rounded-lg border border-border bg-card"
            />
          ))}
        </div>
      ) : variables.length === 0 ? (
        <div className="rounded-lg border border-border bg-card p-8 text-center">
          <Settings2 className="mx-auto h-10 w-10 text-muted-foreground" />
          <p className="mt-3 text-muted-foreground">No variables yet.</p>
        </div>
      ) : (
        <div className="space-y-1">
          {variables.map((v) => (
            <div
              key={v.id}
              className="flex items-center gap-3 rounded-lg border border-border bg-card px-4 py-3"
            >
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <p className="text-sm font-medium font-mono">{v.key}</p>
                  {v.secret && (
                    <span className="rounded bg-yellow-500/15 px-1.5 py-0.5 text-xs text-yellow-400">
                      secret
                    </span>
                  )}
                </div>
                {v.description && (
                  <p className="mt-0.5 text-xs text-muted-foreground">
                    {v.description}
                  </p>
                )}
              </div>
              <span className="shrink-0 text-sm font-mono text-muted-foreground max-w-48 truncate">
                {v.secret ? "********" : v.value || "--"}
              </span>
              <button
                type="button"
                onClick={() => handleDelete(v.id)}
                className="rounded p-1 text-muted-foreground hover:bg-destructive/20 hover:text-destructive"
              >
                <Trash2 className="h-4 w-4" />
              </button>
            </div>
          ))}
        </div>
      )}

      <Modal
        open={showCreate}
        onOpenChange={setShowCreate}
        title="Add Variable"
      >
        <form onSubmit={handleCreate} className="space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium">Key</label>
            <input
              type="text"
              value={key}
              onChange={(e) => setKey(e.target.value)}
              placeholder="e.g. DATABASE_URL"
              className="w-full rounded-md border border-border bg-background px-3 py-2 font-mono text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              required
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">Value</label>
            <input
              type="text"
              value={value}
              onChange={(e) => setValue(e.target.value)}
              placeholder="Variable value"
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium">
              Description
            </label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Optional description"
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="secret"
              checked={secret}
              onChange={(e) => setSecret(e.target.checked)}
              className="h-4 w-4 rounded border-border"
            />
            <label htmlFor="secret" className="text-sm">
              Secret (value will be masked)
            </label>
          </div>
          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setShowCreate(false)}
              className="rounded-md border border-border px-3 py-1.5 text-sm hover:bg-accent/50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={!key.trim()}
              className="rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            >
              Create
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
