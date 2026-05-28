import { severityStyles } from "../../constants/scans";
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
    <div className="rounded-md border border-zinc-300 bg-white p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-mono text-sm text-zinc-500">{finding.id}</p>
          <h3 className="mt-1 text-xl font-semibold">{finding.title}</h3>
        </div>
        <span
          className={`rounded border px-3 py-1.5 text-sm font-semibold ${severityStyles[finding.severity]}`}
        >
          {finding.severity}
        </span>
      </div>

      <div className="mt-5 grid gap-4">
        <FindingDetail label="Status" value={finding.status} />
        <FindingDetail label="Check" value={finding.check} />
        <FindingDetail
          label="Affected URL, file, route, or endpoint"
          value={finding.affected}
        />
        <FindingDetail label="What happened" value={finding.summary} />
        <FindingDetail label="Potential impact" value={finding.impact} />
        {finding.reason && <FindingDetail label="Reason" value={finding.reason} />}
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
