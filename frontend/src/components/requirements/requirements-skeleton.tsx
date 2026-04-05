export function RequirementsSkeleton() {
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
