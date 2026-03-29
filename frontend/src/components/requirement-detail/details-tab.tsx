export function DetailsTab({
  req,
}: {
  req: {
    statement: string;
    rationale: string;
    createdAt: string;
    updatedAt: string;
    archivedAt: string | null;
  };
}) {
  return (
    <div className="space-y-4">
      {req.statement && (
        <div>
          <h3 className="mb-1 text-sm font-medium text-muted-foreground">
            Statement
          </h3>
          <p className="whitespace-pre-wrap text-sm">{req.statement}</p>
        </div>
      )}
      {req.rationale && (
        <div>
          <h3 className="mb-1 text-sm font-medium text-muted-foreground">
            Rationale
          </h3>
          <p className="whitespace-pre-wrap text-sm">{req.rationale}</p>
        </div>
      )}
      <div className="flex gap-6 text-xs text-muted-foreground pt-4 border-t border-border">
        <span>Created: {new Date(req.createdAt).toLocaleString()}</span>
        <span>Updated: {new Date(req.updatedAt).toLocaleString()}</span>
        {req.archivedAt && (
          <span>Archived: {new Date(req.archivedAt).toLocaleString()}</span>
        )}
      </div>
    </div>
  );
}
