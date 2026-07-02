import { useEffect, useMemo, useState } from "react";
import { generateFixPrompt } from "../api/genai";
import { downloadReport, getReportData, ReportData } from "../api/reports";
import { getScanComparison, ApiScanComparison } from "../api/scans";
import { FindingDetailsPanel } from "../components/analysis/FindingDetailsPanel";
import { FindingListItem } from "../components/analysis/FindingListItem";
import { SeveritySummary } from "../components/analysis/SeveritySummary";
import { scanLabels, severityStyles } from "../constants/scans";
import { Analysis, Finding, FindingStatus } from "../types/domain";
import { getSeverityCounts } from "../utils/analysis";

type AnalysisDetailPageProps = {
  analysis?: Analysis;
  navigate: (path: string) => void;
  onSelectFinding: (id: string) => void;
  onRescan: (analysisId: string) => Promise<void>;
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
  onRescan,
  onUpdateFinding,
  resolutionReason,
  selectedFindingId,
  setResolutionReason,
}: AnalysisDetailPageProps) {
  const [report, setReport] = useState<ReportData>();
  const [reportStatus, setReportStatus] = useState<"idle" | "loading" | "error">("idle");
  const [reportError, setReportError] = useState("");
  const [comparison, setComparison] = useState<ApiScanComparison>();
  const [comparisonStatus, setComparisonStatus] = useState<"idle" | "loading" | "error">("idle");
  const [comparisonError, setComparisonError] = useState("");
  const [rescanStatus, setRescanStatus] = useState<"idle" | "loading" | "error">("idle");
  const [rescanError, setRescanError] = useState("");
  const selectedFinding =
    analysis?.findings.find((finding) => finding.id === selectedFindingId) ??
    analysis?.findings[0];
  const reportAnalysisId = analysis?.id;
  const reportAnalysisStatus = analysis?.status;

  useEffect(() => {
    if (analysis?.findings[0] && !selectedFindingId) {
      onSelectFinding(analysis.findings[0].id);
    }
  }, [analysis, onSelectFinding, selectedFindingId]);

  useEffect(() => {
    if (!reportAnalysisId || reportAnalysisStatus !== "Completed") {
      setReport(undefined);
      setReportStatus("idle");
      setReportError("");
      return;
    }
    let cancelled = false;
    setReportStatus("loading");
    setReportError("");
    getReportData(reportAnalysisId)
      .then((data) => {
        if (!cancelled) {
          setReport(data);
          setReportStatus("idle");
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setReportError(error instanceof Error ? error.message : "Could not load the report.");
          setReportStatus("error");
        }
      });
    return () => {
      cancelled = true;
    };
  }, [reportAnalysisId, reportAnalysisStatus]);

  useEffect(() => {
    if (!reportAnalysisId || reportAnalysisStatus !== "Completed") {
      setComparison(undefined);
      setComparisonStatus("idle");
      setComparisonError("");
      return;
    }
    let cancelled = false;
    setComparisonStatus("loading");
    setComparisonError("");
    getScanComparison(Number(reportAnalysisId))
      .then((data) => {
        if (!cancelled) {
          setComparison(data);
          setComparisonStatus("idle");
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setComparisonError(error instanceof Error ? error.message : "Could not load scan comparison.");
          setComparisonStatus("error");
        }
      });
    return () => {
      cancelled = true;
    };
  }, [reportAnalysisId, reportAnalysisStatus]);

  // Findings the comparison flags as new in this scan, keyed by finding id, so the
  // list can call them out inline instead of repeating them in a second list.
  const newFindingIds = useMemo(() => {
    const ids = new Set<string>();
    comparison?.findings.forEach((finding) => {
      if (finding.changeStatus === "Newly introduced" && finding.findingId != null) {
        ids.add(String(finding.findingId));
      }
    });
    return ids;
  }, [comparison]);

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
  const inProgress = analysis.status === "Pending" || analysis.status === "Running";
  const analysisId = analysis.id;

  async function handleRescan() {
    setRescanStatus("loading");
    setRescanError("");
    try {
      await onRescan(analysisId);
      setRescanStatus("idle");
    } catch (error) {
      setRescanError(error instanceof Error ? error.message : "Could not start a rescan.");
      setRescanStatus("error");
    }
  }

  return (
    <main className="space-y-5">
      <AnalysisHeader
        analysis={analysis}
        navigate={navigate}
        counts={severityCounts}
        inProgress={inProgress}
        onRescan={handleRescan}
        rescanLoading={rescanStatus === "loading"}
      />
      {rescanStatus === "error" && (
        <p className="rounded-md bg-red-50 p-3 text-sm text-red-800">
          {rescanError}
        </p>
      )}
      <ComparisonSummary
        comparison={comparison}
        comparisonError={comparisonError}
        comparisonStatus={comparisonStatus}
        ready={analysis.status === "Completed"}
      />

      <section className="grid gap-4 lg:grid-cols-[260px_minmax(0,1fr)_minmax(300px,380px)]">
        <div className="space-y-2">
          {analysis.findings.map((finding) => (
            <FindingListItem
              finding={finding}
              isNew={newFindingIds.has(finding.id)}
              key={finding.id}
              onSelectFinding={onSelectFinding}
              selected={finding.id === selectedFinding?.id}
            />
          ))}
        </div>
        {selectedFinding && (
          <>
            <FindingDetailsPanel
              analysis={analysis}
              finding={selectedFinding}
              onUpdateFinding={onUpdateFinding}
              resolutionReason={resolutionReason}
              setResolutionReason={setResolutionReason}
            />
            <GenerateFixPrompt finding={selectedFinding} />
          </>
        )}
      </section>

      <ReportPanel
        analysis={analysis}
        report={report}
        reportError={reportError}
        reportStatus={reportStatus}
      />
    </main>
  );
}

