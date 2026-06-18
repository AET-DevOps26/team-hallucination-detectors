import { FormEvent, useState } from "react";
import { ProfileRow } from "../components/ui/ProfileRow";
import { scanDescriptions, scanLabels } from "../constants/scans";
import { NewAnalysisInput, ScanOption, Site } from "../types/domain";

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
    "https",
    "headers",
    "sensitiveFiles",
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

    let parsed: URL;
    try {
      parsed = new URL(url);
    } catch {
      setFormError("Enter a valid URL.");
      return;
    }
    if (!["http:", "https:"].includes(parsed.protocol)) {
      setFormError("Enter a valid http or https URL.");
      return;
    }
    if (selectedScans.length === 0) {
      setFormError("Choose at least one scan.");
      return;
    }

    setSubmitting(true);
    try {
      await createAnalysis({
        url: parsed.toString().replace(/\/$/, ""),
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
    <main className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_360px]">
      <section className="rounded-md border border-zinc-300 bg-white p-5">
        <h2 className="text-2xl font-semibold">Create new analysis</h2>
        <p className="mt-1 text-zinc-600">
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
          {formError && (
            <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
              {formError}
            </p>
          )}
          <div className="flex flex-col gap-3 sm:flex-row">
            <button className="rounded-md bg-teal-700 px-4 py-3 font-semibold text-white hover:bg-teal-800 disabled:cursor-not-allowed disabled:opacity-60" disabled={submitting} type="submit">
              {submitting ? "Starting…" : "Start analysis"}
            </button>
            <button className="rounded-md border border-zinc-300 px-4 py-3 font-semibold text-zinc-700 hover:border-teal-500" onClick={() => navigate("/analysis")} type="button">
              Cancel
            </button>
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
      <span className="text-sm font-medium text-zinc-700">Target URL</span>
      <input className="mt-2 w-full rounded-md border border-zinc-300 px-3 py-3 text-base outline-none transition focus:border-teal-600 focus:ring-2 focus:ring-teal-100" list="known-sites" onChange={(event) => setUrl(event.target.value)} placeholder="https://example.com" required type="url" value={url} />
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
      <p className="text-sm font-medium text-zinc-700">Specific scans</p>
      <div className="mt-3 grid gap-3 sm:grid-cols-2">
        {(Object.keys(scanLabels) as ScanOption[]).map((scan) => (
          <label className="flex cursor-pointer items-start gap-3 rounded-md border border-zinc-200 p-3 hover:border-teal-500" key={scan}>
            <input checked={selectedScans.includes(scan)} className="mt-1" onChange={() => toggleScan(scan)} type="checkbox" />
            <span className="block text-sm font-semibold">{scanLabels[scan]}</span>
          </label>
        ))}
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
        <span className="text-sm font-medium text-zinc-700">Crawl depth</span>
        <input className="mt-2 w-full rounded-md border border-zinc-300 px-3 py-3 text-base outline-none transition focus:border-teal-600 focus:ring-2 focus:ring-teal-100" max={3} min={0} onChange={(event) => setCrawlDepth(Number(event.target.value))} type="number" value={crawlDepth} />
      </label>
      <label className="flex items-center gap-3 rounded-md border border-zinc-200 p-3">
        <input checked={includeSubdomains} onChange={(event) => setIncludeSubdomains(event.target.checked)} type="checkbox" />
        <span className="text-sm font-semibold">Include subdomains</span>
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
    <aside className="rounded-md border border-zinc-300 bg-white p-5">
      <h2 className="text-xl font-semibold">Selected scope</h2>
      <div className="mt-4 space-y-3">
        <ProfileRow label="URL" value={url || "Not set"} />
        <ProfileRow label="Crawl depth" value={String(crawlDepth)} />
        <ProfileRow label="Subdomains" value={includeSubdomains ? "Included" : "Excluded"} />
      </div>
      {selectedScans.length > 0 && (
        <div className="mt-5">
          <p className="text-sm font-medium text-zinc-700">What will be checked</p>
          <ul className="mt-3 space-y-3">
            {selectedScans.map((scan) => (
              <li className="flex gap-2" key={scan}>
                <span className="mt-0.5 text-teal-600">✓</span>
                <div>
                  <p className="text-sm font-semibold text-zinc-800">{scanLabels[scan]}</p>
                  <p className="text-xs text-zinc-500">{scanDescriptions[scan]}</p>
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </aside>
  );
}
