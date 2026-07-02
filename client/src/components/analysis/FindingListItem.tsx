import { scanCategoryStyles, severityStyles } from "../../constants/scans";
import { Finding } from "../../types/domain";

type FindingListItemProps = {
  finding: Finding;
  onGeneratePrompt?: (id: string) => void;
  onSelectFinding: (id: string) => void;
  selected: boolean;
};

export function FindingListItem({
  finding,
  onGeneratePrompt,
  onSelectFinding,
  selected,
}: FindingListItemProps) {
  return (
    <div
      className={`w-full rounded-md border p-4 text-left transition hover:border-teal-500 ${
        selected ? "border-teal-600 bg-teal-50" : "border-zinc-200 bg-white"
      }`}
    >
      <button
        className="w-full text-left"
        onClick={() => onSelectFinding(finding.id)}
        type="button"
      >
        <div className="flex flex-wrap items-center gap-2">
          <span
            className={`rounded border px-2 py-1 text-xs font-semibold ${scanCategoryStyles[finding.check]}`}
          >
            {finding.checkLabel}
          </span>
          <span
            className={`rounded border px-2 py-1 text-xs font-semibold ${severityStyles[finding.severity]}`}
          >
            {finding.severity}
          </span>
          <span className="ml-auto rounded bg-zinc-100 px-2 py-1 text-xs font-medium text-zinc-600">
            {finding.status}
          </span>
        </div>
        <p className="mt-3 font-semibold">{finding.title}</p>
        <p className="mt-1 truncate text-sm text-zinc-500">{finding.affected}</p>
      </button>
      {onGeneratePrompt && (
        <button
          className="mt-3 rounded-md border border-zinc-300 px-3 py-2 text-xs font-semibold text-zinc-700 hover:border-teal-500"
          onClick={() => onGeneratePrompt(finding.id)}
          type="button"
        >
          Generate prompt
        </button>
      )}
    </div>
  );
}
