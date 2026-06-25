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

export const scanCategoryStyles: Record<ScanOption, string> = {
  crawl: "border-cyan-300 bg-cyan-50 text-cyan-800",
  https: "border-emerald-300 bg-emerald-50 text-emerald-800",
  headers: "border-indigo-300 bg-indigo-50 text-indigo-800",
  adminPaths: "border-fuchsia-300 bg-fuchsia-50 text-fuchsia-800",
  secrets: "border-rose-300 bg-rose-50 text-rose-800",
  sensitiveFiles: "border-amber-300 bg-amber-50 text-amber-800",
};

export const severityStyles: Record<Severity, string> = {
  Critical: "border-red-700 bg-red-50 text-red-800",
  High: "border-orange-600 bg-orange-50 text-orange-800",
  Medium: "border-amber-500 bg-amber-50 text-amber-800",
  Low: "border-sky-500 bg-sky-50 text-sky-800",
  Info: "border-zinc-400 bg-zinc-50 text-zinc-700",
};