function ReportPanel({
  analysis,
  report,
  reportError,
  reportStatus,
}: {
  analysis: Analysis;
  report?: ReportData;
  reportError: string;
  reportStatus: "idle" | "loading" | "error";
}) {
  const [downloadStatus, setDownloadStatus] = useState<"idle" | "loading" | "error">("idle");
  const [downloadError, setDownloadError] = useState("");
  const ready = analysis.status === "Completed";

  async function handleDownload(kind: "summary-html" | "summary-pdf" | "full-pdf") {
    setDownloadStatus("loading");
    setDownloadError("");
    try {
      await downloadReport(analysis.id, kind);
      setDownloadStatus("idle");
    } catch (error) {
      setDownloadError(error instanceof Error ? error.message : "Could not download the report.");
      setDownloadStatus("error");
    }
  }

  return (
    <section className="rounded-md border border-zinc-300 bg-white p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-xl font-semibold">Export</h2>
          <p className="mt-1 text-sm text-zinc-600">
            Executive summary or full scan report for launch review.
          </p>
        </div>
        {ready && report && (
          <div className="flex flex-wrap gap-2">
            <button
              className="rounded-md bg-teal-700 px-3 py-2 text-sm font-semibold text-white hover:bg-teal-800 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={downloadStatus === "loading"}
              onClick={() => handleDownload("summary-pdf")}
              type="button"
            >
              Summary PDF
            </button>
            <button
              className="rounded-md border border-zinc-300 px-3 py-2 text-sm font-semibold text-zinc-700 hover:border-teal-500 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={downloadStatus === "loading"}
              onClick={() => handleDownload("summary-html")}
              type="button"
            >
              HTML
            </button>
            <button
              className="rounded-md border border-zinc-300 px-3 py-2 text-sm font-semibold text-zinc-700 hover:border-teal-500 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={downloadStatus === "loading"}
              onClick={() => handleDownload("full-pdf")}
              type="button"
            >
              Full PDF
            </button>
          </div>
        )}
      </div>

      {!ready && (
        <p className="mt-4 rounded-md bg-zinc-50 p-3 text-sm text-zinc-600">
          Reports are available after this scan completes.
        </p>
      )}

      {ready && reportStatus === "loading" && (
        <p className="mt-4 rounded-md bg-zinc-50 p-3 text-sm text-zinc-600">
          Preparing report preview…
        </p>
      )}

      {ready && reportStatus === "error" && (
        <p className="mt-4 rounded-md bg-red-50 p-3 text-sm text-red-800">
          {reportError}
        </p>
      )}

      {ready && report && (
        <div className="mt-4 grid gap-4 lg:grid-cols-[220px_minmax(0,1fr)]">
          <div className="rounded-md border border-zinc-200 p-3">
            <p className="text-xs font-semibold uppercase text-zinc-500">Safe to launch</p>
            <p className="mt-1 text-lg font-semibold text-zinc-900">{report.safeToLaunch.status}</p>
            <p className="text-sm text-zinc-600">
              {report.safeToLaunch.blockingIssues} checklist item
              {report.safeToLaunch.blockingIssues === 1 ? "" : "s"} need attention.
            </p>
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            {report.safeToLaunch.items.map((item) => (
              <div className="rounded-md border border-zinc-200 p-2 text-sm" key={item.label}>
                <div className="flex items-start justify-between gap-2">
                  <span className="font-medium text-zinc-800">{item.label}</span>
                  <span className={`shrink-0 rounded px-2 py-0.5 text-xs font-semibold ${checklistResultClass(item.result)}`}>
                    {item.result}
                  </span>
                </div>
                <span className="mt-1 block text-xs text-zinc-500">{item.reason}</span>
              </div>
            ))}
          </div>

          {report.executiveSummary.recommendedNextSteps.length > 0 && (
            <div className="lg:col-span-2">
              <p className="text-sm font-semibold text-zinc-800">Recommended next steps</p>
              <ol className="mt-2 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                {report.executiveSummary.recommendedNextSteps.slice(0, 3).map((step) => (
                  <li className="rounded-md border border-zinc-200 p-2 text-sm" key={`${step.order}-${step.title}`}>
                    <span className="font-medium text-zinc-800">
                      {step.order}. {step.title}
                    </span>
                    <span className="block text-xs text-zinc-500">
                      {step.severity} · {step.effort.level} · {step.effort.estimate}
                    </span>
                  </li>
                ))}
              </ol>
            </div>
          )}

          {downloadStatus === "error" && (
            <p className="lg:col-span-2 rounded-md bg-red-50 p-3 text-sm text-red-800">
              {downloadError}
            </p>
          )}
        </div>
      )}
    </section>
  );
}

