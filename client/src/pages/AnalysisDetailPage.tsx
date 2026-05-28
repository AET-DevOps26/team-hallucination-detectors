import { useEffect } from "react";
import { FindingDetailsPanel } from "../components/analysis/FindingDetailsPanel";
import { FindingListItem } from "../components/analysis/FindingListItem";
import { SeveritySummary } from "../components/analysis/SeveritySummary";
import { scanLabels, severityStyles } from "../constants/scans";
import { Analysis, FindingStatus } from "../types/domain";
import { getSeverityCounts, sortOpenFindings } from "../utils/analysis";

type AnalysisDetailPageProps = {
  analysis?: Analysis;
  navigate: (path: string) => void;
  onSelectFinding: (id: string) => void;
  onUpdateFinding: (
    analysisId: string,
    findingId: string,
    status: FindingStatus,
  ) => void;
  resolutionReason: string;
  selectedFindingId: string;
  setResolutionReason: (value: string) => void;
};

export function AnalysisDetailPage({
  analysis,
  navigate,
  onSelectFinding,
  onUpdateFinding,
  resolutionReason,
  selectedFindingId,
  setResolutionReason,
}: AnalysisDetailPageProps) {
  const selectedFinding =
    analysis?.findings.find((finding) => finding.id === selectedFindingId) ??
    analysis?.findings[0];

  useEffect(() => {
    if (analysis?.findings[0] && !selectedFindingId) {
      onSelectFinding(analysis.findings[0].id);
    }
  }, [analysis, onSelectFinding, selectedFindingId]);

  if (!analysis) {
    return (
      <main className="rounded-md border border-zinc-300 bg-white p-6">
        <h2 className="text-2xl font-semibold">Analysis not found</h2>
        <button
          className="mt-5 rounded-md bg-teal-700 px-4 py-3 font-semibold text-white hover:bg-teal-800"
          onClick={() => navigate("/analysis")}
          type="button"
        >
          Back to analyses
        </button>
      </main>
    );
  }

  const severityCounts = getSeverityCounts(analysis.findings);
  const fixNext = sortOpenFindings(analysis.findings);

  return (
    <main className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_340px]">
      <section className="space-y-5">
        <AnalysisHeader analysis={analysis} navigate={navigate} counts={severityCounts} />
        <div className="grid gap-4 lg:grid-cols-[minmax(0,0.9fr)_minmax(360px,1.1fr)]">
          <div className="space-y-3">
            {analysis.findings.map((finding) => (
              <FindingListItem
                finding={finding}
                key={finding.id}
                onSelectFinding={onSelectFinding}
                selected={finding.id === selectedFinding?.id}
              />
            ))}
          </div>
          {selectedFinding && (
            <FindingDetailsPanel
              analysis={analysis}
              finding={selectedFinding}
              onUpdateFinding={onUpdateFinding}
              resolutionReason={resolutionReason}
              setResolutionReason={setResolutionReason}
            />
          )}
        </div>
      </section>
      <FixNext findings={fixNext} onSelectFinding={onSelectFinding} />
    </main>
  );
}

function AnalysisHeader({
  analysis,
  counts,
  navigate,
}: {
  analysis: Analysis;
  counts: ReturnType<typeof getSeverityCounts>;
  navigate: (path: string) => void;
}) {
  return (
    <div className="rounded-md border border-zinc-300 bg-white p-5">
      <button className="mb-4 rounded-md border border-zinc-300 px-3 py-2 text-sm font-semibold text-zinc-700 hover:border-teal-500" onClick={() => navigate("/analysis")} type="button">
        Back to analyses
      </button>
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <h2 className="text-2xl font-semibold">{analysis.siteName}</h2>
          <p className="mt-1 text-zinc-600">{analysis.url}</p>
          <p className="mt-2 text-sm text-zinc-500">
            {analysis.status} analysis from {analysis.createdAt}
          </p>
        </div>
        <SeveritySummary counts={counts} />
      </div>
      <div className="mt-5 flex flex-wrap gap-2">
        {analysis.selectedScans.map((scan) => (
          <span className="rounded bg-zinc-100 px-2 py-1 text-xs font-semibold text-zinc-700" key={scan}>
            {scanLabels[scan]}
          </span>
        ))}
      </div>
    </div>
  );
}

function FixNext({
  findings,
  onSelectFinding,
}: {
  findings: Analysis["findings"];
  onSelectFinding: (id: string) => void;
}) {
  return (
    <aside className="rounded-md border border-zinc-300 bg-white p-5">
      <h2 className="text-xl font-semibold">Fix next</h2>
      <div className="mt-4 space-y-3">
        {findings.map((finding) => (
          <button className="w-full rounded-md border border-zinc-200 p-3 text-left hover:border-teal-500" key={finding.id} onClick={() => onSelectFinding(finding.id)} type="button">
            <span className={`rounded border px-2 py-1 text-xs font-semibold ${severityStyles[finding.severity]}`}>
              {finding.severity}
            </span>
            <p className="mt-2 text-sm font-semibold">{finding.title}</p>
            <p className="mt-1 truncate text-xs text-zinc-500">{finding.affected}</p>
          </button>
        ))}
        {findings.length === 0 && (
          <p className="rounded-md bg-emerald-50 p-3 text-sm text-emerald-800">
            No open findings in this analysis.
          </p>
        )}
      </div>
    </aside>
  );
}
