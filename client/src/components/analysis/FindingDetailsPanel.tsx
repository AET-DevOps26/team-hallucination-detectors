import { scanCategoryStyles, severityStyles } from "../../constants/scans";
import { Analysis, Finding, FindingStatus } from "../../types/domain";
import { Badge } from "../ui/Badge";
import { Button } from "../ui/Button";
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
    <div className="rounded-xl border border-line bg-surface p-5 shadow-card">
      <div className="flex flex-wrap items-start justify-between gap-4 border-b border-line pb-5">
        <div>
          <p className="font-mono text-xs font-medium uppercase tracking-wide text-muted">
            {finding.id}
          </p>
          <h3 className="mt-2 text-2xl font-semibold tracking-tight text-fg">
            {finding.title}
          </h3>
        </div>
        <Badge className={severityStyles[finding.severity]} size="md">
          {finding.severity}
        </Badge>
      </div>

      <div className="mt-5 grid gap-4 lg:grid-cols-2">
        <div className="rounded-lg bg-elevated p-3">
          <FindingDetail label="Status" value={finding.status} />
        </div>
        <div className="rounded-lg bg-elevated p-3">
          <p className="text-xs font-semibold uppercase text-muted">Scan category</p>
          <span
            className={`mt-1 inline-flex rounded border px-2 py-1 text-sm font-semibold ${scanCategoryStyles[finding.check]}`}
          >
            {finding.checkLabel}
          </span>
        </div>
        <div className="rounded-lg bg-elevated p-3 lg:col-span-2">
          <FindingDetail
            label="Affected URL, file, route, or endpoint"
            value={finding.affected}
          />
        </div>
        <div className="rounded-lg border border-line p-4 lg:col-span-2">
          <FindingDetail label="What happened" value={finding.summary} />
        </div>
        <div className="rounded-lg border border-line p-4 lg:col-span-2">
          <FindingDetail label="Potential impact" value={finding.impact} />
        </div>
        {finding.reason && (
          <div className="rounded-lg border border-line p-4 lg:col-span-2">
            <FindingDetail label="Reason" value={finding.reason} />
          </div>
        )}
      </div>

      <label className="mt-5 block">
        <span className="text-sm font-medium text-muted">
          Fixed or ignored reason
        </span>
        <textarea
          className="mt-2 min-h-24 w-full rounded-lg border border-line bg-surface px-3 py-2 text-sm text-fg outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
          onChange={(event) => setResolutionReason(event.target.value)}
          placeholder="Short note for the audit trail"
          value={resolutionReason}
        />
      </label>

      <div className="mt-4 flex flex-col gap-3 sm:flex-row">
        <button
          className="rounded-lg bg-emerald-600 px-4 py-2.5 font-semibold text-white transition hover:bg-emerald-700 active:scale-[0.98]"
          onClick={() => onUpdateFinding(analysis.id, finding.id, "Fixed")}
          type="button"
        >
          Mark fixed
        </button>
        <Button onClick={() => onUpdateFinding(analysis.id, finding.id, "Ignored")} variant="secondary">
          Ignore
        </Button>
        <Button onClick={() => onUpdateFinding(analysis.id, finding.id, "Open")} variant="ghost">
          Reopen
        </Button>
      </div>
    </div>
  );
}
