import { useEffect, useMemo, useState } from "react";
import { generateFixPrompt } from "../api/genai";
import { LLM_PROVIDERS, useLlmProvider } from "../hooks/useLlmProvider";
import { downloadReport, getReportData, ReportData } from "../api/reports";
import { getScanComparison, ApiScanComparison } from "../api/scans";
import { FindingDetailsPanel } from "../components/analysis/FindingDetailsPanel";
import { FindingListItem } from "../components/analysis/FindingListItem";
import { ScanProgress } from "../components/analysis/ScanProgress";
import { SeveritySummary } from "../components/analysis/SeveritySummary";
import { SitePreview } from "../components/analysis/SitePreview";
import { Alert } from "../components/ui/Alert";
import { Badge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import { Skeleton } from "../components/ui/Skeleton";
import { useToast } from "../components/ui/useToast";
import { scanLabels, severityStyles } from "../constants/scans";
import { Analysis, Finding, FindingStatus } from "../types/domain";
import { getSeverityCounts } from "../utils/analysis";
import { hostnameOf } from "../utils/url";

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
  const { toast } = useToast();
  const [report, setReport] = useState<ReportData>();
  const [reportStatus, setReportStatus] = useState<"idle" | "loading" | "error">("idle");
  const [reportError, setReportError] = useState("");
  const [comparison, setComparison] = useState<ApiScanComparison>();
  const [comparisonStatus, setComparisonStatus] = useState<"idle" | "loading" | "error">("idle");
  const [comparisonError, setComparisonError] = useState("");
  const [rescanStatus, setRescanStatus] = useState<"idle" | "loading" | "error">("idle");
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
      <main className="rounded-xl border border-line bg-surface p-8 text-center shadow-card">
        <h2 className="text-2xl font-semibold text-fg">Analysis not found</h2>
        <p className="mt-2 text-muted">This analysis may have been removed.</p>
        <Button className="mt-5" onClick={() => navigate("/analysis")}>
          Back to analyses
        </Button>
      </main>
    );
  }

  const severityCounts = getSeverityCounts(analysis.findings);
  const inProgress = analysis.status === "Pending" || analysis.status === "Running";
  const analysisId = analysis.id;

  async function handleRescan() {
    setRescanStatus("loading");
    try {
      await onRescan(analysisId);
      setRescanStatus("idle");
      toast("Rescan started.", "success");
    } catch (error) {
      setRescanStatus("error");
      toast(error instanceof Error ? error.message : "Could not start a rescan.", "error");
    }
  }

  return (
    <main className="animate-fade-in space-y-5">
      <AnalysisHeader
        analysis={analysis}
        counts={severityCounts}
        inProgress={inProgress}
        navigate={navigate}
        onRescan={handleRescan}
        rescanLoading={rescanStatus === "loading"}
      />

      {inProgress ? (
        <section className="grid gap-5 lg:grid-cols-2">
          <SitePreview url={analysis.url} />
          <ScanProgress
            host={hostnameOf(analysis.url)}
            scans={analysis.selectedScans}
            status={analysis.status as "Pending" | "Running"}
          />
        </section>
      ) : (
        <>
          <section className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_360px]">
            <SitePreview url={analysis.url} />
            <div className="space-y-5">
              <SeverityOverview counts={severityCounts} findings={analysis.findings} />
              <ComparisonSummary
                comparison={comparison}
                comparisonError={comparisonError}
                comparisonStatus={comparisonStatus}
                ready={analysis.status === "Completed"}
              />
            </div>
          </section>

          <ReportPanel
            analysis={analysis}
            report={report}
            reportError={reportError}
            reportStatus={reportStatus}
          />

          <FindingsPanel
            analysis={analysis}
            comparison={comparison}
            newFindingIds={newFindingIds}
            onSelectFinding={onSelectFinding}
            selectedFindingId={selectedFinding?.id}
          />

          {selectedFinding && (
            <section className="grid gap-5 2xl:grid-cols-[minmax(0,1fr)_360px]">
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
        </>
      )}
    </main>
  );
}

