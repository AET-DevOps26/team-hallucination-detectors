import { ScanOption, Severity } from "../types/domain";

export const severityOrder: Severity[] = [
  "Critical",
  "High",
  "Medium",
  "Low",
  "Info",
];

export const scanLabels: Record<ScanOption, string> = {
  crawl: "Page, form, and endpoint discovery",
  https: "HTTPS and mixed-content checks",
  headers: "Security header checks",
  adminPaths: "Public admin and login paths",
  secrets: "Client bundle secrets",
  sensitiveFiles: "Sensitive files and backups",
};

export const scanDescriptions: Record<ScanOption, string> = {
  crawl: "Discovers linked pages, forms, and API endpoints so other checks have full coverage of your site.",
  https: "Verifies your site forces HTTPS and flags mixed-content warnings that could expose user data.",
  headers: "Checks for missing security headers like CSP, HSTS, and X-Frame-Options that protect against common attacks.",
  adminPaths: "Looks for publicly reachable admin panels, login pages, and management interfaces that should be restricted.",
  secrets: "Scans your client-side JavaScript bundles for accidentally exposed API keys, tokens, and credentials.",
  sensitiveFiles: "Probes for backup files, config dumps, and sensitive paths like .env or wp-config.php left publicly accessible.",
};

export const severityStyles: Record<Severity, string> = {
  Critical: "border-red-700 bg-red-50 text-red-800",
  High: "border-orange-600 bg-orange-50 text-orange-800",
  Medium: "border-amber-500 bg-amber-50 text-amber-800",
  Low: "border-sky-500 bg-sky-50 text-sky-800",
  Info: "border-zinc-400 bg-zinc-50 text-zinc-700",
};
