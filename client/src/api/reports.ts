import { apiClient } from "./client";
import type { components } from "./schema";

export type ReportData = components["schemas"]["ReportData"];

export async function getReportData(scanId: string): Promise<ReportData> {
  const { data } = await apiClient.get<ReportData>(`/api/v1/scans/${scanId}/report/data`);
  return data;
}

export async function downloadReport(
  scanId: string,
  kind: "summary-html" | "summary-pdf" | "full-pdf",
): Promise<void> {
  const pathByKind = {
    "summary-html": "summary.html",
    "summary-pdf": "summary.pdf",
    "full-pdf": "full.pdf",
  } satisfies Record<typeof kind, string>;
  const { data, headers } = await apiClient.get<Blob>(
    `/api/v1/scans/${scanId}/report/${pathByKind[kind]}`,
    { responseType: "blob" },
  );
  const fallbackName = `vibeshield-scan-${scanId}-${pathByKind[kind]}`;
  const filename = filenameFromDisposition(headers["content-disposition"]) ?? fallbackName;
  const url = URL.createObjectURL(data);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function filenameFromDisposition(disposition?: string): string | undefined {
  const match = disposition?.match(/filename="?([^";]+)"?/i);
  return match?.[1];
}
