import { RequirementForm } from "@/components/requirement-form";
import { StatusBadgeDropdown } from "@/components/status-badge";
import { PriorityBadge, TypeBadge } from "@/components/ui/badge";
import { Modal } from "@/components/ui/modal";
import { useToast } from "@/components/ui/toast";
import { useProjectContext } from "@/contexts/project-context";
import {
  useBulkTransition,
  useCreateRequirement,
  useRequirements,
  useTransitionStatus,
} from "@/hooks/use-requirements";
import { cn } from "@/lib/utils";
import type {
  BulkStatusTransitionResponse,
  Priority,
  RequirementRequest,
  RequirementResponse,
  RequirementType,
  Status,
  UpdateRequirementRequest,
} from "@/types/api";
import * as Checkbox from "@radix-ui/react-checkbox";
import { Check, ChevronDown, ChevronUp, FileText, Plus } from "lucide-react";
import { useCallback, useState } from "react";
import { useNavigate } from "react-router-dom";

type SortField =
  | "uid"
  | "title"
  | "requirementType"
  | "priority"
  | "status"
  | "wave"
  | "updatedAt";
type SortDir = "asc" | "desc";

export function Requirements() {
  const { activeProject, isLoading: projectLoading } = useProjectContext();
  const navigate = useNavigate();
  const { toast } = useToast();

  const [page, setPage] = useState(0);
  const [size, setSize] = useState(25);
  const [sortField, setSortField] = useState<SortField>("uid");
  const [sortDir, setSortDir] = useState<SortDir>("asc");
  const [statusFilter, setStatusFilter] = useState("");
  const [typeFilter, setTypeFilter] = useState("");
  const [priorityFilter, setPriorityFilter] = useState("");
  const [waveFilter, setWaveFilter] = useState("");
  const [searchFilter, setSearchFilter] = useState("");
  const [createOpen, setCreateOpen] = useState(false);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [bulkStatus, setBulkStatus] = useState<Status>("ACTIVE");

  const sort = `${sortField},${sortDir}`;

  const { data, isLoading } = useRequirements({
    status: statusFilter || undefined,
    type: typeFilter || undefined,
    priority: priorityFilter || undefined,
    wave: waveFilter || undefined,
    search: searchFilter || undefined,
    page,
    size,
    sort,
  });

  const createMutation = useCreateRequirement();
  const bulkMutation = useBulkTransition();

  function handleSort(field: SortField) {
    if (sortField === field) {
      setSortDir(sortDir === "asc" ? "desc" : "asc");
    } else {
      setSortField(field);
      setSortDir("asc");
    }
    setPage(0);
  }

  function SortIcon({ field }: { field: SortField }) {
    if (sortField !== field) return null;
    return sortDir === "asc" ? (
      <ChevronUp className="h-3 w-3" />
    ) : (
      <ChevronDown className="h-3 w-3" />
    );
  }

  function toggleSelected(id: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function toggleAll() {
    if (!data) return;
    if (selected.size === data.content.length) {
      setSelected(new Set());
    } else {
      setSelected(new Set(data.content.map((r) => r.id)));
    }
  }

  const handleCreate = useCallback(
    (formData: RequirementRequest | UpdateRequirementRequest) => {
      createMutation.mutate(formData as RequirementRequest, {
        onSuccess: () => {
          setCreateOpen(false);
          toast({ title: "Requirement created", variant: "success" });
        },
        onError: (err) => {
          toast({
            title: "Failed to create requirement",
            description: err.message,
            variant: "error",
          });
        },
      });
    },
    [createMutation, toast],
  );

  function handleBulkTransition() {
    if (selected.size === 0) return;
    bulkMutation.mutate(
      { ids: Array.from(selected), status: bulkStatus },
      {
        onSuccess: (result: BulkStatusTransitionResponse) => {
          setSelected(new Set());
          toast({
            title: `Transitioned ${result.totalSucceeded} requirements`,
            description:
              result.totalFailed > 0
                ? `${result.totalFailed} failed`
                : undefined,
            variant: result.totalFailed > 0 ? "error" : "success",
          });
        },
        onError: (err) => {
          toast({
            title: "Bulk transition failed",
            description: err.message,
            variant: "error",
          });
        },
      },
    );
  }

  if (projectLoading || isLoading) return <LoadingSkeleton />;

  if (!activeProject) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20 text-center">
        <FileText className="h-12 w-12 text-muted-foreground" />
        <h1 className="text-2xl font-semibold">Requirements</h1>
        <p className="text-muted-foreground">
          Select a project to view requirements.
        </p>
      </div>
    );
  }

  const content = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Requirements</h1>
        <div className="flex items-center gap-3">
          <span className="text-sm text-muted-foreground">
            {totalElements} total
          </span>
          <button
            type="button"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-3 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            onClick={() => setCreateOpen(true)}
          >
            <Plus className="h-4 w-4" /> New Requirement
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3 rounded-lg border border-border bg-card p-3">
        <input
          className="rounded-md border border-input bg-background px-3 py-1.5 text-sm placeholder:text-muted-foreground w-64"
          placeholder="Search by UID or title..."
          value={searchFilter}
          onChange={(e) => {
            setSearchFilter(e.target.value);
            setPage(0);
          }}
        />
        <FilterSelect
          value={statusFilter}
          onChange={(v) => {
            setStatusFilter(v);
            setPage(0);
          }}
          placeholder="Status"
          options={["DRAFT", "ACTIVE", "DEPRECATED", "ARCHIVED"]}
        />
        <FilterSelect
          value={typeFilter}
          onChange={(v) => {
            setTypeFilter(v);
            setPage(0);
          }}
          placeholder="Type"
          options={[
            "FUNCTIONAL",
            "NON_FUNCTIONAL",
            "CONSTRAINT",
            "INTERFACE",
            "PERFORMANCE",
            "SECURITY",
            "DATA",
          ]}
        />
        <FilterSelect
          value={priorityFilter}
          onChange={(v) => {
            setPriorityFilter(v);
            setPage(0);
          }}
          placeholder="Priority"
          options={["MUST", "SHOULD", "COULD", "WONT"]}
        />
        <input
          type="number"
          className="rounded-md border border-input bg-background px-3 py-1.5 text-sm w-20 placeholder:text-muted-foreground"
          placeholder="Wave"
          value={waveFilter}
          onChange={(e) => {
            setWaveFilter(e.target.value);
            setPage(0);
          }}
          min={0}
        />
        {(statusFilter ||
          typeFilter ||
          priorityFilter ||
          waveFilter ||
          searchFilter) && (
          <button
            type="button"
            className="text-xs text-muted-foreground hover:text-foreground"
            onClick={() => {
              setStatusFilter("");
              setTypeFilter("");
              setPriorityFilter("");
              setWaveFilter("");
              setSearchFilter("");
              setPage(0);
            }}
          >
            Clear filters
          </button>
        )}
      </div>

      {/* Bulk actions */}
      {selected.size > 0 && (
        <div className="flex items-center gap-3 rounded-lg border border-primary/30 bg-primary/5 p-3">
          <span className="text-sm font-medium">{selected.size} selected</span>
          <select
            className="rounded-md border border-input bg-background px-2 py-1 text-sm"
            value={bulkStatus}
            onChange={(e) => setBulkStatus(e.target.value as Status)}
          >
            <option value="ACTIVE">ACTIVE</option>
            <option value="DEPRECATED">DEPRECATED</option>
            <option value="ARCHIVED">ARCHIVED</option>
          </select>
          <button
            type="button"
            className="rounded-md bg-primary px-3 py-1 text-sm text-primary-foreground hover:bg-primary/90"
            onClick={handleBulkTransition}
            disabled={bulkMutation.isPending}
          >
            {bulkMutation.isPending ? "..." : "Transition"}
          </button>
          <button
            type="button"
            className="text-xs text-muted-foreground hover:text-foreground"
            onClick={() => setSelected(new Set())}
          >
            Clear
          </button>
        </div>
      )}

      {/* Table */}
      <div className="rounded-lg border border-border overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-card border-b border-border">
            <tr>
              <th className="w-10 px-3 py-3">
                <Checkbox.Root
                  className="flex h-4 w-4 items-center justify-center rounded border border-input bg-background"
                  checked={
                    content.length > 0 && selected.size === content.length
                  }
                  onCheckedChange={toggleAll}
                >
                  <Checkbox.Indicator>
                    <Check className="h-3 w-3" />
                  </Checkbox.Indicator>
                </Checkbox.Root>
              </th>
              <SortableHeader
                field="uid"
                label="UID"
                currentSort={sortField}
                onSort={handleSort}
              >
                <SortIcon field="uid" />
              </SortableHeader>
              <SortableHeader
                field="title"
                label="Title"
                currentSort={sortField}
                onSort={handleSort}
              >
                <SortIcon field="title" />
              </SortableHeader>
              <SortableHeader
                field="requirementType"
                label="Type"
                currentSort={sortField}
                onSort={handleSort}
              >
                <SortIcon field="requirementType" />
              </SortableHeader>
              <SortableHeader
                field="priority"
                label="Priority"
                currentSort={sortField}
                onSort={handleSort}
              >
                <SortIcon field="priority" />
              </SortableHeader>
              <SortableHeader
                field="status"
                label="Status"
                currentSort={sortField}
                onSort={handleSort}
              >
                <SortIcon field="status" />
              </SortableHeader>
              <SortableHeader
                field="wave"
                label="Wave"
                currentSort={sortField}
                onSort={handleSort}
              >
                <SortIcon field="wave" />
              </SortableHeader>
              <SortableHeader
                field="updatedAt"
                label="Updated"
                currentSort={sortField}
                onSort={handleSort}
              >
                <SortIcon field="updatedAt" />
              </SortableHeader>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {content.map((req: RequirementResponse) => (
              <RequirementRow
                key={req.id}
                req={req}
                selected={selected.has(req.id)}
                onToggle={() => toggleSelected(req.id)}
                onClick={() => navigate(`/requirements/${req.id}`)}
              />
            ))}
            {content.length === 0 && (
              <tr>
                <td
                  colSpan={8}
                  className="px-4 py-8 text-center text-muted-foreground"
                >
                  No requirements found.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="text-sm text-muted-foreground">Rows per page:</span>
          <select
            className="rounded-md border border-input bg-background px-2 py-1 text-sm"
            value={size}
            onChange={(e) => {
              setSize(Number(e.target.value));
              setPage(0);
            }}
          >
            <option value="10">10</option>
            <option value="25">25</option>
            <option value="50">50</option>
          </select>
        </div>
        <div className="flex items-center gap-3">
          <span className="text-sm text-muted-foreground">
            Page {page + 1} of {Math.max(totalPages, 1)}
          </span>
          <button
            type="button"
            className="rounded-md border border-input bg-background px-3 py-1 text-sm hover:bg-accent disabled:opacity-50"
            disabled={page === 0}
            onClick={() => setPage(page - 1)}
          >
            Previous
          </button>
          <button
            type="button"
            className="rounded-md border border-input bg-background px-3 py-1 text-sm hover:bg-accent disabled:opacity-50"
            disabled={page >= totalPages - 1}
            onClick={() => setPage(page + 1)}
          >
            Next
          </button>
        </div>
      </div>

      {/* Create modal */}
      <Modal
        open={createOpen}
        onOpenChange={setCreateOpen}
        title="New Requirement"
        className="max-w-2xl"
      >
        <RequirementForm
          mode="create"
          onSubmit={handleCreate}
          onCancel={() => setCreateOpen(false)}
          loading={createMutation.isPending}
        />
      </Modal>
    </div>
  );
}

