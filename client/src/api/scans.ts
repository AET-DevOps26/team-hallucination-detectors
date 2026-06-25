import { apiClient } from "./client";
import type { components } from "./schema";

/**
 * Typed access to the websites/scans/findings contract. All types come from the
 * generated schema (api/openapi.yaml is the single source of truth) — never
 * hand-declare request or response shapes here.
 */
export type ApiWebsite = components["schemas"]["Website"];
export type ApiScan = components["schemas"]["Scan"];
export type ApiFinding = components["schemas"]["Finding"];
export type ApiScanRequest = components["schemas"]["ScanRequest"];
export type ApiScanComparison = components["schemas"]["ScanComparison"];

export async function listWebsites(): Promise<ApiWebsite[]> {
  const { data } = await apiClient.get<ApiWebsite[]>("/api/v1/websites");
  return data;
}

export async function createWebsite(url: string): Promise<ApiWebsite> {
  const { data } = await apiClient.post<ApiWebsite>("/api/v1/websites", { url });
  return data;
}

export async function listScans(websiteId: number): Promise<ApiScan[]> {
  const { data } = await apiClient.get<ApiScan[]>(`/api/v1/websites/${websiteId}/scans`);
  return data;
}

/** 202: the scan starts Pending; poll getScan until Completed or Failed. */
export async function triggerScan(
  websiteId: number,
  request: ApiScanRequest,
): Promise<ApiScan> {
  const { data } = await apiClient.post<ApiScan>(
    `/api/v1/websites/${websiteId}/scans`,
    request,
  );
  return data;
}

export async function getScan(scanId: number): Promise<ApiScan> {
  const { data } = await apiClient.get<ApiScan>(`/api/v1/scans/${scanId}`);
  return data;
}

export async function listFindings(scanId: number): Promise<ApiFinding[]> {
  const { data } = await apiClient.get<ApiFinding[]>(`/api/v1/scans/${scanId}/findings`);
  return data;
}

export async function rescanScan(scanId: number): Promise<ApiScan> {
  const { data } = await apiClient.post<ApiScan>(`/api/v1/scans/${scanId}/rescan`);
  return data;
}

export async function getScanComparison(scanId: number): Promise<ApiScanComparison> {
  const { data } = await apiClient.get<ApiScanComparison>(`/api/v1/scans/${scanId}/comparison`);
  return data;
}
