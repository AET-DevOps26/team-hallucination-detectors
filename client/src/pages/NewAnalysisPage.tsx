import { FormEvent, useState } from "react";
import { Alert } from "../components/ui/Alert";
import { Button } from "../components/ui/Button";
import { ProfileRow } from "../components/ui/ProfileRow";
import { defaultScanSelection, scanDescriptions, scanLabels } from "../constants/scans";
import { NewAnalysisInput, ScanOption, Site } from "../types/domain";
import { validateTargetUrl } from "../utils/url";

type NewAnalysisPageProps = {
  createAnalysis: (input: NewAnalysisInput) => Promise<void>;
  navigate: (path: string) => void;
  sites: Site[];
};

export function NewAnalysisPage({
  createAnalysis,
  navigate,
  sites,
}: NewAnalysisPageProps) {
  const [url, setUrl] = useState(sites[0]?.url ?? "");
  // Contract bounds: crawlDepth 0–3, 0 = only the entered URL.
  const [crawlDepth, setCrawlDepth] = useState(0);
  const [includeSubdomains, setIncludeSubdomains] = useState(false);
  const [selectedScans, setSelectedScans] = useState<ScanOption[]>([
    ...defaultScanSelection,
  ]);
  const [formError, setFormError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  function toggleScan(scan: ScanOption) {
    setSelectedScans((current) =>
      current.includes(scan)
        ? current.filter((item) => item !== scan)
        : [...current, scan],
    );
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError("");

    const result = validateTargetUrl(url);
    if (!result.ok) {
      setFormError(result.error);
      return;
    }
    if (selectedScans.length === 0) {
      setFormError("Choose at least one scan.");
      return;
    }

    setSubmitting(true);
    try {
      await createAnalysis({
        url: result.url,
        selectedScans,
        crawlDepth,
        includeSubdomains,
      });
    } catch (error) {
      // Surfaces backend rejections, e.g. "A scan for this website is already
      // pending or running." (409) — the duplicate-trigger guard of #11.
      setFormError(
        error instanceof Error ? error.message : "Starting the analysis failed.",
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="animate-fade-in grid gap-5 lg:grid-cols-[minmax(0,1fr)_360px]">
      <section className="rounded-xl border border-line bg-surface p-6 shadow-card">
        <h2 className="text-2xl font-semibold text-fg">Create new analysis</h2>
        <p className="mt-1 text-muted">
          Enter the target URL and select which checks should run.
        </p>
        <form className="mt-6 space-y-5" onSubmit={submit}>
          <TargetUrlInput setUrl={setUrl} sites={sites} url={url} />
          <ScanOptions selectedScans={selectedScans} toggleScan={toggleScan} />
          <ScopeControls
            crawlDepth={crawlDepth}
            includeSubdomains={includeSubdomains}
            setCrawlDepth={setCrawlDepth}
            setIncludeSubdomains={setIncludeSubdomains}
          />
          {formError && <Alert tone="error">{formError}</Alert>}
          <div className="flex flex-col gap-3 sm:flex-row">
            <Button loading={submitting} size="lg" type="submit">
              {submitting ? "Starting…" : "Start analysis"}
            </Button>
            <Button onClick={() => navigate("/analysis")} size="lg" variant="secondary">
              Cancel
            </Button>
          </div>
        </form>
      </section>
      <SelectedScope
        crawlDepth={crawlDepth}
        includeSubdomains={includeSubdomains}
        selectedScans={selectedScans}
        url={url}
      />
    </main>
  );
}

function TargetUrlInput({
  setUrl,
  sites,
  url,
}: {
  setUrl: (value: string) => void;
  sites: Site[];
  url: string;
}) {
  return (
    <label className="block">
      <span className="text-sm font-medium text-muted">Target URL</span>
      <input
        className="mt-2 w-full rounded-lg border border-line bg-surface px-3 py-3 text-base text-fg outline-none transition placeholder:text-muted focus:border-primary focus:ring-2 focus:ring-primary/20"
        list="known-sites"
        onChange={(event) => setUrl(event.target.value)}
        placeholder="https://example.com"
        required
        type="url"
        value={url}
      />
      <datalist id="known-sites">
        {sites.map((site) => (
          <option key={site.id} value={site.url} />
        ))}
      </datalist>
    </label>
  );
}

function ScanOptions({
  selectedScans,
  toggleScan,
}: {
  selectedScans: ScanOption[];
  toggleScan: (scan: ScanOption) => void;
}) {
  return (
    <div>
      <p className="text-sm font-medium text-muted">Specific scans</p>
      <div className="mt-3 grid gap-3 sm:grid-cols-2">
        {(Object.keys(scanLabels) as ScanOption[]).map((scan) => {
          const checked = selectedScans.includes(scan);
          return (
            <label
              className={`flex cursor-pointer items-start gap-3 rounded-lg border p-3 transition ${
                checked
                  ? "border-primary bg-primary/5"
                  : "border-line hover:border-primary"
              }`}
              key={scan}
            >
              <input
                checked={checked}
                className="mt-1 accent-[rgb(var(--primary))]"
                onChange={() => toggleScan(scan)}
                type="checkbox"
              />
              <span className="block text-sm font-semibold text-fg">{scanLabels[scan]}</span>
            </label>
          );
        })}
      </div>
    </div>
  );
}

function ScopeControls({
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
    <div className="grid gap-4 sm:grid-cols-2">
      <label className="block">
        <span className="text-sm font-medium text-muted">Crawl depth</span>
        <input
          className="mt-2 w-full rounded-lg border border-line bg-surface px-3 py-3 text-base text-fg outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20"
          max={3}
          min={0}
          onChange={(event) => setCrawlDepth(Number(event.target.value))}
          type="number"
          value={crawlDepth}
        />
      </label>
      <label className="flex items-center gap-3 rounded-lg border border-line p-3">
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

function SelectedScope({
  crawlDepth,
  includeSubdomains,
  selectedScans,
  url,
}: {
  crawlDepth: number;
  includeSubdomains: boolean;
  selectedScans: ScanOption[];
  url: string;
}) {
  return (
    <aside className="rounded-xl border border-line bg-surface p-6 shadow-card lg:sticky lg:top-24 lg:self-start">
      <h2 className="text-xl font-semibold text-fg">Selected scope</h2>
      <div className="mt-4 space-y-3">
        <ProfileRow label="URL" value={url || "Not set"} />
        <ProfileRow label="Crawl depth" value={String(crawlDepth)} />
        <ProfileRow label="Subdomains" value={includeSubdomains ? "Included" : "Excluded"} />
      </div>
      {selectedScans.length > 0 && (
        <div className="mt-5">
          <p className="text-sm font-medium text-muted">What will be checked</p>
          <ul className="mt-3 space-y-3">
            {selectedScans.map((scan) => (
              <li className="flex gap-2" key={scan}>
                <span className="mt-0.5 text-primary">✓</span>
                <div>
                  <p className="text-sm font-semibold text-fg">{scanLabels[scan]}</p>
                  <p className="text-xs text-muted">{scanDescriptions[scan]}</p>
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </aside>
  );
}