function RequirementRow({
  req,
  selected,
  onToggle,
  onClick,
}: {
  req: {
    id: string;
    uid: string;
    title: string;
    requirementType: RequirementType;
    priority: Priority;
    status: Status;
    wave: number;
    updatedAt: string;
  };
  selected: boolean;
  onToggle: () => void;
  onClick: () => void;
}) {
  const { toast } = useToast();
  const transition = useTransitionStatus(req.id);

  function handleRowClick(e: React.MouseEvent) {
    const target = e.target as HTMLElement;
    if (target.closest("[data-no-row-click]")) return;
    onClick();
  }

  function handleRowKeyDown(e: React.KeyboardEvent) {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      onClick();
    }
  }

  return (
    <tr
      className="hover:bg-accent/30 cursor-pointer"
      onClick={handleRowClick}
      onKeyDown={handleRowKeyDown}
      tabIndex={0}
    >
      <td className="px-3 py-3" data-no-row-click>
        <Checkbox.Root
          className="flex h-4 w-4 items-center justify-center rounded border border-input bg-background"
          checked={selected}
          onCheckedChange={onToggle}
        >
          <Checkbox.Indicator>
            <Check className="h-3 w-3" />
          </Checkbox.Indicator>
        </Checkbox.Root>
      </td>
      <td className="px-3 py-3 font-mono text-xs text-muted-foreground">
        {req.uid}
      </td>
      <td className="px-3 py-3 font-medium max-w-[300px] truncate">
        {req.title}
      </td>
      <td className="px-3 py-3">
        <TypeBadge type={req.requirementType} />
      </td>
      <td className="px-3 py-3">
        <PriorityBadge priority={req.priority} />
      </td>
      <td className="px-3 py-3" data-no-row-click>
        <StatusBadgeDropdown
          status={req.status}
          onTransition={(s) =>
            transition.mutate(s, {
              onSuccess: () =>
                toast({ title: `Status changed to ${s}`, variant: "success" }),
              onError: (err) =>
                toast({
                  title: "Transition failed",
                  description: err.message,
                  variant: "error",
                }),
            })
          }
        />
      </td>
      <td className="px-3 py-3 text-muted-foreground">{req.wave}</td>
      <td className="px-3 py-3 text-xs text-muted-foreground">
        {new Date(req.updatedAt).toLocaleDateString()}
      </td>
    </tr>
  );
}

