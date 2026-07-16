import type { ApiScanComparison } from "../api/scans";
import { severityOrder } from "../constants/scans";
import { Severity } from "../types/domain";

export type SeverityCount = { severity: Severity; count: number };

/**
 * What changed in a scan relative to the previous completed scan for its site:
 * - `baseline`  — nothing to compare against (first completed scan, or not done)
 * - `no-change` — a previous scan exists but no findings were fixed or introduced
 * - `changes`   — the fixed / newly introduced findings, bucketed by severity
 */
export type ScanTransition =
  | { kind: "baseline" }
  | { kind: "no-change" }
  | { kind: "changes"; fixed: SeverityCount[]; introduced: SeverityCount[] };

/**
 * Distils a scan comparison into the deltas shown on the analysis list. Only
 * `Fixed` and `Newly introduced` findings matter here — the row calls out what
 * *changed* since the previous scan, not the still-standing backlog, so
 * `Still present` findings are intentionally dropped.
 */
export function summarizeScanTransition(comparison: ApiScanComparison): ScanTransition {
  // Not comparable means there is no earlier completed scan to diff against, so
  // there is no delta to describe: treat this scan as the site's baseline.
  if (!comparison.comparable) {
    return { kind: "baseline" };
  }

  const fixed = countBySeverity(comparison, "Fixed");
  const introduced = countBySeverity(comparison, "Newly introduced");

  if (fixed.length === 0 && introduced.length === 0) {
    return { kind: "no-change" };
  }
  return { kind: "changes", fixed, introduced };
}

function countBySeverity(
  comparison: ApiScanComparison,
  changeStatus: "Fixed" | "Newly introduced",
): SeverityCount[] {
  const counts = new Map<Severity, number>();
  for (const finding of comparison.findings) {
    if (finding.changeStatus === changeStatus) {
      counts.set(finding.severity, (counts.get(finding.severity) ?? 0) + 1);
    }
  }
  // Order worst-first so "2 Critical, 1 Low" always reads by descending severity.
  return severityOrder
    .filter((severity) => counts.has(severity))
    .map((severity) => ({ severity, count: counts.get(severity) as number }));
}
