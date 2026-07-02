import { useCallback, useEffect, useState } from "react";
import {
  AiBuilder,
  FixPromptMode,
  FixPromptPackage,
  generateFixPrompt,
} from "../api/genai";
import { downloadReport, getReportData, ReportData } from "../api/reports";
import { getScanComparison, ApiScanComparison } from "../api/scans";
import { FindingDetailsPanel } from "../components/analysis/FindingDetailsPanel";
import { FindingListItem } from "../components/analysis/FindingListItem";
import { SeveritySummary } from "../components/analysis/SeveritySummary";
import { scanCategoryStyles, scanLabels, severityStyles } from "../constants/scans";
import { Analysis, Finding, FindingStatus, ScanOption } from "../types/domain";
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
  const [promptRequest, setPromptRequest] = useState<{ findingId: string; nonce: number }>();
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

  function handleGeneratePrompt(findingId: string) {
    onSelectFinding(findingId);
    setPromptRequest({ findingId, nonce: Date.now() });
  }

  const selectedFindingChangeStatus = selectedFinding
    ? changeStatusForFinding(comparison, selectedFinding)
    : undefined;

  return (
    <main className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_340px]">
      <section className="space-y-5">
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
        <ScanComparisonPanel
          comparison={comparison}
          comparisonError={comparisonError}
          comparisonStatus={comparisonStatus}
          ready={analysis.status === "Completed"}
        />
        <div className="grid gap-4 lg:grid-cols-[minmax(0,0.9fr)_minmax(360px,1.1fr)]">
          <div className="space-y-3">
            {analysis.findings.map((finding) => (
              <FindingListItem
                finding={finding}
                key={finding.id}
                onGeneratePrompt={handleGeneratePrompt}
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
        <GenerateFixPrompt
          changeStatus={selectedFindingChangeStatus}
          finding={selectedFinding}
          promptRequest={promptRequest}
        />
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

function ScanComparisonPanel({
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
  return (
    <section className="rounded-md border border-zinc-300 bg-white p-5">
      <div className="flex flex-col gap-2 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <h2 className="text-xl font-semibold">Changes since previous scan</h2>
          <p className="mt-1 text-sm text-zinc-600">
            Compare the current scan with the last completed scan for this site.
          </p>
        </div>
        {comparison?.comparable && (
          <div className="grid grid-cols-3 gap-2 text-center">
            <MiniChangeMetric label="Fixed" value={comparison.summary.fixed} tone="good" />
            <MiniChangeMetric label="Still" value={comparison.summary.stillPresent} tone="warn" />
            <MiniChangeMetric label="New" value={comparison.summary.newlyIntroduced} tone="bad" />
          </div>
        )}
      </div>

      {!ready && (
        <p className="mt-4 rounded-md bg-zinc-50 p-3 text-sm text-zinc-600">
          Comparison is available after this scan completes.
        </p>
      )}
      {ready && comparisonStatus === "loading" && (
        <p className="mt-4 rounded-md bg-zinc-50 p-3 text-sm text-zinc-600">
          Loading comparison…
        </p>
      )}
      {ready && comparisonStatus === "error" && (
        <p className="mt-4 rounded-md bg-red-50 p-3 text-sm text-red-800">
          {comparisonError}
        </p>
      )}
      {ready && comparison && !comparison.comparable && (
        <p className="mt-4 rounded-md bg-zinc-50 p-3 text-sm text-zinc-600">
          {comparison.message}
        </p>
      )}
      {ready && comparison?.comparable && (
        <div className="mt-4 grid gap-4 xl:grid-cols-[minmax(0,1fr)_320px]">
          <div>
            <p className="text-sm font-semibold text-zinc-800">Finding changes</p>
            <div className="mt-2 space-y-2">
              {comparison.findings.length === 0 ? (
                <p className="rounded-md bg-zinc-50 p-3 text-sm text-zinc-600">
                  No finding changes were detected.
                </p>
              ) : (
                comparison.findings.map((finding) => (
                  <ComparisonFindingRow finding={finding} key={`${finding.changeStatus}-${finding.title}-${finding.affected}`} />
                ))
              )}
            </div>
          </div>
          <div>
            <p className="text-sm font-semibold text-zinc-800">Action plan</p>
            <div className="mt-2 space-y-2">
              {comparison.actionPlan.length === 0 ? (
                <p className="rounded-md bg-emerald-50 p-3 text-sm text-emerald-800">
                  No open findings require action in this scan.
                </p>
              ) : (
                comparison.actionPlan.slice(0, 5).map((finding) => (
                  <div className="rounded-md border border-zinc-200 p-3 text-sm" key={`${finding.suggestedFixOrder}-${finding.title}`}>
                    <span
                      className={`inline-flex rounded border px-2 py-1 text-xs font-semibold ${scanCategoryClass(finding.check)}`}
                    >
                      {scanCategoryLabel(finding.check)}
                    </span>
                    <p className="mt-2 font-semibold text-zinc-900">
                      {finding.suggestedFixOrder}. {finding.title}
                    </p>
                    <p className="mt-1 text-xs text-zinc-500">
                      {finding.severity} · {finding.effort.level} · {finding.effort.estimate}
                    </p>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
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
    <div className={`rounded-md px-3 py-2 ${toneClass}`}>
      <p className="text-lg font-semibold">{value}</p>
      <p className="text-xs font-medium">{label}</p>
    </div>
  );
}

function ComparisonFindingRow({ finding }: { finding: ApiScanComparison["findings"][number] }) {
  return (
    <div className="rounded-md border border-zinc-200 p-3 text-sm">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <span
            className={`inline-flex rounded border px-2 py-1 text-xs font-semibold ${scanCategoryClass(finding.check)}`}
          >
            {scanCategoryLabel(finding.check)}
          </span>
          <p className="mt-2 font-semibold text-zinc-900">{finding.title}</p>
          <p className="mt-1 text-xs text-zinc-500">{finding.affected}</p>
        </div>
        <span className={`shrink-0 rounded px-2 py-1 text-xs font-semibold ${changeStatusClass(finding.changeStatus)}`}>
          {finding.changeStatus}
        </span>
      </div>
      <p className="mt-2 text-xs text-zinc-500">
        {finding.severity} · {finding.effort.level} · {finding.effort.estimate}
      </p>
    </div>
  );
}

function scanCategoryLabel(check: string) {
  return scanLabels[check as ScanOption] ?? check;
}

function scanCategoryClass(check: string) {
  return scanCategoryStyles[check as ScanOption] ?? "border-zinc-300 bg-zinc-50 text-zinc-700";
}

function changeStatusForFinding(
  comparison: ApiScanComparison | undefined,
  finding: Finding,
) {
  if (!comparison?.comparable) return undefined;
  const comparisonFinding =
    comparison.actionPlan.find((item) => String(item.findingId) === finding.id) ??
    comparison.findings.find((item) => String(item.findingId) === finding.id) ??
    comparison.findings.find(
      (item) =>
        item.title === finding.title &&
        item.affected === finding.affected &&
        item.check === finding.check,
    );
  return comparisonFinding?.changeStatus;
}

function changeStatusClass(status: string) {
  if (status === "Fixed") {
    return "bg-emerald-50 text-emerald-800";
  }
  if (status === "Newly introduced") {
    return "bg-red-50 text-red-800";
  }
  return "bg-amber-50 text-amber-800";
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
 * the site repairs it.
 */
const aiBuilders: AiBuilder[] = ["Generic", "Cursor", "Lovable", "v0", "Bolt", "Replit"];
const promptModes: FixPromptMode[] = [
  "Quick fix",
  "Detailed implementation",
  "Explain and fix",
  "Verification only",
];

function GenerateFixPrompt({
  changeStatus,
  finding,
  promptRequest,
}: {
  changeStatus?: string;
  finding?: Finding;
  promptRequest?: { findingId: string; nonce: number };
}) {
  const [builder, setBuilder] = useState<AiBuilder>("Generic");
  const [mode, setMode] = useState<FixPromptMode>("Quick fix");
  const [promptPackage, setPromptPackage] = useState<FixPromptPackage>();
  const [status, setStatus] = useState<"idle" | "loading" | "error">("idle");
  const [error, setError] = useState("");
  const [copied, setCopied] = useState<"prompt" | "verification" | "package" | undefined>();

  // A different finding is a different prompt — clear any stale output.
  useEffect(() => {
    setPromptPackage(undefined);
    setStatus("idle");
    setError("");
    setCopied(undefined);
  }, [finding?.id]);

  const handleGenerate = useCallback(async () => {
    if (!finding) return;
    setStatus("loading");
    setError("");
    setCopied(undefined);
    try {
      const result = await generateFixPrompt({
        ...finding,
        builder,
        changeStatus,
        mode,
      });
      setPromptPackage(result);
      setStatus("idle");
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Could not generate a fix prompt. Please try again.",
      );
      setStatus("error");
    }
  }, [builder, changeStatus, finding, mode]);

  useEffect(() => {
    if (promptRequest?.findingId === finding?.id) {
      void handleGenerate();
    }
  }, [finding?.id, handleGenerate, promptRequest?.findingId, promptRequest?.nonce]);

  async function handleCopy(kind: "prompt" | "verification" | "package", text: string) {
    await navigator.clipboard.writeText(text);
    setCopied(kind);
    window.setTimeout(() => setCopied(undefined), 2000);
  }

  function fullPackageText(packageData: FixPromptPackage) {
    return [
      `AI builder: ${packageData.builder}`,
      `Mode: ${packageData.mode}`,
      `Issue type: ${packageData.issueType}`,
      `Expected secure behavior: ${packageData.expectedResult}`,
      `Likely targets: ${packageData.likelyTargets}`,
      `Risk note: ${packageData.riskNote}`,
      `Rollback note: ${packageData.rollbackNote}`,
      "",
      "Fix prompt:",
      packageData.prompt,
      "",
      "Verification prompt:",
      packageData.verificationPrompt,
    ].join("\n");
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
            {changeStatus && (
              <p className="mt-2 text-xs font-semibold text-zinc-600">
                Rescan status: {changeStatus}
              </p>
            )}
          </div>

          <div className="grid gap-3">
            <label className="block">
              <span className="text-sm font-medium text-zinc-700">AI builder</span>
              <select
                className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
                onChange={(event) => setBuilder(event.target.value as AiBuilder)}
                value={builder}
              >
                {aiBuilders.map((option) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </select>
            </label>

            <label className="block">
              <span className="text-sm font-medium text-zinc-700">Prompt style</span>
              <select
                className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
                onChange={(event) => setMode(event.target.value as FixPromptMode)}
                value={mode}
              >
                {promptModes.map((option) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <button
            className="w-full rounded-md bg-teal-700 px-4 py-3 font-semibold text-white hover:bg-teal-800 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={status === "loading"}
            onClick={handleGenerate}
            type="button"
          >
            {status === "loading"
              ? "Generating…"
              : promptPackage
                ? "Regenerate fix prompt"
                : "Generate fix prompt"}
          </button>

          {status === "error" && (
            <p className="rounded-md bg-red-50 p-3 text-sm text-red-800">
              {error}
            </p>
          )}

          {promptPackage && (
            <div className="space-y-4">
              <div className="rounded-md border border-zinc-200 p-3 text-sm">
                <p className="font-semibold text-zinc-900">{promptPackage.issueType}</p>
                <p className="mt-2 text-xs font-semibold uppercase text-zinc-500">
                  Expected secure behavior
                </p>
                <p className="mt-1 text-sm text-zinc-700">{promptPackage.expectedResult}</p>
                <p className="mt-2 text-xs font-semibold uppercase text-zinc-500">
                  Likely targets
                </p>
                <p className="mt-1 text-sm text-zinc-700">{promptPackage.likelyTargets}</p>
                <p className="mt-2 text-xs font-semibold uppercase text-zinc-500">
                  Safety note
                </p>
                <p className="mt-1 text-sm text-zinc-700">{promptPackage.riskNote}</p>
                <p className="mt-2 text-xs font-semibold uppercase text-zinc-500">
                  Rollback note
                </p>
                <p className="mt-1 text-sm text-zinc-700">{promptPackage.rollbackNote}</p>
              </div>

              <textarea
                className="min-h-48 w-full rounded-md border border-zinc-300 px-3 py-2 font-mono text-xs text-zinc-800 outline-none focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
                readOnly
                value={promptPackage.prompt}
              />

              <div className="grid gap-2">
                <button
                  className="rounded-md border border-zinc-300 px-4 py-2 text-sm font-semibold text-zinc-700 hover:border-teal-500"
                  onClick={() => handleCopy("prompt", promptPackage.prompt)}
                  type="button"
                >
                  {copied === "prompt" ? "Copied" : "Copy fix prompt"}
                </button>
                <button
                  className="rounded-md border border-zinc-300 px-4 py-2 text-sm font-semibold text-zinc-700 hover:border-teal-500"
                  onClick={() => handleCopy("verification", promptPackage.verificationPrompt)}
                  type="button"
                >
                  {copied === "verification" ? "Copied" : "Copy verification prompt"}
                </button>
                <button
                  className="rounded-md border border-zinc-300 px-4 py-2 text-sm font-semibold text-zinc-700 hover:border-teal-500"
                  onClick={() => handleCopy("package", fullPackageText(promptPackage))}
                  type="button"
                >
                  {copied === "package" ? "Copied" : "Copy full fix package"}
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </aside>
  );
}
