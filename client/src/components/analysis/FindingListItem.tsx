import { severityStyles } from "../../constants/scans";
import { Finding } from "../../types/domain";

type FindingListItemProps = {
  finding: Finding;
  onSelectFinding: (id: string) => void;
  selected: boolean;
};

export function FindingListItem({
  finding,
  onSelectFinding,
  selected,
}: FindingListItemProps) {
  return (
    <button
      className={`w-full rounded-md border p-4 text-left transition hover:border-teal-500 ${
        selected ? "border-teal-600 bg-teal-50" : "border-zinc-200 bg-white"
      }`}
      onClick={() => onSelectFinding(finding.id)}
      type="button"
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span
          className={`rounded border px-2 py-1 text-xs font-semibold ${severityStyles[finding.severity]}`}
        >
          {finding.severity}
        </span>
        <span className="rounded bg-zinc-100 px-2 py-1 text-xs font-medium text-zinc-600">
          {finding.status}
        </span>
      </div>
      <p className="mt-3 font-semibold">{finding.title}</p>
      <p className="mt-1 truncate text-sm text-zinc-500">{finding.affected}</p>
    </button>
  );
}
