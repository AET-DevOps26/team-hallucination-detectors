import { scanCategoryStyles, severityStyles } from "../../constants/scans";
import { Finding } from "../../types/domain";

type FindingListItemProps = {
  finding: Finding;
  isNew?: boolean;
  onSelectFinding: (id: string) => void;
  selected: boolean;
};

export function FindingListItem({
  finding,
  isNew,
  onSelectFinding,
  selected,
}: FindingListItemProps) {
  return (
    <button
      className={`w-full rounded-lg border p-3 text-left transition hover:border-primary hover:bg-primary/5 ${
        selected
          ? "border-primary bg-primary/10 shadow-sm ring-1 ring-primary/20"
          : "border-line bg-surface"
      }`}
      onClick={() => onSelectFinding(finding.id)}
      type="button"
    >
      <div className="flex flex-wrap items-center gap-1.5">
        <span
          className={`rounded border px-1.5 py-0.5 text-[11px] font-semibold ${scanCategoryStyles[finding.check]}`}
        >
          {finding.checkLabel}
        </span>
        <span
          className={`rounded border px-1.5 py-0.5 text-[11px] font-semibold ${severityStyles[finding.severity]}`}
        >
          {finding.severity}
        </span>
        {isNew && (
          <span className="rounded border border-red-300 bg-red-50 px-1.5 py-0.5 text-[11px] font-semibold text-red-800 dark:border-red-500/40 dark:bg-red-500/15 dark:text-red-300">
            New
          </span>
        )}
        <span className="ml-auto shrink-0 rounded-full bg-elevated px-2 py-0.5 text-[11px] font-medium text-muted">
          {finding.status}
        </span>
      </div>
      <p className="mt-2 text-sm font-semibold leading-5 text-fg">{finding.title}</p>
      <p className="mt-1 truncate text-xs text-muted">{finding.affected}</p>
    </button>
  );
}
