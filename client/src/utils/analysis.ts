import { severityOrder } from "../constants/scans";
import { Finding } from "../types/domain";

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
