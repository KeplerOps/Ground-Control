import {
  FormField,
  inputClass,
  primaryButton,
  secondaryButton,
  selectClass,
} from "@/components/ui/form-field";
import { useProjectContext } from "@/contexts/project-context";
import { apiFetch } from "@/lib/api-client";
import type {
  PagedResponse,
  RelationRequest,
  RelationType,
  RequirementResponse,
} from "@/types/api";
import { useCallback, useState } from "react";

const RELATION_TYPES: RelationType[] = [
  "DEPENDS_ON",
  "PARENT",
  "REFINES",
  "CONFLICTS_WITH",
  "SUPERSEDES",
  "RELATED",
];

interface RelationFormProps {
  sourceId: string;
  onSubmit: (data: RelationRequest) => void;
  onCancel: () => void;
  loading?: boolean;
}

export function RelationForm({
  onSubmit,
  onCancel,
  loading,
}: RelationFormProps) {
  const { activeProject } = useProjectContext();
  const [search, setSearch] = useState("");
  const [targetId, setTargetId] = useState("");
  const [relationType, setRelationType] = useState<RelationType>("DEPENDS_ON");
  const [suggestions, setSuggestions] = useState<RequirementResponse[]>([]);
  const [selectedUid, setSelectedUid] = useState("");

  const searchRequirements = useCallback(
    async (q: string) => {
      if (q.length < 2) {
        setSuggestions([]);
        return;
      }
      const data = await apiFetch<PagedResponse<RequirementResponse>>(
        "/requirements",
        {
          params: {
            project: activeProject?.identifier,
            search: q,
            size: "10",
          },
        },
      );
      setSuggestions(data.content);
    },
    [activeProject],
  );

  function handleSearchChange(value: string) {
    setSearch(value);
    setSelectedUid("");
    setTargetId("");
    searchRequirements(value);
  }

  function selectTarget(req: RequirementResponse) {
    setTargetId(req.id);
    setSelectedUid(req.uid);
    setSearch(req.uid);
    setSuggestions([]);
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!targetId) return;
    onSubmit({ targetId, relationType });
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <FormField label="Target Requirement">
        <div className="relative">
          <input
            className={inputClass}
            value={search}
            onChange={(e) => handleSearchChange(e.target.value)}
            placeholder="Search by UID or title..."
          />
          {suggestions.length > 0 && !selectedUid && (
            <div className="absolute z-10 mt-1 w-full rounded-md border border-border bg-card shadow-lg max-h-48 overflow-y-auto">
              {suggestions.map((r) => (
                <button
                  key={r.id}
                  type="button"
                  className="w-full px-3 py-2 text-left text-sm hover:bg-accent flex items-center gap-2"
                  onClick={() => selectTarget(r)}
                >
                  <span className="font-mono text-xs text-muted-foreground">
                    {r.uid}
                  </span>
                  <span className="truncate">{r.title}</span>
                </button>
              ))}
            </div>
          )}
        </div>
        {selectedUid && (
          <p className="text-xs text-muted-foreground mt-1">
            Selected: {selectedUid}
          </p>
        )}
      </FormField>
      <FormField label="Relation Type">
        <select
          className={selectClass}
          value={relationType}
          onChange={(e) => setRelationType(e.target.value as RelationType)}
        >
          {RELATION_TYPES.map((t) => (
            <option key={t} value={t}>
              {t.replace("_", " ")}
            </option>
          ))}
        </select>
      </FormField>
      <div className="flex justify-end gap-3 pt-2">
        <button type="button" className={secondaryButton} onClick={onCancel}>
          Cancel
        </button>
        <button
          type="submit"
          className={primaryButton}
          disabled={loading || !targetId}
        >
          {loading ? "Adding..." : "Add Relation"}
        </button>
      </div>
    </form>
  );
}
