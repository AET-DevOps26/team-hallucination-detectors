import { ReactNode, useEffect, useMemo, useState } from "react";
import { generateFixPrompt } from "../api/genai";
import { LLM_PROVIDERS, useLlmProvider } from "../hooks/useLlmProvider";
import { downloadReport } from "../api/reports";
import { getScanComparison, ApiScanComparison } from "../api/scans";
import { FindingListItem } from "../components/analysis/FindingListItem";
import { ScanProgress } from "../components/analysis/ScanProgress";
import { SeveritySummary } from "../components/analysis/SeveritySummary";
import { SitePreview } from "../components/analysis/SitePreview";
import { Alert } from "../components/ui/Alert";
import { Badge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import { useToast } from "../components/ui/useToast";
import { severityStyles } from "../constants/scans";
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
  selectedFindingId: string;
};

export function AnalysisDetailPage({
  analysis,
  navigate,
  onSelectFinding,
  onRescan,
  onUpdateFinding,
  selectedFindingId,
}: AnalysisDetailPageProps) {
  const { toast } = useToast();
  const [comparison, setComparison] = useState<ApiScanComparison>();
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

  // The comparison is used only to flag findings that are new since the last scan
  // (a "New" badge in the list); its result feeds newFindingIds below.
  useEffect(() => {
    if (!reportAnalysisId || reportAnalysisStatus !== "Completed") {
      setComparison(undefined);
      return;
    }
    let cancelled = false;
    getScanComparison(Number(reportAnalysisId))
      .then((data) => {
        if (!cancelled) {
          setComparison(data);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setComparison(undefined);
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
      <div className="flex flex-wrap items-center justify-between gap-3">
        <button
          className="rounded-lg border border-line bg-surface px-3 py-2 text-sm font-semibold text-fg transition hover:bg-elevated active:scale-95"
          onClick={() => navigate("/analysis")}
          type="button"
        >
          ← Back to analyses
        </button>
        <div className="flex items-center gap-2">
          {analysis.status === "Completed" && <DownloadMenu analysisId={analysisId} />}
          <button
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-3 py-2 text-sm font-semibold text-primary-fg transition hover:bg-primary-hover active:scale-95 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={inProgress || rescanStatus === "loading"}
            onClick={handleRescan}
            type="button"
          >
            {rescanStatus === "loading" ? "Starting..." : "Rerun scan"}
          </button>
        </div>
      </div>

      <AnalysisHeader analysis={analysis} counts={severityCounts} />

      {inProgress ? (
        <ScanProgress
          host={hostnameOf(analysis.url)}
          scans={analysis.selectedScans}
          status={analysis.status as "Pending" | "Running"}
        />
      ) : (
        <>
          <SitePreview url={analysis.url} />

          <FindingsWorkbench
            analysis={analysis}
            comparison={comparison}
            newFindingIds={newFindingIds}
            onSelectFinding={onSelectFinding}
            onUpdateFinding={onUpdateFinding}
            selectedFinding={selectedFinding}
          />
        </>
      )}
    </main>
  );
}

/**
 * Master-detail workbench: the findings list on the left, the selected finding's
 * explanation, status controls, and AI fix-prompt generator on the right. Merging
 * these into one box keeps "which issue" and "what to do about it" side by side so
 * the user never has to scroll between them.
 */
function FindingsWorkbench({
  analysis,
  comparison,
  newFindingIds,
  onSelectFinding,
  onUpdateFinding,
  selectedFinding,
}: {
  analysis: Analysis;
  comparison?: ApiScanComparison;
  newFindingIds: Set<string>;
  onSelectFinding: (id: string) => void;
  onUpdateFinding: (
    analysisId: string,
    findingId: string,
    status: FindingStatus,
  ) => void;
  selectedFinding?: Finding;
}) {
  const openCount = analysis.findings.filter((finding) => finding.status === "Open").length;

  if (analysis.findings.length === 0) {
    return (
      <section className="rounded-xl border border-line bg-surface p-5 shadow-card">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-muted">Findings</h2>
        <div className="mt-4 rounded-lg border border-dashed border-line bg-elevated px-4 py-10 text-center text-sm text-muted">
          No findings for this scan. Nice and clean. ✨
        </div>
      </section>
    );
  }

  return (
    <section className="overflow-hidden rounded-xl border border-line bg-surface shadow-card">
      <div className="grid lg:grid-cols-[260px_minmax(0,1fr)]">
        {/* Findings list */}
        <div className="border-b border-line lg:border-b-0 lg:border-r">
          <div className="flex items-center justify-between gap-2 border-b border-line px-4 py-3">
            <div>
              <h2 className="text-sm font-semibold uppercase tracking-wide text-muted">Findings</h2>
              <p className="mt-0.5 text-xs text-muted">
                {analysis.findings.length} total · {openCount} open
              </p>
            </div>
            {comparison?.summary.newlyIntroduced ? (
              <Badge tone="danger">{comparison.summary.newlyIntroduced} new</Badge>
            ) : null}
          </div>
          <div className="max-h-[28rem] space-y-1.5 overflow-y-auto p-2 lg:max-h-[36rem]">
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
        </div>

        {/* Selected finding detail */}
        {selectedFinding ? (
          <FindingDetailPane
            analysis={analysis}
            finding={selectedFinding}
            onUpdateFinding={onUpdateFinding}
          />
        ) : (
          <div className="flex items-center justify-center p-10 text-sm text-muted">
            Select a finding to see the details.
          </div>
        )}
      </div>
    </section>
  );
}

// Status options for the finding's segmented control. Each option carries the
// filled styling applied when it is the finding's current status, so the same
// control both communicates state and offers the one-click switch to another.
const STATUS_OPTIONS: {
  value: FindingStatus;
  label: string;
  activeClass: string;
  icon: ReactNode;
}[] = [
  {
    value: "Open",
    label: "Open",
    activeClass: "bg-red-600 text-white shadow-sm",
    icon: (
      <svg className="h-3.5 w-3.5" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
        <path d="M12 9v4m0 4h.01M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    ),
  },
  {
    value: "Fixed",
    label: "Fixed",
    activeClass: "bg-emerald-600 text-white shadow-sm",
    icon: (
      <svg className="h-3.5 w-3.5" fill="none" stroke="currentColor" strokeWidth={2.4} viewBox="0 0 24 24">
        <path d="m20 6-11 11-5-5" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    ),
  },
  {
    value: "Ignored",
    label: "Ignored",
    activeClass: "bg-zinc-600 text-white shadow-sm",
    icon: (
      <svg className="h-3.5 w-3.5" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
        <path d="M9.9 4.24A9.1 9.1 0 0 1 12 4c7 0 10 8 10 8a18.5 18.5 0 0 1-2.16 3.19M6.6 6.6A18.5 18.5 0 0 0 2 12s3 8 10 8a9.1 9.1 0 0 0 5.4-1.6M14.1 14.1a3 3 0 0 1-4.2-4.2M2 2l20 20" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    ),
  },
];

function FindingDetailPane({
  analysis,
  finding,
  onUpdateFinding,
}: {
  analysis: Analysis;
  finding: Finding;
  onUpdateFinding: (
    analysisId: string,
    findingId: string,
    status: FindingStatus,
  ) => void;
}) {
  return (
    <div className="flex flex-col p-5">
      {/* Title + status controls */}
      <div className="flex flex-wrap items-start justify-between gap-4 border-b border-line pb-4">
        <div className="min-w-0">
          <span
            className={`inline-flex rounded border px-1.5 py-0.5 text-[11px] font-semibold ${severityStyles[finding.severity]}`}
          >
            {finding.severity}
          </span>
          <h3 className="mt-2 text-xl font-semibold tracking-tight text-fg">
            {finding.title}
          </h3>
        </div>
        <div className="flex flex-col items-end gap-1.5">
          <span className="text-[11px] font-medium uppercase tracking-wide text-muted">
            Status
          </span>
          <div
            className="inline-flex items-center gap-0.5 rounded-lg border border-line bg-elevated p-0.5"
            role="group"
            aria-label="Finding status"
          >
            {STATUS_OPTIONS.map((option) => {
              const active = finding.status === option.value;
              return (
                <button
                  key={option.value}
                  aria-pressed={active}
                  className={`inline-flex items-center gap-1.5 rounded-md px-2.5 py-1.5 text-xs font-semibold transition active:scale-95 ${
                    active
                      ? `${option.activeClass} cursor-default`
                      : "text-muted hover:bg-surface hover:text-fg"
                  }`}
                  onClick={
                    active
                      ? undefined
                      : () => onUpdateFinding(analysis.id, finding.id, option.value)
                  }
                  type="button"
                >
                  {option.icon}
                  {option.label}
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Explanation */}
      <div className="mt-5 space-y-5">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-muted">What happened</p>
          <p className="mt-1.5 text-sm leading-6 text-fg">{finding.summary}</p>
        </div>
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-muted">Potential impact</p>
          <p className="mt-1.5 text-sm leading-6 text-fg">{finding.impact}</p>
        </div>
        {finding.reason && (
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-muted">Reason</p>
            <p className="mt-1.5 text-sm leading-6 text-fg">{finding.reason}</p>
          </div>
        )}
      </div>

      <FixPromptFooter finding={finding} />
    </div>
  );
}

// Report formats offered by the download menu. The API also exposes a "full-pdf"
// variant, but the user-facing choice is deliberately the summary in either format.
const DOWNLOAD_OPTIONS: { kind: "summary-pdf" | "summary-html"; label: string; ext: string }[] = [
  { kind: "summary-pdf", label: "PDF", ext: ".pdf" },
  { kind: "summary-html", label: "HTML", ext: ".html" },
];

/**
 * Split-style download control sitting next to "Rerun scan": one button that
 * opens a small menu to pick the report format (PDF or HTML), then streams the
 * file down via downloadReport.
 */
function DownloadMenu({ analysisId }: { analysisId: string }) {
  const { toast } = useToast();
  const [open, setOpen] = useState(false);
  const [downloading, setDownloading] = useState(false);

  async function handleDownload(kind: "summary-pdf" | "summary-html") {
    setOpen(false);
    setDownloading(true);
    try {
      await downloadReport(analysisId, kind);
      toast("Report download started.", "success");
    } catch (error) {
      toast(error instanceof Error ? error.message : "Could not download the report.", "error");
    } finally {
      setDownloading(false);
    }
  }

  return (
    <div className="relative">
      <button
        aria-expanded={open}
        aria-haspopup="menu"
        className="inline-flex items-center gap-2 rounded-lg border border-line bg-surface px-3 py-2 text-sm font-semibold text-fg transition hover:bg-elevated active:scale-95 disabled:cursor-not-allowed disabled:opacity-60"
        disabled={downloading}
        onClick={() => setOpen((value) => !value)}
        type="button"
      >
        {downloading ? "Downloading…" : "Download report"}
        <svg className="h-3.5 w-3.5" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
          <path d="m6 9 6 6 6-6" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>
      {open && (
        <>
          {/* Click-away layer */}
          <button
            aria-hidden="true"
            className="fixed inset-0 z-10 cursor-default"
            onClick={() => setOpen(false)}
            tabIndex={-1}
            type="button"
          />
          <div
            className="absolute right-0 z-20 mt-1 w-44 overflow-hidden rounded-lg border border-line bg-surface py-1 shadow-card"
            role="menu"
          >
            {DOWNLOAD_OPTIONS.map((option) => (
              <button
                className="flex w-full items-center justify-between px-3 py-2 text-sm font-medium text-fg transition hover:bg-elevated"
                key={option.kind}
                onClick={() => handleDownload(option.kind)}
                role="menuitem"
                type="button"
              >
                {option.label}
                <span className="text-xs text-muted">{option.ext}</span>
              </button>
            ))}
          </div>
        </>
      )}
    </div>
  );
}

function AnalysisHeader({
  analysis,
  counts,
}: {
  analysis: Analysis;
  counts: ReturnType<typeof getSeverityCounts>;
}) {
  return (
    <div className="overflow-hidden rounded-xl border border-line shadow-card">
      <div className="bg-gradient-to-br from-slate-900 to-slate-800 p-5 text-white">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div className="min-w-0">
            <span className="text-sm text-slate-300">{analysis.createdAt}</span>
            <h2 className="mt-2 break-all text-3xl font-semibold tracking-tight">{analysis.siteName}</h2>
          </div>
          <SeveritySummary counts={counts} />
        </div>
      </div>
    </div>
  );
}

/**
 * VibeShield's core GenAI surface: turns the selected finding into a single,
 * ready-to-paste prompt for the user's AI builder (Lovable, Cursor, v0, Bolt,
 * Replit). The user never reads or writes code — the same kind of AI that built
 * the site repairs it. Rendered as the footer of the finding detail so the
 * "here's the issue" and "here's the fix" panels never drift apart.
 */
const AI_BUILDERS = ["Generic", "Lovable", "Cursor", "v0", "Bolt", "Replit"] as const;
type AiBuilder = (typeof AI_BUILDERS)[number];

/** One line per builder so the mode switch reads as intentional, not cosmetic. */
const BUILDER_HELP: Record<AiBuilder, string> = {
  Generic: "Plain-language, platform-agnostic prompt",
  Lovable: "Plain-language app editor prompt",
  Cursor: "Codebase-aware implementation prompt",
  v0: "React/Next UI-focused prompt",
  Bolt: "Full-stack project prompt",
  Replit: "Project/file-structure prompt",
};

function FixPromptFooter({ finding }: { finding: Finding }) {
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
  }, [finding.id]);

  async function handleGenerate() {
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
    <div className="mt-6 rounded-xl border border-primary/30 bg-primary/5 p-4">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div className="space-y-3">
          <div className="space-y-2.5">
            <span className="text-xs font-semibold uppercase tracking-wide text-muted">
              AI builder
            </span>
            <div
              aria-label="AI builder"
              className="inline-flex flex-wrap items-center gap-0.5 rounded-lg border border-line bg-surface p-0.5"
              role="group"
            >
              {AI_BUILDERS.map((b) => (
                <button
                  aria-pressed={builder === b}
                  className={`rounded-md px-3 py-1.5 text-xs font-semibold transition ${
                    builder === b
                      ? "bg-primary text-primary-fg"
                      : "text-muted hover:text-fg"
                  }`}
                  key={b}
                  onClick={() => setBuilder(b)}
                  type="button"
                >
                  {b}
                </button>
              ))}
            </div>
            <p className="text-xs text-muted">{BUILDER_HELP[builder]}</p>
          </div>

          <div className="space-y-2.5">
            <span className="text-xs font-semibold uppercase tracking-wide text-muted">
              AI provider
            </span>
            <div
              aria-label="AI provider"
              className="inline-flex items-center gap-0.5 rounded-lg border border-line bg-surface p-0.5"
              role="group"
            >
              {LLM_PROVIDERS.map((p) => (
                <button
                  aria-pressed={provider === p.value}
                  className={`rounded-md px-3 py-1.5 text-xs font-semibold transition ${
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
        </div>

        <Button
          className="shrink-0"
          leftIcon={
            <svg
              className="h-4 w-4"
              fill="none"
              stroke="currentColor"
              strokeWidth={1.7}
              viewBox="0 0 24 24"
            >
              <path
                d="M22 2 11 13M22 2l-7 20-4-9-9-4 20-7Z"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          }
          loading={status === "loading"}
          onClick={handleGenerate}
        >
          {status === "loading"
            ? "Generating…"
            : prompt
              ? "Regenerate fix prompt"
              : "Generate fix prompt"}
        </Button>
      </div>

      {status === "error" && (
        <div className="mt-3">
          <Alert tone="error">{error}</Alert>
        </div>
      )}

      {prompt && (
        <div className="mt-3 space-y-2">
          <textarea
            className="min-h-40 w-full rounded-lg border border-line bg-surface px-3 py-2 font-mono text-xs text-fg outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
            readOnly
            value={prompt}
          />
          <div className="flex justify-end">
            <Button onClick={handleCopy} variant="secondary">
              {copied ? "Copied ✓" : "Copy prompt"}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
