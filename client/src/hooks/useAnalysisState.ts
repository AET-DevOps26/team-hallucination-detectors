import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ApiError } from "../api/client";
import {
  ApiScan,
  ApiScanRequest,
  ApiWebsite,
  createWebsite,
  getScan,
  listFindings,
  listScans,
  listWebsites,
  rescanScan,
  triggerScan,
} from "../api/scans";
import { Finding, FindingStatus, NewAnalysisInput } from "../types/domain";
import { toAnalysis, toFinding, toSite } from "../utils/apiMapping";

type UseAnalysisStateOptions = {
  navigate: (path: string) => void;
  route: string;
  /** Calls are only made while authenticated; state clears on logout. */
  enabled: boolean;
  /** Poll cadence while a scan is Pending/Running; overridable for tests. */
  pollIntervalMs?: number;
};

/**
 * Websites, scans, and findings backed by the real API (issues #9, #11, #10).
 * The async contract drives the shape: triggering returns 202 with a Pending
 * scan, and this hook polls the status endpoint until the scan reaches a
 * terminal state, then loads its findings.
 */
export function useAnalysisState({
  navigate,
  route,
  enabled,
  pollIntervalMs = 2500,
}: UseAnalysisStateOptions) {
  const [websites, setWebsites] = useState<ApiWebsite[]>([]);
  const [scans, setScans] = useState<ApiScan[]>([]);
  const [findingsByScan, setFindingsByScan] = useState<Record<string, Finding[]>>({});
  const [selectedFindingId, setSelectedFindingId] = useState("");
  const [resolutionReason, setResolutionReason] = useState("");
  // Scan configuration (selected checks, depth) is not part of the scan read
  // model yet, so it is remembered only for scans created in this session.
  const scanConfigs = useRef(new Map<string, NewAnalysisInput>());

  const currentAnalysisId =
    route !== "/analysis/new" ? route.match(/^\/analysis\/([^/]+)$/)?.[1] : undefined;
  const currentScan = scans.find((scan) => String(scan.id) === currentAnalysisId);
  const currentScanStatus = currentScan?.status;

  const refresh = useCallback(async () => {
    const loadedWebsites = await listWebsites();
    const scanHistories = await Promise.all(
      loadedWebsites.map((website) => listScans(website.id)),
    );
    setWebsites(loadedWebsites);
    setScans(scanHistories.flat());
  }, []);

  // Initial load on login; full reset on logout.
  useEffect(() => {
    if (!enabled) {
      setWebsites([]);
      setScans([]);
      setFindingsByScan({});
      return;
    }
    refresh().catch(() => {
      // A failed initial load shows empty lists; navigation retries implicitly.
    });
  }, [enabled, refresh]);

  const loadFindings = useCallback(async (scanId: number) => {
    const findings = await listFindings(scanId);
    setFindingsByScan((current) => ({
      ...current,
      [String(scanId)]: findings.map(toFinding),
    }));
  }, []);

  // Load findings once for any Completed scan being viewed.
  useEffect(() => {
    if (!enabled || !currentScan) return;
    if (currentScan.status === "Completed" && !findingsByScan[String(currentScan.id)]) {
      loadFindings(currentScan.id).catch(() => {
        // Leave findings empty; the next poll or visit retries.
      });
    }
  }, [enabled, currentScan, findingsByScan, loadFindings]);

  // The 202 workflow: poll the status endpoint while the viewed scan is in flight.
  useEffect(() => {
    if (!enabled || !currentScan) return;
    if (currentScanStatus !== "Pending" && currentScanStatus !== "Running") return;

    const scanId = currentScan.id;
    const timer = window.setInterval(() => {
      getScan(scanId)
        .then((updated) => {
          setScans((current) =>
            current.map((scan) => (scan.id === updated.id ? updated : scan)),
          );
          if (updated.status === "Completed") {
            loadFindings(scanId).catch(() => {});
          }
        })
        .catch(() => {
          // Transient poll failures are fine; the next tick retries.
        });
    }, pollIntervalMs);
    return () => window.clearInterval(timer);
  }, [enabled, currentScan, currentScanStatus, loadFindings, pollIntervalMs]);

  /**
   * Registers the website if needed and triggers the scan (202). Rejects with a
   * user-displayable ApiError, e.g. when a scan is already running (409).
   */
  async function createAnalysis(input: NewAnalysisInput): Promise<void> {
    const website = await ensureWebsite(input.url);
    const request: ApiScanRequest = {
      checks: input.selectedScans,
      crawlDepth: input.crawlDepth,
      includeSubdomains: input.includeSubdomains,
    };
    const scan = await triggerScan(website.id, request);

    scanConfigs.current.set(String(scan.id), input);
    setWebsites((current) =>
      current.some((existing) => existing.id === website.id)
        ? current
        : [website, ...current],
    );
    setScans((current) => [scan, ...current.filter((existing) => existing.id !== scan.id)]);
    navigate(`/analysis/${scan.id}`);
  }

  async function rescanAnalysis(analysisId: string): Promise<void> {
    const scan = await rescanScan(Number(analysisId));
    setScans((current) => [scan, ...current.filter((existing) => existing.id !== scan.id)]);
    setFindingsByScan((current) => {
      const next = { ...current };
      delete next[String(scan.id)];
      return next;
    });
    navigate(`/analysis/${scan.id}`);
  }

  async function ensureWebsite(url: string) {
    try {
      return await createWebsite(url);
    } catch (error) {
      if (error instanceof ApiError && error.code === "WEBSITE_ALREADY_REGISTERED") {
        const existing = (await listWebsites()).find((website) => website.url === url);
        if (existing) return existing;
      }
      throw error;
    }
  }

  /**
   * Finding triage is client-side state for now — persistence arrives with the
   * fix-tracking tickets (#22/#14). A reload resets it to Open.
   */
  function updateFinding(analysisId: string, findingId: string, status: FindingStatus) {
    setFindingsByScan((current) => ({
      ...current,
      [analysisId]: (current[analysisId] ?? []).map((finding) =>
        finding.id === findingId
          ? {
              ...finding,
              status,
              reason: status === "Open" ? undefined : resolutionReason || undefined,
            }
          : finding,
      ),
    }));
    setResolutionReason("");
  }

  const websitesById = useMemo(
    () => new Map(websites.map((website) => [website.id, website])),
    [websites],
  );

  const analyses = useMemo(
    () =>
      [...scans]
        .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
        .map((scan) =>
          toAnalysis(
            scan,
            websitesById.get(scan.websiteId),
            findingsByScan[String(scan.id)] ?? [],
            scanConfigs.current.get(String(scan.id)),
          ),
        ),
    [scans, websitesById, findingsByScan],
  );

  const sites = useMemo(() => websites.map(toSite), [websites]);
  const currentAnalysis = analyses.find((analysis) => analysis.id === currentAnalysisId);

  return {
    analyses,
    createAnalysis,
    currentAnalysis,
    currentAnalysisId,
    rescanAnalysis,
    resolutionReason,
    selectedFindingId,
    setResolutionReason,
    setSelectedFindingId,
    sites,
    updateFinding,
  };
}
