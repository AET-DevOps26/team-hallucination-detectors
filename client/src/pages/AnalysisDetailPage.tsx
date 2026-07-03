import { useEffect, useMemo, useState } from "react";
import { generateFixPrompt } from "../api/genai";
import { LLM_PROVIDERS, useLlmProvider } from "../hooks/useLlmProvider";
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

      <section className="grid gap-5 xl:grid-cols-[320px_minmax(0,1fr)]">
        <aside className="xl:sticky xl:top-5 xl:self-start">
          <ReportPanel
            analysis={analysis}
            report={report}
            reportError={reportError}
            reportStatus={reportStatus}
          />
        </aside>

        <div className="space-y-5">
          <FindingsPanel
            analysis={analysis}
            comparison={comparison}
            newFindingIds={newFindingIds}
            onSelectFinding={onSelectFinding}
            selectedFindingId={selectedFinding?.id}
          />

          {selectedFinding && (
            <section className="grid gap-5 2xl:grid-cols-[minmax(0,1fr)_340px]">
              <FindingDetailsPanel
                analysis={analysis}
                finding={selectedFinding}
                onUpdateFinding={onUpdateFinding}
                resolutionReason={resolutionReason}
                setResolutionReason={setResolutionReason}
              />
              <GenerateFixPrompt finding={selectedFinding} />
            </section>
          )}
        </div>
      </section>
    </main>
  );
}