function checklistResultClass(result: string) {
  if (result === "Pass") {
    return "bg-emerald-50 text-emerald-800";
  }
  if (result === "Needs attention" || result === "Incomplete") {
    return "bg-red-50 text-red-800";
  }
  return "bg-zinc-100 text-zinc-600";
}

/**
 * A compact "what changed" strip. Per-finding changes are surfaced inline on the
 * findings list (a "New" badge) instead of being repeated here, so this only
 * needs to carry the headline counts.
 */
function ComparisonSummary({
  comparison,
  comparisonError,
  comparisonStatus,
  ready,
}: {
  comparison?: ApiScanComparison;
  comparisonError: string;
  comparisonStatus: "idle" | "loading" | "error";
  ready: boolean;
}) {
  if (!ready) {
    return null;
  }
  if (comparisonStatus === "loading") {
    return (
      <p className="rounded-md bg-zinc-50 px-4 py-2 text-sm text-zinc-600">
        Loading comparison with the previous scan…
      </p>
    );
  }
  if (comparisonStatus === "error") {
    return (
      <p className="rounded-md bg-red-50 px-4 py-2 text-sm text-red-800">
        {comparisonError}
      </p>
    );
  }
  if (!comparison) {
    return null;
  }
  if (!comparison.comparable) {
    return (
      <p className="rounded-md bg-zinc-50 px-4 py-2 text-sm text-zinc-600">
        {comparison.message}
      </p>
    );
  }

  return (
    <section className="flex flex-wrap items-center gap-3 rounded-md border border-zinc-300 bg-white px-4 py-3">
      <h2 className="text-sm font-semibold text-zinc-800">Since last scan</h2>
      <div className="flex flex-wrap gap-2">
        <MiniChangeMetric label="Fixed" value={comparison.summary.fixed} tone="good" />
        <MiniChangeMetric label="Still open" value={comparison.summary.stillPresent} tone="warn" />
        <MiniChangeMetric label="New" value={comparison.summary.newlyIntroduced} tone="bad" />
      </div>
      {comparison.summary.newlyIntroduced > 0 && (
        <p className="text-xs text-zinc-500">
          New findings are flagged "New" in the list below.
        </p>
      )}
    </section>
  );
}

function MiniChangeMetric({
  label,
  tone,
  value,
}: {
  label: string;
  tone: "good" | "warn" | "bad";
  value: number;
}) {
  const toneClass = {
    good: "bg-emerald-50 text-emerald-800",
    warn: "bg-amber-50 text-amber-800",
    bad: "bg-red-50 text-red-800",
  }[tone];
  return (
    <div className={`flex items-center gap-1.5 rounded-md px-2.5 py-1 ${toneClass}`}>
      <p className="text-sm font-semibold">{value}</p>
      <p className="text-xs font-medium">{label}</p>
    </div>
  );
}

