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
      className={`w-full rounded-md border p-3 text-left transition hover:border-teal-500 hover:bg-teal-50/40 ${
        selected
          ? "border-teal-600 bg-teal-50 shadow-sm ring-1 ring-teal-600/10"
          : "border-zinc-200 bg-white"
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
          <span className="rounded border border-red-300 bg-red-50 px-1.5 py-0.5 text-[11px] font-semibold text-red-800">
            New
          </span>
        )}
        <span className="ml-auto shrink-0 rounded-full bg-zinc-100 px-2 py-0.5 text-[11px] font-medium text-zinc-600">
          {finding.status}
        </span>
      </div>
      <p className="mt-2 text-sm font-semibold leading-5 text-zinc-950">{finding.title}</p>
      <p className="mt-1 truncate text-xs text-zinc-500">{finding.affected}</p>
    </button>
  );
}
