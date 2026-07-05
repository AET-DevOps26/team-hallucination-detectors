import { scanLabels } from "../../constants/scans";
import { ScanOption } from "../../types/domain";
import { Spinner } from "../ui/Spinner";

type ScanProgressProps = {
  status: "Pending" | "Running";
  scans: ScanOption[];
  host: string;
};

/**
 * Feedback while a scan is in flight. The backend reports only a coarse
 * Pending/Running status (no per-check progress), so this is an animated,
 * indeterminate view of the requested checks rather than a real progress bar.
 */
export function ScanProgress({ status, scans, host }: ScanProgressProps) {
  return (
    <div className="animate-fade-in rounded-xl border border-line bg-surface p-8 shadow-card">
      <div className="flex flex-col items-center text-center">
        <div className="relative flex h-20 w-20 items-center justify-center">
          <span className="absolute inset-0 rounded-full bg-primary/20 animate-pulse-ring" />
          <span className="absolute inset-2 rounded-full bg-primary/20 animate-pulse-ring [animation-delay:400ms]" />
          <span className="relative flex h-12 w-12 items-center justify-center rounded-full bg-primary text-primary-fg">
            <svg className="h-7 w-7" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
              <path d="M12 3l7 3v6c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6l7-3z" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </span>
        </div>
        <h3 className="mt-5 text-xl font-semibold text-fg">
          {status === "Pending" ? "Queued for scanning" : "Scanning your site"}
        </h3>
        <p className="mt-1 text-sm text-muted">
          Running security checks against <span className="font-medium text-fg">{host}</span>…
        </p>
      </div>

      <ul className="mx-auto mt-6 max-w-md space-y-2">
        {scans.map((scan) => (
          <li
            className="flex items-center gap-3 rounded-lg border border-line bg-elevated px-3 py-2.5 text-sm text-fg"
            key={scan}
          >
            <Spinner className="text-primary" size="sm" />
            <span>{scanLabels[scan]}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