function AnalysisHeader({
  analysis,
  counts,
  inProgress,
  navigate,
  onRescan,
  rescanLoading,
}: {
  analysis: Analysis;
  counts: ReturnType<typeof getSeverityCounts>;
  inProgress: boolean;
  navigate: (path: string) => void;
  onRescan: () => void;
  rescanLoading: boolean;
}) {
  return (
    <div className="rounded-md border border-zinc-300 bg-white p-5">
      <div className="mb-4 flex flex-wrap gap-2">
        <button className="rounded-md border border-zinc-300 px-3 py-2 text-sm font-semibold text-zinc-700 hover:border-teal-500" onClick={() => navigate("/analysis")} type="button">
          Back to analyses
        </button>
        <button
          className="rounded-md bg-teal-700 px-3 py-2 text-sm font-semibold text-white hover:bg-teal-800 disabled:cursor-not-allowed disabled:opacity-60"
          disabled={inProgress || rescanLoading}
          onClick={onRescan}
          type="button"
        >
          {rescanLoading ? "Starting…" : "Rerun scan"}
        </button>
      </div>
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

/**
 * VibeShield's core GenAI surface: turns the selected finding into a single,
 * ready-to-paste prompt for the user's AI builder (Lovable, Cursor, v0, Bolt,
 * Replit). The user never reads or writes code — the same kind of AI that built
 * the site repairs it. Rendered directly beside the finding it targets, so the
 * "here's the issue" and "here's the fix" panels never drift apart.
 */
const AI_BUILDERS = ["Generic", "Lovable", "Cursor", "v0", "Bolt", "Replit"] as const;
type AiBuilder = (typeof AI_BUILDERS)[number];

function GenerateFixPrompt({ finding }: { finding?: Finding }) {
  const [prompt, setPrompt] = useState("");
  const [status, setStatus] = useState<"idle" | "loading" | "error">("idle");
  const [error, setError] = useState("");
  const [copied, setCopied] = useState(false);
  const [builder, setBuilder] = useState<AiBuilder>("Generic");

  // A different finding is a different prompt — clear any stale output.
  useEffect(() => {
    setPrompt("");
    setStatus("idle");
    setError("");
    setCopied(false);
  }, [finding?.id]);

  async function handleGenerate() {
    if (!finding) return;
    setStatus("loading");
    setError("");
    setCopied(false);
    try {
      const result = await generateFixPrompt(finding, builder);
      setPrompt(result);
      setStatus("idle");
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Could not generate a fix prompt. Please try again.",
      );
      setStatus("error");
    }
  }

  async function handleCopy() {
    await navigator.clipboard.writeText(prompt);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 2000);
  }

  return (
    <aside className="flex flex-col rounded-md border border-teal-200 bg-teal-50/40 p-5">
      <div className="flex items-start justify-between gap-2">
        <h2 className="text-xl font-semibold">Fix prompt</h2>
        {finding && (
          <span
            className={`shrink-0 rounded border px-2 py-1 text-xs font-semibold ${severityStyles[finding.severity]}`}
          >
            {finding.severity}
          </span>
        )}
      </div>
      <p className="mt-1 text-sm text-zinc-600">
        {finding
          ? `Ready-to-paste prompt for "${finding.title}" — hand it to your AI builder.`
          : "Select a finding to generate a fix prompt."}
      </p>

      {finding && (
        <div className="mt-4 flex flex-1 flex-col space-y-4">
          <div className="space-y-1">
            <label className="text-xs font-semibold text-zinc-600" htmlFor="builder-select">
              AI builder
            </label>
            <select
              className="w-full rounded-md border border-zinc-300 bg-white px-3 py-2 text-sm text-zinc-800 outline-none focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
              id="builder-select"
              onChange={(e) => setBuilder(e.target.value as AiBuilder)}
              value={builder}
            >
              {AI_BUILDERS.map((b) => (
                <option key={b} value={b}>{b}</option>
              ))}
            </select>
          </div>

          <button
            className="w-full rounded-md bg-teal-700 px-4 py-3 font-semibold text-white hover:bg-teal-800 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={status === "loading"}
            onClick={handleGenerate}
            type="button"
          >
            {status === "loading"
              ? "Generating…"
              : prompt
                ? "Regenerate fix prompt"
                : "Generate fix prompt"}
          </button>

          {status === "error" && (
            <p className="rounded-md bg-red-50 p-3 text-sm text-red-800">
              {error}
            </p>
          )}

          {prompt && (
            <div className="flex flex-1 flex-col space-y-2">
              <textarea
                className="min-h-48 w-full flex-1 rounded-md border border-zinc-300 bg-white px-3 py-2 font-mono text-xs text-zinc-800 outline-none focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
                readOnly
                value={prompt}
              />
              <button
                className="w-full rounded-md border border-zinc-300 bg-white px-4 py-2 text-sm font-semibold text-zinc-700 hover:border-teal-500"
                onClick={handleCopy}
                type="button"
              >
                {copied ? "Copied" : "Copy prompt"}
              </button>
            </div>
          )}
        </div>
      )}
    </aside>
  );
}
