import { useEffect, useState } from "react";
import { generateFixPrompt } from "../api/genai";
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
  const inProgress = analysis.status === "Pending" || analysis.status === "Running";

  return (
    <main className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_340px]">
      <section className="space-y-5">
        {inProgress && (
          <div className="flex items-start gap-3 rounded-md border border-teal-200 bg-teal-50 px-4 py-3">
            <svg className="mt-0.5 h-5 w-5 shrink-0 animate-spin text-teal-600" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" fill="currentColor" />
            </svg>
            <div>
              <p className="text-sm font-semibold text-teal-800">
                {analysis.status === "Pending" ? "Scan queued" : "Scan running"}
              </p>
              <p className="mt-0.5 text-sm text-teal-700">
                {analysis.status === "Pending"
                  ? "Your scan is queued and will start shortly. This page updates automatically."
                  : "VibeShield is checking your site for security issues. Results will appear here when the scan finishes — usually under a minute."}
              </p>
            </div>
          </div>
        )}
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
      <GenerateFixPrompt finding={selectedFinding} />
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
