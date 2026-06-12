import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { ApiScan, ApiWebsite } from "../api/scans";
import { useAnalysisState } from "./useAnalysisState";

vi.mock("../api/scans");
const api = vi.mocked(await import("../api/scans"));

/**
 * The core user workflow against the async contract: trigger a scan (202,
 * Pending), poll the status endpoint until Completed, then show the findings.
 */
describe("useAnalysisState", () => {
  const website: ApiWebsite = {
    id: 7,
    url: "https://shop.example.org",
    name: "shop.example.org",
    createdAt: "2026-06-10T12:00:00Z",
  };
  const pendingScan: ApiScan = {
    id: 42,
    websiteId: 7,
    status: "Pending",
    createdAt: "2026-06-10T12:01:00Z",
  };
  const completedScan: ApiScan = { ...pendingScan, status: "Completed", findingCount: 1 };

  const navigate = vi.fn();

  function render(route: string) {
    return renderHook(() =>
      useAnalysisState({ navigate, route, enabled: true, pollIntervalMs: 10 }),
    );
  }

  beforeEach(() => {
    vi.clearAllMocks();
    api.listWebsites.mockResolvedValue([website]);
    api.listScans.mockResolvedValue([]);
    api.listFindings.mockResolvedValue([]);
  });

  it("loads websites and scan history on start", async () => {
    api.listScans.mockResolvedValue([completedScan]);

    const { result } = render("/analysis");

    await waitFor(() => expect(result.current.sites).toHaveLength(1));
    expect(result.current.sites[0].url).toBe("https://shop.example.org");
    expect(result.current.analyses).toHaveLength(1);
    expect(result.current.analyses[0].status).toBe("Completed");
  });

  it("triggers a scan, navigates to it, and polls until findings arrive", async () => {
    api.createWebsite.mockResolvedValue(website);
    api.triggerScan.mockResolvedValue(pendingScan);
    api.getScan.mockResolvedValue(completedScan);
    api.listFindings.mockResolvedValue([
      {
        id: 1,
        scanId: 42,
        check: "headers",
        title: "Missing Content-Security-Policy header",
        severity: "Medium",
        affected: "https://shop.example.org/",
        explanation: "Without a CSP, injected scripts run unrestricted.",
        suggestedFix: "Add a Content-Security-Policy header.",
        status: "Open",
      },
    ]);

    const { result, rerender } = render("/analysis/new");

    await act(async () => {
      await result.current.createAnalysis({
        url: "https://shop.example.org",
        selectedScans: ["https", "headers"],
        crawlDepth: 0,
        includeSubdomains: false,
      });
    });

    expect(api.triggerScan).toHaveBeenCalledWith(7, {
      checks: ["https", "headers"],
      crawlDepth: 0,
      includeSubdomains: false,
    });
    expect(navigate).toHaveBeenCalledWith("/analysis/42");

    // Simulate the navigation the hook requested; polling starts on the detail
    // route. The fresh hook instance loads the Pending scan from history first.
    rerender();
    api.listScans.mockResolvedValue([pendingScan]);
    const detail = renderHook(() =>
      useAnalysisState({
        navigate,
        route: "/analysis/42",
        enabled: true,
        pollIntervalMs: 10,
      }),
    );

    await waitFor(() =>
      expect(detail.result.current.currentAnalysis?.status).toBe("Completed"),
    );
    await waitFor(() =>
      expect(detail.result.current.currentAnalysis?.findings).toHaveLength(1),
    );
    expect(detail.result.current.currentAnalysis?.findings[0].title).toContain(
      "Content-Security-Policy",
    );
  });

  it("reuses the existing website when the URL is already registered", async () => {
    const { ApiError } = await import("../api/client");
    api.createWebsite.mockRejectedValue(
      new ApiError("You already registered this URL.", "WEBSITE_ALREADY_REGISTERED", 409),
    );
    api.triggerScan.mockResolvedValue(pendingScan);

    const { result } = render("/analysis/new");

    await act(async () => {
      await result.current.createAnalysis({
        url: "https://shop.example.org",
        selectedScans: ["https"],
        crawlDepth: 0,
        includeSubdomains: false,
      });
    });

    expect(api.triggerScan).toHaveBeenCalledWith(7, expect.anything());
  });

  it("surfaces the backend's 409 when a scan is already running", async () => {
    const { ApiError } = await import("../api/client");
    api.createWebsite.mockResolvedValue(website);
    api.triggerScan.mockRejectedValue(
      new ApiError(
        "A scan for this website is already pending or running.",
        "SCAN_IN_PROGRESS",
        409,
      ),
    );

    const { result } = render("/analysis/new");

    await expect(
      result.current.createAnalysis({
        url: "https://shop.example.org",
        selectedScans: ["https"],
        crawlDepth: 0,
        includeSubdomains: false,
      }),
    ).rejects.toThrow("already pending or running");
  });
});
