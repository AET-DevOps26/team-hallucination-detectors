import { useState } from "react";
import { Badge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import { MiniMetric } from "../components/ui/Metric";
import { Analysis } from "../types/domain";
import { getSeverityCounts } from "../utils/analysis";

type AnalysisListPageProps = {
  analyses: Analysis[];
  navigate: (path: string) => void;
};

type SiteGroup = {
  siteId: string;
  siteName: string;
  url: string;
  scans: Analysis[];
};

/** Buckets scans under their site, newest scan first, sites ordered by latest scan. */
function groupBySite(analyses: Analysis[]): SiteGroup[] {
  const groups = new Map<string, SiteGroup>();
  for (const analysis of analyses) {
    const existing = groups.get(analysis.siteId);
    if (existing) {
      existing.scans.push(analysis);
    } else {
      groups.set(analysis.siteId, {
        siteId: analysis.siteId,
        siteName: analysis.siteName,
        url: analysis.url,
        scans: [analysis],
      });
    }
  }
  // `analyses` arrives newest-first, so each group's scans and the group order
  // both preserve that; the first scan seen per site is its most recent.
  return [...groups.values()];
}

export function AnalysisListPage({ analyses, navigate }: AnalysisListPageProps) {
  const groups = groupBySite(analyses);

  return (
    <main className="animate-fade-in space-y-5">
      <section className="flex flex-col gap-4 rounded-xl border border-line bg-surface p-6 shadow-card lg:flex-row lg:items-start lg:justify-between">
        <div>
          <h2 className="text-2xl font-semibold text-fg">Analysis</h2>
          <p className="mt-1 text-muted">
            Scans grouped by site. Expand a site to inspect its scans.
          </p>
        </div>
        <Button onClick={() => navigate("/analysis/new")} size="lg">
          Create new analysis
        </Button>
      </section>

      <section className="grid gap-4">
        {groups.length === 0 ? (
          <div className="flex flex-col items-center gap-4 rounded-xl border border-dashed border-line bg-surface px-6 py-16 text-center animate-slide-up">
            <div className="rounded-full bg-primary/10 p-4">
              <svg className="h-8 w-8 text-primary" fill="none" stroke="currentColor" strokeWidth={1.5} viewBox="0 0 24 24">
                <path d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </div>
            <div>
              <h3 className="text-lg font-semibold text-fg">No analyses yet</h3>
              <p className="mt-1 text-sm text-muted">
                Start by scanning your first site. VibeShield checks for common security issues and generates AI-powered fix prompts.
              </p>
            </div>
            <Button onClick={() => navigate("/analysis/new")}>Scan your first site</Button>
          </div>
        ) : (
          groups.map((group) => (
            <SiteGroupCard group={group} key={group.siteId} navigate={navigate} />
          ))
        )}
      </section>
    </main>
  );
}

function SiteGroupCard({
  group,
  navigate,
}: {
  group: SiteGroup;
  navigate: (path: string) => void;
}) {
  const [expanded, setExpanded] = useState(true);
  const latest = group.scans[0];
  const inProgress = latest.status === "Pending" || latest.status === "Running";
  const openCount = latest.findings.filter(
    (finding) => finding.status === "Open",
  ).length;
  const severityCounts = getSeverityCounts(latest.findings);
  const scanLabel = group.scans.length === 1 ? "1 scan" : `${group.scans.length} scans`;

  return (
    <div className="overflow-hidden rounded-xl border border-line bg-surface shadow-card">
      <button
        aria-expanded={expanded}
        className="flex w-full flex-col gap-4 p-5 text-left transition hover:bg-line/30 lg:flex-row lg:items-start lg:justify-between"
        onClick={() => setExpanded((open) => !open)}
        type="button"
      >
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <ChevronIcon expanded={expanded} />
            <h3 className="text-xl font-semibold text-fg">{group.siteName}</h3>
            <Badge tone="neutral">{scanLabel}</Badge>
            {inProgress && (
              <Badge tone="primary">
                <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-current" />
                {latest.status}
              </Badge>
            )}
          </div>
          <p className="mt-1 truncate text-sm text-muted">{group.url}</p>
          <p className="mt-2 text-sm text-muted">Latest scan {latest.createdAt}</p>
        </div>
        <div className="grid grid-cols-3 gap-3 text-center sm:grid-cols-6">
          <MiniMetric label="Open" value={String(openCount)} />
          {severityCounts.map(({ severity, count }) => (
            <MiniMetric key={severity} label={severity} value={String(count)} />
          ))}
        </div>
      </button>

      {expanded && (
        <ul className="divide-y divide-line border-t border-line">
          {group.scans.map((scan) => (
            <ScanRow key={scan.id} navigate={navigate} scan={scan} />
          ))}
        </ul>
      )}
    </div>
  );
}

function ScanRow({
  scan,
  navigate,
}: {
  scan: Analysis;
  navigate: (path: string) => void;
}) {
  const inProgress = scan.status === "Pending" || scan.status === "Running";
  const openCount = scan.findings.filter(
    (finding) => finding.status === "Open",
  ).length;

  return (
    <li>
      <button
        className="flex w-full items-center justify-between gap-4 px-5 py-3 text-left transition hover:bg-primary/5"
        onClick={() => navigate(`/analysis/${scan.id}`)}
        type="button"
      >
        <div className="flex min-w-0 items-center gap-3">
          {inProgress && (
            <Badge tone="primary">
              <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-current" />
              {scan.status}
            </Badge>
          )}
          <span className="truncate text-sm text-fg">{scan.createdAt}</span>
        </div>
        <div className="flex shrink-0 items-center gap-3 text-sm text-muted">
          <span>{openCount} open</span>
          <span aria-hidden="true" className="text-muted">→</span>
        </div>
      </button>
    </li>
  );
}

function ChevronIcon({ expanded }: { expanded: boolean }) {
  return (
    <svg
      aria-hidden="true"
      className={`h-4 w-4 shrink-0 text-muted transition-transform ${expanded ? "rotate-90" : ""}`}
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      viewBox="0 0 24 24"
    >
      <path d="M8.25 4.5l7.5 7.5-7.5 7.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
