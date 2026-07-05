import { scanDescriptions, scanLabels } from "../../constants/scans";
import { ScanOption } from "../../types/domain";

/**
 * Shared scan-configuration controls used by both the home-page hero form and
 * the dedicated "Create new analysis" page, so the two stay visually and
 * behaviourally in sync.
 */

export function ScanChecklist({
  label = "Checks to run",
  selectedScans,
  toggleScan,
}: {
  label?: string;
  selectedScans: ScanOption[];
  toggleScan: (scan: ScanOption) => void;
}) {
  return (
    <div>
      <p className="text-sm font-medium text-muted">{label}</p>
      <div className="mt-3 grid gap-2 sm:grid-cols-2">
        {(Object.keys(scanLabels) as ScanOption[]).map((scan) => {
          const checked = selectedScans.includes(scan);
          return (
            <label
              className={`flex cursor-pointer items-center gap-3 rounded-lg border p-3 text-sm transition ${
                checked ? "border-primary bg-primary/5" : "border-line hover:border-primary"
              }`}
              key={scan}
            >
              <input
                checked={checked}
                className="accent-[rgb(var(--primary))]"
                onChange={() => toggleScan(scan)}
                type="checkbox"
              />
              <span className="font-semibold text-fg">{scanLabels[scan]}</span>
              <ScanInfoTooltip description={scanDescriptions[scan]} label={scanLabels[scan]} />
            </label>
          );
        })}
      </div>
    </div>
  );
}

/**
 * Info icon that reveals a scan's description on hover/focus. Rendered inside the
 * check's <label>, so the button suppresses the default click to avoid toggling
 * the checkbox when the user is only asking "what does this check do?".
 */
function ScanInfoTooltip({ description, label }: { description: string; label: string }) {
  return (
    <span className="group/tip relative ml-auto flex items-center">
      <button
        aria-label={`What "${label}" checks`}
        className="rounded-full text-muted transition hover:text-primary focus:text-primary focus:outline-none focus-visible:ring-2 focus-visible:ring-primary/30"
        onClick={(event) => event.preventDefault()}
        type="button"
      >
        <svg
          aria-hidden="true"
          className="h-4 w-4"
          fill="none"
          stroke="currentColor"
          strokeWidth={1.7}
          viewBox="0 0 24 24"
        >
          <path
            d="M12 9h.01M11 12h1v4h1m8-4a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </button>
      <span
        className="pointer-events-none absolute bottom-full right-0 z-20 mb-2 w-60 rounded-lg border border-line bg-surface p-3 text-left text-xs font-normal leading-relaxed text-muted opacity-0 shadow-card transition-opacity duration-150 group-hover/tip:opacity-100 group-focus-within/tip:opacity-100"
        role="tooltip"
      >
        {description}
      </span>
    </span>
  );
}

export function ScopeFields({
  crawlDepth,
  includeSubdomains,
  setCrawlDepth,
  setIncludeSubdomains,
}: {
  crawlDepth: number;
  includeSubdomains: boolean;
  setCrawlDepth: (value: number) => void;
  setIncludeSubdomains: (value: boolean) => void;
}) {
  return (
    <div className="grid gap-3 sm:grid-cols-2">
      <label className="block">
        <span className="text-sm font-medium text-muted">Crawl depth</span>
        <input
          className="mt-2 w-full rounded-lg border border-line bg-surface px-3 py-2.5 text-base text-fg outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20"
          max={3}
          min={0}
          onChange={(event) => setCrawlDepth(Number(event.target.value))}
          type="number"
          value={crawlDepth}
        />
      </label>
      <label className="flex items-center gap-3 self-end rounded-lg border border-line p-3">
        <input
          checked={includeSubdomains}
          className="accent-[rgb(var(--primary))]"
          onChange={(event) => setIncludeSubdomains(event.target.checked)}
          type="checkbox"
        />
        <span className="text-sm font-semibold text-fg">Include subdomains</span>
      </label>
    </div>
  );
}
