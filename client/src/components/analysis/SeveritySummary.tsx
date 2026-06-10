import { severityStyles } from "../../constants/scans";
import { Severity } from "../../types/domain";

type SeveritySummaryProps = {
  counts: { severity: Severity; count: number }[];
};

export function SeveritySummary({ counts }: SeveritySummaryProps) {
  return (
    <div className="grid grid-cols-5 gap-2">
      {counts.map(({ severity, count }) => (
        <div
          className={`min-w-16 rounded-md border px-3 py-2 text-center ${severityStyles[severity]}`}
          key={severity}
        >
          <p className="text-lg font-semibold">{count}</p>
          <p className="text-xs">{severity}</p>
        </div>
      ))}
    </div>
  );
}
