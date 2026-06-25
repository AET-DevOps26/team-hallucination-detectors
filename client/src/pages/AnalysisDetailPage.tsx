import { useEffect, useState } from "react";
import { generateFixPrompt } from "../api/genai";
import { downloadReport, getReportData, ReportData } from "../api/reports";
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
  const [report, setReport] = useState<ReportData>();
  const [reportStatus, setReportStatus] = useState<"idle" | "loading" | "error">("idle");
  const [reportError, setReportError] = useState("");
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
      <aside className="space-y-5">
        <GenerateFixPrompt finding={selectedFinding} />
        <ReportPanel
          analysis={analysis}
          report={report}
          reportError={reportError}
          reportStatus={reportStatus}
        />
      </aside>
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
    <div className="rounded-md border border-zinc-300 bg-white p-5">
      <h2 className="text-xl font-semibold">Reports</h2>
      <p className="mt-1 text-sm text-zinc-600">
        Export an executive summary or a full scan report for launch review.
      </p>

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
        <div className="mt-4 space-y-4">
          <div className="rounded-md border border-zinc-200 p-3">
            <p className="text-xs font-semibold uppercase text-zinc-500">Safe to launch</p>
            <p className="mt-1 text-lg font-semibold text-zinc-900">{report.safeToLaunch.status}</p>
            <p className="text-sm text-zinc-600">
              {report.safeToLaunch.blockingIssues} checklist item
              {report.safeToLaunch.blockingIssues === 1 ? "" : "s"} need attention.
            </p>
          </div>

          <div>
            <p className="text-sm font-semibold text-zinc-800">Checklist</p>
            <ul className="mt-2 space-y-2">
              {report.safeToLaunch.items.map((item) => (
                <li className="rounded-md border border-zinc-200 p-2 text-sm" key={item.label}>
                  <div className="flex items-start justify-between gap-2">
                    <span className="font-medium text-zinc-800">{item.label}</span>
                    <span className={`shrink-0 rounded px-2 py-0.5 text-xs font-semibold ${checklistResultClass(item.result)}`}>
                      {item.result}
                    </span>
                  </div>
                  <span>
                    <span className="block text-xs text-zinc-500">{item.reason}</span>
                  </span>
                </li>
              ))}
            </ul>
          </div>

          <div>
            <p className="text-sm font-semibold text-zinc-800">Next steps</p>
            {report.executiveSummary.recommendedNextSteps.length === 0 ? (
              <p className="mt-2 text-sm text-zinc-500">No open findings require action.</p>
            ) : (
              <ol className="mt-2 space-y-2">
                {report.executiveSummary.recommendedNextSteps.slice(0, 3).map((step) => (
                  <li className="text-sm" key={`${step.order}-${step.title}`}>
                    <span className="font-medium text-zinc-800">
                      {step.order}. {step.title}
                    </span>
                    <span className="block text-xs text-zinc-500">
                      {step.severity} · {step.effort.level} · {step.effort.estimate}
                    </span>
                  </li>
                ))}
              </ol>
            )}
          </div>

          <div className="grid gap-2">
            <button
              className="rounded-md bg-teal-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-teal-800 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={downloadStatus === "loading"}
              onClick={() => handleDownload("summary-pdf")}
              type="button"
            >
              Summary PDF
            </button>
            <div className="grid grid-cols-2 gap-2">
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
          </div>

          {downloadStatus === "error" && (
            <p className="rounded-md bg-red-50 p-3 text-sm text-red-800">
              {downloadError}
            </p>
          )}
        </div>
      )}
    </div>
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

/**
 * VibeShield's core GenAI surface: turns the selected finding into a single,
 * ready-to-paste prompt for the user's AI builder (Lovable, Cursor, v0, Bolt,
 * Replit). The user never reads or writes code — the same kind of AI that built
 * the site repairs it.
 */
function GenerateFixPrompt({ finding }: { finding?: Finding }) {
  const [prompt, setPrompt] = useState("");
  const [status, setStatus] = useState<"idle" | "loading" | "error">("idle");
  const [error, setError] = useState("");
  const [copied, setCopied] = useState(false);

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
      const result = await generateFixPrompt(finding);
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
    <aside className="rounded-md border border-zinc-300 bg-white p-5">
      <h2 className="text-xl font-semibold">Generate fix prompt</h2>
      <p className="mt-1 text-sm text-zinc-600">
        Create a ready-to-paste prompt for your AI builder (Lovable, Cursor, v0,
        Bolt, Replit) — let the AI that built your site repair it.
      </p>

      {!finding && (
        <p className="mt-4 rounded-md bg-zinc-50 p-3 text-sm text-zinc-600">
          Select a finding to generate a fix prompt.
        </p>
      )}

      {finding && (
        <div className="mt-4 space-y-4">
          <div className="rounded-md border border-zinc-200 p-3">
            <span
              className={`rounded border px-2 py-1 text-xs font-semibold ${severityStyles[finding.severity]}`}
            >
              {finding.severity}
            </span>
            <p className="mt-2 text-sm font-semibold">{finding.title}</p>
            <p className="mt-1 truncate text-xs text-zinc-500">
              {finding.affected}
            </p>
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
            <div className="space-y-2">
              <textarea
                className="min-h-48 w-full rounded-md border border-zinc-300 px-3 py-2 font-mono text-xs text-zinc-800 outline-none focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
                readOnly
                value={prompt}
              />
              <button
                className="w-full rounded-md border border-zinc-300 px-4 py-2 text-sm font-semibold text-zinc-700 hover:border-teal-500"
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
