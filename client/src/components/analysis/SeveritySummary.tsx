import { severityStyles } from "../../constants/scans";
import { Severity } from "../../types/domain";

type SeveritySummaryProps = {
  counts: { severity: Severity; count: number }[];
  variant?: "light" | "dark";
};

export function SeveritySummary({ counts, variant = "light" }: SeveritySummaryProps) {
  return (
    <div className="grid grid-cols-5 gap-2">
      {counts.map(({ severity, count }) => (
        <div
          className={
            variant === "dark"
              ? "min-w-16 rounded-lg border border-white/10 bg-white/10 px-3 py-2 text-center text-white"
              : `min-w-16 rounded-lg border px-3 py-2 text-center ${severityStyles[severity]}`
          }
          key={severity}
        >
          <p className="text-lg font-semibold">{count}</p>
          <p className={variant === "dark" ? "text-xs text-slate-300" : "text-xs"}>
            {severity}
          </p>
        </div>
      ))}
    </div>
  );
}
