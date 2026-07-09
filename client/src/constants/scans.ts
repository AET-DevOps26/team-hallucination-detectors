import { ScanOption, Severity } from "../types/domain";

export const severityOrder: Severity[] = [
  "Critical",
  "High",
  "Medium",
  "Low",
  "Info",
];

/** Sensible default checks for the one-click hero scan and the advanced form. */
export const defaultScanSelection: ScanOption[] = [
  "https",
  "headers",
  "sensitiveFiles",
];

export const scanLabels: Record<ScanOption, string> = {
  crawl: "Page, form, and endpoint discovery",
  https: "HTTPS and mixed-content checks",
  headers: "Security header checks",
  adminPaths: "Public admin and login paths",
  secrets: "Client bundle secrets",
  sensitiveFiles: "Sensitive files and backups",
  cors: "Cross-origin resource sharing policy",
};

export const scanCategoryStyles: Record<ScanOption, string> = {
  crawl: "border-cyan-300 bg-cyan-50 text-cyan-800 dark:border-cyan-500/40 dark:bg-cyan-500/10 dark:text-cyan-300",
  https: "border-emerald-300 bg-emerald-50 text-emerald-800 dark:border-emerald-500/40 dark:bg-emerald-500/10 dark:text-emerald-300",
  headers: "border-indigo-300 bg-indigo-50 text-indigo-800 dark:border-indigo-500/40 dark:bg-indigo-500/10 dark:text-indigo-300",
  adminPaths: "border-fuchsia-300 bg-fuchsia-50 text-fuchsia-800 dark:border-fuchsia-500/40 dark:bg-fuchsia-500/10 dark:text-fuchsia-300",
  secrets: "border-rose-300 bg-rose-50 text-rose-800 dark:border-rose-500/40 dark:bg-rose-500/10 dark:text-rose-300",
  sensitiveFiles: "border-amber-300 bg-amber-50 text-amber-800 dark:border-amber-500/40 dark:bg-amber-500/10 dark:text-amber-300",
  cors: "border-sky-300 bg-sky-50 text-sky-800 dark:border-sky-500/40 dark:bg-sky-500/10 dark:text-sky-300",
};

export const scanDescriptions: Record<ScanOption, string> = {
  crawl: "Discovers linked pages, forms, and API endpoints so other checks have full coverage of your site.",
  https: "Verifies your site forces HTTPS and flags mixed-content warnings that could expose user data.",
  headers: "Checks for missing security headers like CSP, HSTS, and X-Frame-Options that protect against common attacks.",
  adminPaths: "Looks for publicly reachable admin panels, login pages, and management interfaces that should be restricted.",
  secrets: "Scans your client-side JavaScript bundles for accidentally exposed API keys, tokens, and credentials.",
  sensitiveFiles: "Probes for backup files, config dumps, and sensitive paths like .env or wp-config.php left publicly accessible.",
  cors: "Checks whether your site's CORS policy blindly reflects any requesting origin instead of using a fixed allow-list.",
};

export const severityStyles: Record<Severity, string> = {
  Critical: "border-red-700 bg-red-50 text-red-800 dark:border-red-500/50 dark:bg-red-500/15 dark:text-red-300",
  High: "border-orange-600 bg-orange-50 text-orange-800 dark:border-orange-500/50 dark:bg-orange-500/15 dark:text-orange-300",
  Medium: "border-amber-500 bg-amber-50 text-amber-800 dark:border-amber-500/50 dark:bg-amber-500/15 dark:text-amber-300",
  Low: "border-sky-500 bg-sky-50 text-sky-800 dark:border-sky-500/50 dark:bg-sky-500/15 dark:text-sky-300",
  Info: "border-zinc-400 bg-zinc-50 text-zinc-700 dark:border-zinc-500/50 dark:bg-zinc-500/15 dark:text-zinc-300",
};
