import { ApiFinding, ApiScan, ApiWebsite } from "../api/scans";
import { scanLabels } from "../constants/scans";
import { Analysis, Finding, NewAnalysisInput, ScanOption, Site } from "../types/domain";

/**
 * Maps contract types (generated from api/openapi.yaml) onto the UI's view types.
 * Status, severity, and check values are identical strings in both worlds by
 * contract design, so mapping is mostly identity plus joins and formatting.
 */

export function toSite(website: ApiWebsite): Site {
  return {
    id: String(website.id),
    name: website.name,
    url: website.url,
  };
}

export function toFinding(finding: ApiFinding): Finding {
  const check = finding.check as ScanOption;
  return {
    id: String(finding.id),
    title: finding.title,
    severity: finding.severity,
    status: finding.status,
    affected: finding.affected,
    summary: finding.explanation,
    impact: finding.suggestedFix,
    check,
    checkLabel: scanLabels[check] ?? finding.check,
  };
}

/**
 * Joins a scan with its website and (lazily loaded) findings. Scan configuration
 * (selected checks, depth) is not part of the scan read model yet, so it is only
 * known for scans created in this session and defaults to empty otherwise.
 */
export function toAnalysis(
  scan: ApiScan,
  website: ApiWebsite | undefined,
  findings: Finding[],
  config: NewAnalysisInput | undefined,
): Analysis {
  return {
    id: String(scan.id),
    siteId: String(scan.websiteId),
    siteName: website?.name ?? "Unknown site",
    url: website?.url ?? "",
    createdAt: formatTimestamp(scan.createdAt),
    status: scan.status,
    selectedScans: config?.selectedScans ?? [],
    crawlDepth: config?.crawlDepth ?? 0,
    includeSubdomains: config?.includeSubdomains ?? false,
    findings,
  };
}

function formatTimestamp(isoTimestamp: string): string {
  return new Date(isoTimestamp).toLocaleString(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  });
}
