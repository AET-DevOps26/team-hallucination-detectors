import { scanCategoryStyles, severityStyles } from "../../constants/scans";
import { Analysis, Finding, FindingStatus } from "../../types/domain";
import { FindingDetail } from "./FindingDetail";

type FindingDetailsPanelProps = {
  analysis: Analysis;
  finding: Finding;
  onUpdateFinding: (
    analysisId: string,
    findingId: string,
    status: FindingStatus,
  ) => void;
  resolutionReason: string;
  setResolutionReason: (value: string) => void;
};

export function FindingDetailsPanel({
  analysis,
  finding,
  onUpdateFinding,
  resolutionReason,
  setResolutionReason,
}: FindingDetailsPanelProps) {
  return (
    <div className="rounded-md border border-zinc-200 bg-white p-5 shadow-sm">
      <div className="flex flex-wrap items-start justify-between gap-4 border-b border-zinc-200 pb-5">
        <div>
          <p className="font-mono text-xs font-medium uppercase tracking-wide text-zinc-500">
            {finding.id}
          </p>
          <h3 className="mt-2 text-2xl font-semibold tracking-tight text-zinc-950">
            {finding.title}
          </h3>
        </div>
        <span
          className={`rounded-full border px-3 py-1.5 text-sm font-semibold ${severityStyles[finding.severity]}`}
        >
          {finding.severity}
        </span>
      </div>

      <div className="mt-5 grid gap-4 lg:grid-cols-2">
        <div className="rounded-md bg-zinc-50 p-3">
          <FindingDetail label="Status" value={finding.status} />
        </div>
        <div className="rounded-md bg-zinc-50 p-3">
          <p className="text-xs font-semibold uppercase text-zinc-500">Scan category</p>
          <span
            className={`mt-1 inline-flex rounded border px-2 py-1 text-sm font-semibold ${scanCategoryStyles[finding.check]}`}
          >
            {finding.checkLabel}
          </span>
        </div>
        <div className="rounded-md bg-zinc-50 p-3 lg:col-span-2">
          <FindingDetail
            label="Affected URL, file, route, or endpoint"
            value={finding.affected}
          />
        </div>
        <div className="rounded-md border border-zinc-200 p-4 lg:col-span-2">
          <FindingDetail label="What happened" value={finding.summary} />
        </div>
        <div className="rounded-md border border-zinc-200 p-4 lg:col-span-2">
          <FindingDetail label="Potential impact" value={finding.impact} />
        </div>
        {finding.reason && (
          <div className="rounded-md border border-zinc-200 p-4 lg:col-span-2">
            <FindingDetail label="Reason" value={finding.reason} />
          </div>
        )}
      </div>

      <label className="mt-5 block">
        <span className="text-sm font-medium text-zinc-700">
          Fixed or ignored reason
        </span>
        <textarea
          className="mt-2 min-h-24 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
          onChange={(event) => setResolutionReason(event.target.value)}
          placeholder="Short note for the audit trail"
          value={resolutionReason}
        />
      </label>

      <div className="mt-4 flex flex-col gap-3 sm:flex-row">
        <button
          className="rounded-md bg-emerald-700 px-4 py-3 font-semibold text-white hover:bg-emerald-800"
          onClick={() => onUpdateFinding(analysis.id, finding.id, "Fixed")}
          type="button"
        >
          Mark fixed
        </button>
        <button
          className="rounded-md bg-zinc-800 px-4 py-3 font-semibold text-white hover:bg-zinc-700"
          onClick={() => onUpdateFinding(analysis.id, finding.id, "Ignored")}
          type="button"
        >
          Ignore
        </button>
        <button
          className="rounded-md border border-zinc-300 px-4 py-3 font-semibold text-zinc-700 hover:border-teal-500"
          onClick={() => onUpdateFinding(analysis.id, finding.id, "Open")}
          type="button"
        >
          Reopen
        </button>
      </div>
    </div>
  );
}
