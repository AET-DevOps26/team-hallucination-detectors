import { FormEvent, ReactNode, useState } from "react";
import { defaultScanSelection } from "../../constants/scans";
import { ScanOption, Site } from "../../types/domain";
import { validateTargetUrl } from "../../utils/url";
import { Button } from "../ui/Button";
import { ScanChecklist, ScopeFields } from "./ScanControls";

/** Optional scan tuning passed up from the inline advanced-options panel. */
export type ScanOverrides = {
  selectedScans: ScanOption[];
  crawlDepth: number;
  includeSubdomains: boolean;
};

type ScanBoxProps = {
  /** Kicks off the scan-intent flow. May reject to surface a submit error. */
  onScan: (url: string, overrides?: ScanOverrides) => Promise<void>;
  /** Pre-fills the URL input (e.g. the user's first known site). */
  initialUrl?: string;
  submitLabel?: string;
  submittingLabel?: string;
  autoFocus?: boolean;
  /** Known sites offered as an autocomplete datalist. */
  sites?: Site[];
  /** Rendered below the box, inside the form (e.g. a secondary action). */
  footer?: ReactNode;
};

/**
 * The gradient-bordered, glassy scan input — the product's primary call to
 * action. Shared by the home-page hero and the dedicated "Create new analysis"
 * page so the two stay visually and behaviourally in sync. Owns its own scan
 * configuration state; the parent only supplies the submit handler.
 */
export function ScanBox({
  onScan,
  initialUrl = "",
  submitLabel = "Scan my site",
  submittingLabel = "Starting…",
  autoFocus = false,
  sites,
  footer,
}: ScanBoxProps) {
  const [url, setUrl] = useState(initialUrl);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const [advancedOpen, setAdvancedOpen] = useState(false);
  const [selectedScans, setSelectedScans] = useState<ScanOption[]>([...defaultScanSelection]);
  // Contract bounds: crawlDepth 0–3, 0 = only the entered URL.
  const [crawlDepth, setCrawlDepth] = useState(0);
  const [includeSubdomains, setIncludeSubdomains] = useState(false);

  function toggleScan(scan: ScanOption) {
    setSelectedScans((current) =>
      current.includes(scan) ? current.filter((item) => item !== scan) : [...current, scan],
    );
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const result = validateTargetUrl(url);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    if (advancedOpen && selectedScans.length === 0) {
      setError("Choose at least one scan.");
      return;
    }
    setError("");
    setSubmitting(true);
    try {
      if (advancedOpen) {
        await onScan(result.url, { selectedScans, crawlDepth, includeSubdomains });
      } else {
        await onScan(result.url);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not start the scan.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="w-full" onSubmit={handleSubmit}>
      {/* Gradient-bordered, glassy input — the primary call to action. */}
      <div className="rounded-3xl bg-gradient-to-r from-primary/60 via-sky-400/40 to-primary/60 p-[1.5px] shadow-glow transition">
        <div className="overflow-hidden rounded-[calc(1.5rem-1.5px)] bg-white">
          <div className="flex flex-col gap-2 p-2.5 sm:flex-row sm:items-center">
            <div className="flex flex-1 items-center gap-3 px-4">
              <svg
                className="h-6 w-6 shrink-0 text-primary"
                fill="none"
                stroke="currentColor"
                strokeWidth={1.7}
                viewBox="0 0 24 24"
              >
                <path
                  d="M13.19 8.688a4.5 4.5 0 0 1 1.242 7.244l-4.5 4.5a4.5 4.5 0 0 1-6.364-6.364l1.757-1.757m13.35-.622 1.757-1.757a4.5 4.5 0 0 0-6.364-6.364l-4.5 4.5a4.5 4.5 0 0 0 1.242 7.244"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
              <input
                aria-label="Website URL"
                autoComplete="url"
                autoFocus={autoFocus}
                className="w-full bg-transparent py-4 text-lg text-slate-900 outline-none placeholder:text-slate-400 focus-visible:ring-0 focus-visible:ring-offset-0"
                list={sites?.length ? "known-sites" : undefined}
                onChange={(event) => {
                  setUrl(event.target.value);
                  if (error) setError("");
                }}
                placeholder="https://your-site.com"
                type="text"
                value={url}
              />
              {sites?.length ? (
                <datalist id="known-sites">
                  {sites.map((site) => (
                    <option key={site.id} value={site.url} />
                  ))}
                </datalist>
              ) : null}
            </div>
            <Button className="rounded-2xl" loading={submitting} size="lg" type="submit">
              {submitting ? submittingLabel : submitLabel}
            </Button>
          </div>

          <AdvancedOptions
            crawlDepth={crawlDepth}
            includeSubdomains={includeSubdomains}
            open={advancedOpen}
            selectedScans={selectedScans}
            setCrawlDepth={setCrawlDepth}
            setIncludeSubdomains={setIncludeSubdomains}
            setOpen={setAdvancedOpen}
            toggleScan={toggleScan}
          />
        </div>
      </div>

      {error && (
        <p
          className="mt-3 animate-fade-in text-sm font-medium text-red-600 dark:text-red-400"
          role="alert"
        >
          {error}
        </p>
      )}

      {footer}
    </form>
  );
}

function AdvancedOptions({
  crawlDepth,
  includeSubdomains,
  open,
  selectedScans,
  setCrawlDepth,
  setIncludeSubdomains,
  setOpen,
  toggleScan,
}: {
  crawlDepth: number;
  includeSubdomains: boolean;
  open: boolean;
  selectedScans: ScanOption[];
  setCrawlDepth: (value: number) => void;
  setIncludeSubdomains: (value: boolean) => void;
  setOpen: (value: boolean) => void;
  toggleScan: (scan: ScanOption) => void;
}) {
  return (
    <div className="border-t border-line/60">
      <button
        aria-expanded={open}
        className="flex w-full items-center justify-center gap-1.5 py-2.5 text-sm font-semibold text-muted transition hover:text-primary"
        onClick={() => setOpen(!open)}
        type="button"
      >
        Advanced options
        <svg
          className={`h-4 w-4 transition-transform ${open ? "rotate-180" : ""}`}
          fill="none"
          stroke="currentColor"
          strokeWidth={2}
          viewBox="0 0 24 24"
        >
          <path d="m6 9 6 6 6-6" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>

      {/* Smooth height reveal via the grid-rows 0fr→1fr trick. */}
      <div
        className={`grid transition-all duration-300 ease-out ${
          open ? "grid-rows-[1fr] opacity-100" : "grid-rows-[0fr] opacity-0"
        }`}
      >
        <div className="overflow-hidden">
          <div className="space-y-4 px-5 pb-5 text-left">
            <ScanChecklist selectedScans={selectedScans} toggleScan={toggleScan} />
            <ScopeFields
              crawlDepth={crawlDepth}
              includeSubdomains={includeSubdomains}
              setCrawlDepth={setCrawlDepth}
              setIncludeSubdomains={setIncludeSubdomains}
            />
          </div>
        </div>
      </div>
    </div>
  );
}