function SeverityOverview({
  counts,
  findings,
}: {
  counts: ReturnType<typeof getSeverityCounts>;
  findings: Finding[];
}) {
  const openCount = findings.filter((finding) => finding.status === "Open").length;
  return (
    <div className="rounded-xl border border-line bg-surface p-5 shadow-card">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-muted">
          Severity
        </h2>
        <span className="text-sm text-muted">
          {findings.length} total · {openCount} open
        </span>
      </div>
      <div className="mt-4">
        <SeveritySummary counts={counts} />
      </div>
    </div>
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
    <section className="rounded-xl border border-line bg-surface p-5 shadow-card">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-sm font-semibold uppercase tracking-wide text-muted">
            Findings
          </h2>
          <p className="mt-1 text-sm text-muted">
            {analysis.findings.length} total, {openCount} open
          </p>
        </div>
        {comparison?.summary.newlyIntroduced ? (
          <Badge tone="danger">{comparison.summary.newlyIntroduced} new since last scan</Badge>
        ) : null}
      </div>

      {analysis.findings.length === 0 ? (
        <div className="mt-4 rounded-lg border border-dashed border-line bg-elevated px-4 py-10 text-center text-sm text-muted">
          No findings for this scan. Nice and clean. ✨
        </div>
      ) : (
        <div className="mt-4 grid max-h-80 gap-2 overflow-y-auto pr-1 lg:grid-cols-2">
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
      )}
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
  const { toast } = useToast();
  const [downloadStatus, setDownloadStatus] = useState<"idle" | "loading">("idle");
  const ready = analysis.status === "Completed";

  async function handleDownload(kind: "summary-html" | "summary-pdf" | "full-pdf") {
    setDownloadStatus("loading");
    try {
      await downloadReport(analysis.id, kind);
      toast("Report download started.", "success");
    } catch (error) {
      toast(error instanceof Error ? error.message : "Could not download the report.", "error");
    } finally {
      setDownloadStatus("idle");
    }
  }

  return (
    <section className="overflow-hidden rounded-xl border border-line bg-surface shadow-card">
      <div className="flex flex-col gap-3 border-b border-line bg-gradient-to-br from-slate-900 to-slate-800 px-5 py-4 text-white sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-primary">
            Scan report
          </p>
          <h2 className="mt-0.5 text-lg font-semibold">Summary &amp; launch checklist</h2>
        </div>
        {ready && report && (
          <div className="flex shrink-0 flex-wrap gap-1.5">
            <Button loading={downloadStatus === "loading"} onClick={() => handleDownload("summary-pdf")} size="sm">
              PDF
            </Button>
            <button
              className="rounded-lg border border-white/20 bg-white/10 px-2.5 py-1.5 text-xs font-semibold text-white transition hover:bg-white/20 disabled:opacity-60"
              disabled={downloadStatus === "loading"}
              onClick={() => handleDownload("summary-html")}
              type="button"
            >
              HTML
            </button>
            <button
              className="rounded-lg border border-white/20 bg-white/10 px-2.5 py-1.5 text-xs font-semibold text-white transition hover:bg-white/20 disabled:opacity-60"
              disabled={downloadStatus === "loading"}
              onClick={() => handleDownload("full-pdf")}
              type="button"
            >
              Full
            </button>
          </div>
        )}
      </div>

      {!ready && (
        <p className="m-4 rounded-lg bg-elevated p-3 text-sm text-muted">
          Reports are available after this scan completes.
        </p>
      )}

      {ready && reportStatus === "loading" && (
        <div className="space-y-3 p-5">
          <Skeleton className="h-20 w-full" />
          <Skeleton className="h-12 w-full" />
          <Skeleton className="h-12 w-2/3" />
        </div>
      )}

      {ready && reportStatus === "error" && (
        <div className="p-4">
          <Alert tone="error">{reportError}</Alert>
        </div>
      )}

      {ready && report && (
        <div className="grid gap-4 p-5 lg:grid-cols-[280px_minmax(0,1fr)]">
          <div className="rounded-lg border border-line bg-elevated p-4">
            <p className="text-xs font-semibold uppercase tracking-wide text-muted">
              Launch readiness
            </p>
            <div className="mt-2 flex items-end justify-between gap-3">
              <p className="text-2xl font-semibold leading-none text-fg">
                {report.safeToLaunch.status}
              </p>
              <Badge tone={report.safeToLaunch.blockingIssues > 0 ? "danger" : "success"}>
                {report.safeToLaunch.blockingIssues} blocking
              </Badge>
            </div>
            <p className="mt-2 text-sm text-muted">
              {report.safeToLaunch.blockingIssues} checklist item
              {report.safeToLaunch.blockingIssues === 1 ? "" : "s"} need attention.
            </p>

            {report.executiveSummary.recommendedNextSteps.length > 0 && (
              <div className="mt-4 border-t border-line pt-4">
                <p className="text-xs font-semibold uppercase tracking-wide text-muted">
                  Next steps
                </p>
                <ol className="mt-2 space-y-2">
                  {report.executiveSummary.recommendedNextSteps.slice(0, 3).map((step) => (
                    <li className="rounded-lg bg-surface p-2 text-sm" key={`${step.order}-${step.title}`}>
                      <span className="font-medium text-fg">
                        {step.order}. {step.title}
                      </span>
                      <span className="block text-xs text-muted">
                        {step.severity} · {step.effort.level} · {step.effort.estimate}
                      </span>
                    </li>
                  ))}
                </ol>
              </div>
            )}
          </div>

          <div>
            <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted">
              Checklist
            </p>
            <div className="grid gap-2 sm:grid-cols-2">
              {report.safeToLaunch.items.map((item) => (
                <div className="rounded-lg border border-line bg-surface p-3 text-sm" key={item.label}>
                  <div className="flex items-start gap-3">
                    <span className={`mt-0.5 h-2.5 w-2.5 shrink-0 rounded-full ${checklistDotClass(item.result)}`} />
                    <div className="min-w-0 flex-1">
                      <div className="flex items-start justify-between gap-2">
                        <span className="font-medium text-fg">{item.label}</span>
                        <span className={`shrink-0 rounded-full px-2 py-0.5 text-[11px] font-semibold ${checklistResultClass(item.result)}`}>
                          {item.result}
                        </span>
                      </div>
                      <span className="mt-1 block text-xs leading-5 text-muted">{item.reason}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

function checklistResultClass(result: string) {
  if (result === "Pass") {
    return "bg-emerald-50 text-emerald-800 dark:bg-emerald-500/15 dark:text-emerald-300";
  }
  if (result === "Needs attention" || result === "Incomplete") {
    return "bg-red-50 text-red-800 dark:bg-red-500/15 dark:text-red-300";
  }
  return "bg-elevated text-muted";
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
      <div className="rounded-xl border border-line bg-surface p-5 shadow-card">
        <Skeleton className="h-5 w-40" />
        <div className="mt-3 flex gap-2">
          <Skeleton className="h-7 w-20" />
          <Skeleton className="h-7 w-20" />
          <Skeleton className="h-7 w-20" />
        </div>
      </div>
    );
  }
  if (comparisonStatus === "error") {
    return <Alert tone="error">{comparisonError}</Alert>;
  }
  if (!comparison) {
    return null;
  }
  if (!comparison.comparable) {
    return (
      <div className="rounded-xl border border-line bg-surface p-4 text-sm text-muted shadow-card">
        {comparison.message}
      </div>
    );
  }

  return (
    <section className="rounded-xl border border-line bg-surface p-5 shadow-card">
      <h2 className="text-sm font-semibold uppercase tracking-wide text-muted">
        Since last scan
      </h2>
      <div className="mt-3 flex flex-wrap gap-2">
        <MiniChangeMetric label="Fixed" tone="good" value={comparison.summary.fixed} />
        <MiniChangeMetric label="Still open" tone="warn" value={comparison.summary.stillPresent} />
        <MiniChangeMetric label="New" tone="bad" value={comparison.summary.newlyIntroduced} />
      </div>
      {comparison.summary.newlyIntroduced > 0 && (
        <p className="mt-3 text-xs text-muted">
          New findings are flagged in the list so you can triage them first.
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
    good: "bg-emerald-50 text-emerald-800 dark:bg-emerald-500/15 dark:text-emerald-300",
    warn: "bg-amber-50 text-amber-800 dark:bg-amber-500/15 dark:text-amber-300",
    bad: "bg-red-50 text-red-800 dark:bg-red-500/15 dark:text-red-300",
  }[tone];
  return (
    <div className={`flex items-center gap-1.5 rounded-full px-3 py-1 ${toneClass}`}>
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
    <div className="overflow-hidden rounded-xl border border-line shadow-card">
      <div className="bg-gradient-to-br from-slate-900 to-slate-800 p-5 text-white">
        <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
          <button
            className="rounded-lg border border-white/20 bg-white/10 px-3 py-2 text-sm font-semibold text-white transition hover:bg-white/20 active:scale-95"
            onClick={() => navigate("/analysis")}
            type="button"
          >
            ← Back to analyses
          </button>
          <button
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-3 py-2 text-sm font-semibold text-primary-fg transition hover:bg-primary-hover active:scale-95 disabled:cursor-not-allowed disabled:opacity-60"
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
              <span className="rounded-full bg-white/10 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-primary">
                {analysis.status}
              </span>
              <span className="text-sm text-slate-300">{analysis.createdAt}</span>
            </div>
            <h2 className="mt-3 text-3xl font-semibold tracking-tight">{analysis.siteName}</h2>
            <p className="mt-2 break-all text-slate-300">{analysis.url}</p>
          </div>
          <SeveritySummary counts={counts} variant="dark" />
        </div>
      </div>
      <div className="flex flex-wrap gap-2 bg-surface p-4">
        {analysis.selectedScans.map((scan) => (
          <Badge key={scan} tone="neutral">
            {scanLabels[scan]}
          </Badge>
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
  const { toast } = useToast();
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
      const message =
        err instanceof Error
          ? err.message
          : "Could not generate a fix prompt. Please try again.";
      setError(message);
      setStatus("error");
      toast(message, "error");
    }
  }

  async function handleCopy() {
    await navigator.clipboard.writeText(prompt);
    setCopied(true);
    toast("Fix prompt copied to clipboard.", "success");
    window.setTimeout(() => setCopied(false), 2000);
  }

  return (
    <aside className="flex flex-col rounded-xl border border-primary/30 bg-primary/5 p-5 shadow-card">
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-primary">
            AI repair
          </p>
          <h2 className="mt-1 text-xl font-semibold text-fg">Fix prompt</h2>
        </div>
        {finding && (
          <Badge className={severityStyles[finding.severity]}>{finding.severity}</Badge>
        )}
      </div>
      <p className="mt-1 text-sm text-muted">
        {finding
          ? `Ready-to-paste prompt for "${finding.title}" — hand it to your AI builder.`
          : "Select a finding to generate a fix prompt."}
      </p>

      {finding && (
        <div className="mt-4 flex flex-1 flex-col space-y-4">
          <div className="space-y-1">
            <label className="text-xs font-semibold text-muted" htmlFor="builder-select">
              AI builder
            </label>
            <select
              className="w-full rounded-lg border border-line bg-surface px-3 py-2 text-sm text-fg outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
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
            <span className="text-xs font-semibold text-muted">AI provider</span>
            <div
              aria-label="AI provider"
              className="flex items-center gap-0.5 rounded-lg border border-line bg-surface p-0.5"
              role="group"
            >
              {LLM_PROVIDERS.map((p) => (
                <button
                  aria-pressed={provider === p.value}
                  className={`flex-1 rounded-md px-2.5 py-1.5 text-xs font-semibold transition ${
                    provider === p.value
                      ? "bg-primary text-primary-fg"
                      : "text-muted hover:text-fg"
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

          <Button fullWidth loading={status === "loading"} onClick={handleGenerate} variant="secondary">
            {status === "loading"
              ? "Generating…"
              : prompt
                ? "Regenerate fix prompt"
                : "Generate fix prompt"}
          </Button>

          {status === "error" && <Alert tone="error">{error}</Alert>}

          {prompt && (
            <div className="flex flex-1 flex-col space-y-2">
              <textarea
                className="min-h-48 w-full flex-1 rounded-lg border border-line bg-surface px-3 py-2 font-mono text-xs text-fg outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
                readOnly
                value={prompt}
              />
              <Button fullWidth onClick={handleCopy} variant="secondary">
                {copied ? "Copied ✓" : "Copy prompt"}
              </Button>
            </div>
          )}
        </div>
      )}
    </aside>
  );
}