function SortableHeader({
  field,
  label,
  currentSort,
  onSort,
  children,
}: {
  field: SortField;
  label: string;
  currentSort: SortField;
  onSort: (f: SortField) => void;
  children: React.ReactNode;
}) {
  return (
    <th className="px-3 py-3 text-left">
      <button
        type="button"
        className={cn(
          "inline-flex items-center gap-1 text-xs font-medium text-muted-foreground cursor-pointer select-none hover:text-foreground",
          currentSort === field && "text-foreground",
        )}
        onClick={() => onSort(field)}
      >
        {label}
        {children}
      </button>
    </th>
  );
}

function FilterSelect({
  value,
  onChange,
  placeholder,
  options,
}: {
  value: string;
  onChange: (v: string) => void;
  placeholder: string;
  options: string[];
}) {
  return (
    <select
      className="rounded-md border border-input bg-background px-2 py-1.5 text-sm text-foreground"
      value={value}
      onChange={(e) => onChange(e.target.value)}
    >
      <option value="">{placeholder}</option>
      {options.map((o) => (
        <option key={o} value={o}>
          {o.replace(/_/g, " ")}
        </option>
      ))}
    </select>
  );
}

function LoadingSkeleton() {
  return (
    <div className="space-y-4">
      <div className="h-8 w-48 animate-pulse rounded bg-muted" />
      <div className="h-12 animate-pulse rounded-lg bg-muted" />
      <div className="space-y-1">
        {["s1", "s2", "s3", "s4", "s5"].map((key) => (
          <div key={key} className="h-12 animate-pulse rounded bg-muted" />
        ))}
      </div>
    </div>
  );
}
