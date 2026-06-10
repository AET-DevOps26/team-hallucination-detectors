import { scanLabels, severityOrder } from "../constants/scans";
import { Finding, ScanOption } from "../types/domain";

export function getSeverityCounts(findings: Finding[]) {
  return severityOrder.map((severity) => ({
    severity,
    count: findings.filter(
      (finding) => finding.severity === severity && finding.status !== "Ignored",
    ).length,
  }));
}

export function sortOpenFindings(findings: Finding[]) {
  return [...findings]
    .filter((finding) => finding.status === "Open")
    .sort(
      (a, b) =>
        severityOrder.indexOf(a.severity) - severityOrder.indexOf(b.severity),
    );
}

export function buildPendingFindings(url: string, scans: ScanOption[]): Finding[] {
  return scans.slice(0, 3).map((scan, index) => ({
    id: `F-new-${Date.now()}-${index}`,
    title:
      scan === "headers"
        ? "Security header review queued"
        : scan === "secrets"
          ? "Client bundle secret scan queued"
          : `${scanLabels[scan]} queued`,
    severity: index === 0 ? "Info" : "Low",
    status: "Open",
    affected: url,
    summary: "This check has been queued for the new analysis.",
    impact:
      "The result will become actionable once the backend scanner reports concrete evidence.",
    check: scanLabels[scan],
  }));
}
