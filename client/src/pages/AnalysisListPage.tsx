import { MiniMetric } from "../components/ui/Metric";
import { Analysis } from "../types/domain";
import { getSeverityCounts } from "../utils/analysis";

type AnalysisListPageProps = {
  analyses: Analysis[];
  navigate: (path: string) => void;
};

export function AnalysisListPage({ analyses, navigate }: AnalysisListPageProps) {
  return (
    <main className="space-y-5">
      <section className="flex flex-col gap-4 rounded-md border border-zinc-300 bg-white p-5 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <h2 className="text-2xl font-semibold">Analysis</h2>
          <p className="mt-1 text-zinc-600">
            Scans grouped by site. Open an analysis to inspect its findings.
          </p>
        </div>
        <button
          className="rounded-md bg-teal-700 px-4 py-3 font-semibold text-white hover:bg-teal-800"
          onClick={() => navigate("/analysis/new")}
          type="button"
        >
          Create new analysis
        </button>
      </section>

      <section className="grid gap-4">
        {analyses.map((analysis) => (
          <AnalysisCard analysis={analysis} key={analysis.id} navigate={navigate} />
        ))}
      </section>
    </main>
  );
}

function AnalysisCard({
  analysis,
  navigate,
}: {
  analysis: Analysis;
  navigate: (path: string) => void;
}) {
  const openCount = analysis.findings.filter(
    (finding) => finding.status === "Open",
  ).length;
  const severityCounts = getSeverityCounts(analysis.findings);

  return (
    <button
      className="rounded-md border border-zinc-300 bg-white p-5 text-left transition hover:border-teal-500 hover:shadow-sm"
      onClick={() => navigate(`/analysis/${analysis.id}`)}
      type="button"
    >
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <h3 className="text-xl font-semibold">{analysis.siteName}</h3>
            <span className="rounded bg-zinc-100 px-2 py-1 text-xs font-semibold text-zinc-700">
              {analysis.status}
            </span>
          </div>
          <p className="mt-1 truncate text-sm text-zinc-500">{analysis.url}</p>
          <p className="mt-2 text-sm text-zinc-500">
            Created {analysis.createdAt}
          </p>
        </div>
        <div className="grid grid-cols-3 gap-3 text-center sm:grid-cols-6">
          <MiniMetric label="Open" value={String(openCount)} />
          {severityCounts.map(({ severity, count }) => (
            <MiniMetric key={severity} label={severity} value={String(count)} />
          ))}
        </div>
      </div>
    </button>
  );
}