function FindingsPanel({
  analysis,
  comparison,
  newFindingIds,
  onSelectFinding,
  selectedFindingId,
}: {
  analysis: Analysis;
  comparison?: ApiScanComparison;
  newFindingIds: Set<string>;
  onSelectFinding: (id: string) => void;
  selectedFindingId?: string;
}) {
  const openCount = analysis.findings.filter((finding) => finding.status === "Open").length;

  return (
    <section className="rounded-md border border-zinc-200 bg-white p-4 shadow-sm">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-sm font-semibold uppercase tracking-wide text-zinc-500">
            Findings
          </h2>
          <p className="mt-1 text-sm text-zinc-600">
            {analysis.findings.length} total, {openCount} open
          </p>
        </div>
        {comparison?.summary.newlyIntroduced ? (
          <span className="w-fit rounded-full bg-red-50 px-2.5 py-1 text-xs font-semibold text-red-800">
            {comparison.summary.newlyIntroduced} new since last scan
          </span>
        ) : null}
      </div>

      <div className="mt-4 grid max-h-72 gap-2 overflow-y-auto pr-1 lg:grid-cols-2">
        {analysis.findings.map((finding) => (
          <FindingListItem
            finding={finding}
            isNew={newFindingIds.has(finding.id)}
            key={finding.id}
            onSelectFinding={onSelectFinding}
            selected={finding.id === selectedFindingId}
          />
        ))}
      </div>
    </section>
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
    <section className="overflow-hidden rounded-md border border-zinc-200 bg-white shadow-sm">
      <div className="bg-zinc-950 px-4 py-4 text-white">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-teal-200">
            Scan report
          </p>
          <h2 className="mt-1 text-xl font-semibold">Summary and launch checklist</h2>
          <p className="mt-2 text-sm leading-5 text-zinc-300">
            Report snapshot, launch readiness, and export options for this scan.
          </p>
        </div>
      </div>

      {ready && report && (
        <div className="border-b border-zinc-200 bg-zinc-50 px-4 py-3">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-sm font-semibold text-zinc-900">Export report</p>
              <p className="mt-0.5 text-xs text-zinc-500">
                Download this report summary for review.
              </p>
            </div>
            <div className="flex shrink-0 flex-wrap gap-1.5">
              <button
                className="rounded-md bg-zinc-900 px-2.5 py-1.5 text-xs font-semibold text-white hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-60"
                disabled={downloadStatus === "loading"}
                onClick={() => handleDownload("summary-pdf")}
                type="button"
              >
                PDF
              </button>
              <button
                className="rounded-md border border-zinc-300 bg-white px-2.5 py-1.5 text-xs font-semibold text-zinc-700 hover:border-teal-500 disabled:cursor-not-allowed disabled:opacity-60"
                disabled={downloadStatus === "loading"}
                onClick={() => handleDownload("summary-html")}
                type="button"
              >
                HTML
              </button>
              <button
                className="rounded-md border border-zinc-300 bg-white px-2.5 py-1.5 text-xs font-semibold text-zinc-700 hover:border-teal-500 disabled:cursor-not-allowed disabled:opacity-60"
                disabled={downloadStatus === "loading"}
                onClick={() => handleDownload("full-pdf")}
                type="button"
              >
                Full
              </button>
            </div>
          </div>
        </div>
      )}

      {!ready && (
        <p className="m-4 rounded-md bg-zinc-50 p-3 text-sm text-zinc-600">
          Reports are available after this scan completes.
        </p>
      )}

      {ready && reportStatus === "loading" && (
        <p className="m-4 rounded-md bg-zinc-50 p-3 text-sm text-zinc-600">
          Preparing report preview...
        </p>
      )}

      {ready && reportStatus === "error" && (
        <p className="m-4 rounded-md bg-red-50 p-3 text-sm text-red-800">
          {reportError}
        </p>
      )}

      {ready && report && (
        <div className="space-y-4 p-4">
          <div className="rounded-md border border-zinc-200 bg-zinc-50 p-3">
            <p className="text-xs font-semibold uppercase tracking-wide text-zinc-500">
              Launch readiness
            </p>
            <div className="mt-2 flex items-end justify-between gap-3">
              <p className="text-2xl font-semibold leading-none text-zinc-950">
                {report.safeToLaunch.status}
              </p>
              <span className="rounded-full bg-white px-2.5 py-1 text-xs font-semibold text-zinc-700 ring-1 ring-zinc-200">
                {report.safeToLaunch.blockingIssues} blocking
              </span>
            </div>
            <p className="mt-2 text-sm text-zinc-600">
              {report.safeToLaunch.blockingIssues} checklist item
              {report.safeToLaunch.blockingIssues === 1 ? "" : "s"} need attention.
            </p>
          </div>

          <div>
            <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-zinc-500">
              Checklist
            </p>
            <div className="space-y-2">
              {report.safeToLaunch.items.map((item) => (
                <div className="rounded-md border border-zinc-200 bg-white p-3 text-sm" key={item.label}>
                  <div className="flex items-start gap-3">
                    <span className={`mt-0.5 h-2.5 w-2.5 shrink-0 rounded-full ${checklistDotClass(item.result)}`} />
                    <div className="min-w-0 flex-1">
                      <div className="flex items-start justify-between gap-2">
                        <span className="font-medium text-zinc-900">{item.label}</span>
                        <span className={`shrink-0 rounded-full px-2 py-0.5 text-[11px] font-semibold ${checklistResultClass(item.result)}`}>
                          {item.result}
                        </span>
                      </div>
                      <span className="mt-1 block text-xs leading-5 text-zinc-500">{item.reason}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {report.executiveSummary.recommendedNextSteps.length > 0 && (
            <div className="border-t border-zinc-200 pt-4">
              <p className="text-xs font-semibold uppercase tracking-wide text-zinc-500">
                Next steps
              </p>
              <ol className="mt-2 space-y-2">
                {report.executiveSummary.recommendedNextSteps.slice(0, 3).map((step) => (
                  <li className="rounded-md bg-zinc-50 p-2 text-sm" key={`${step.order}-${step.title}`}>
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
            <p className="rounded-md bg-red-50 p-3 text-sm text-red-800">
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

function checklistDotClass(result: string) {
  if (result === "Pass") {
    return "bg-emerald-500";
  }
  if (result === "Needs attention" || result === "Incomplete") {
    return "bg-red-500";
  }
  return "bg-zinc-400";
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
    <section className="rounded-md border border-zinc-200 bg-white px-4 py-3">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h2 className="text-sm font-semibold uppercase tracking-wide text-zinc-500">
            Since last scan
          </h2>
          {comparison.summary.newlyIntroduced > 0 && (
            <p className="mt-0.5 text-xs text-zinc-500">
              New findings are flagged in the list so you can triage them first.
            </p>
          )}
        </div>
        <div className="flex flex-wrap gap-2">
        <MiniChangeMetric label="Fixed" value={comparison.summary.fixed} tone="good" />
        <MiniChangeMetric label="Still open" value={comparison.summary.stillPresent} tone="warn" />
        <MiniChangeMetric label="New" value={comparison.summary.newlyIntroduced} tone="bad" />
        </div>
      </div>
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
    <div className={`flex items-center gap-1.5 rounded-full px-2.5 py-1 ${toneClass}`}>
      <p className="text-sm font-semibold leading-none">{value}</p>
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
    <div className="overflow-hidden rounded-md border border-zinc-200 bg-white shadow-sm">
      <div className="border-b border-zinc-200 bg-gradient-to-br from-zinc-950 to-zinc-800 p-5 text-white">
        <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
          <button
            className="rounded-md border border-white/20 bg-white/10 px-3 py-2 text-sm font-semibold text-white hover:bg-white/15"
            onClick={() => navigate("/analysis")}
            type="button"
          >
            Back to analyses
          </button>
          <button
            className="rounded-md bg-teal-500 px-3 py-2 text-sm font-semibold text-zinc-950 hover:bg-teal-400 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={inProgress || rescanLoading}
            onClick={onRescan}
            type="button"
          >
            {rescanLoading ? "Starting..." : "Rerun scan"}
          </button>
        </div>
        <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <span className="rounded-full bg-white/10 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-teal-100">
                {analysis.status}
              </span>
              <span className="text-sm text-zinc-300">{analysis.createdAt}</span>
            </div>
            <h2 className="mt-3 text-3xl font-semibold tracking-tight">{analysis.siteName}</h2>
            <p className="mt-2 break-all text-zinc-300">{analysis.url}</p>
          </div>
          <SeveritySummary counts={counts} variant="dark" />
        </div>
      </div>
      <div className="flex flex-wrap gap-2 p-4">
        {analysis.selectedScans.map((scan) => (
          <span className="rounded-full bg-zinc-100 px-3 py-1 text-xs font-semibold text-zinc-700" key={scan}>
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
  const { provider, setProvider } = useLlmProvider();
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
      const result = await generateFixPrompt(finding, builder, provider);
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
    <aside className="flex flex-col rounded-md border border-teal-200 bg-teal-50/60 p-5 shadow-sm">
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-teal-800">
            AI repair
          </p>
          <h2 className="mt-1 text-xl font-semibold">Fix prompt</h2>
        </div>
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

          <div className="space-y-1">
            <span className="text-xs font-semibold text-zinc-600">AI provider</span>
            <div
              aria-label="AI provider"
              className="flex items-center gap-0.5 rounded-md border border-zinc-300 p-0.5"
              role="group"
            >
              {LLM_PROVIDERS.map((p) => (
                <button
                  aria-pressed={provider === p.value}
                  className={`flex-1 rounded px-2.5 py-1.5 text-xs font-semibold transition-colors ${
                    provider === p.value
                      ? "bg-teal-700 text-white"
                      : "text-zinc-600 hover:text-zinc-900"
                  }`}
                  key={p.value}
                  onClick={() => setProvider(p.value)}
                  title={`Use ${p.label} for AI generation`}
                  type="button"
                >
                  {p.label}
                </button>
              ))}
            </div>
          </div>

          <button
            className="w-full rounded-md bg-zinc-950 px-4 py-3 font-semibold text-white hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-60"
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
