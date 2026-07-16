import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AnalysisListPage } from "./AnalysisListPage";
import type { Analysis } from "../types/domain";
import { getScanComparison } from "../api/scans";
import type { ApiScanComparison } from "../api/scans";

vi.mock("../api/scans", () => ({
  getScanComparison: vi.fn(),
}));

const mockGetScanComparison = vi.mocked(getScanComparison);

// Rows fetch a comparison for every completed scan; default to "no previous
// scan" so tests that don't care about the delta line stay quiet.
function comparison(overrides: Partial<ApiScanComparison> = {}): ApiScanComparison {
  return {
    scanId: 42,
    comparable: false,
    message: "",
    summary: { fixed: 0, stillPresent: 0, newlyIntroduced: 0 },
    findings: [],
    actionPlan: [],
    ...overrides,
  };
}

beforeEach(() => {
  mockGetScanComparison.mockReset();
  mockGetScanComparison.mockResolvedValue(comparison());
});

const navigate = vi.fn();

const analysis: Analysis = {
  id: "42",
  siteId: "7",
  siteName: "shop.example.org",
  url: "https://shop.example.org",
  status: "Completed",
  createdAt: "2026-06-10T12:01:00Z",
  selectedScans: ["headers"],
  crawlDepth: 0,
  includeSubdomains: false,
  findings: [
    {
      id: "1",
      check: "headers",
      checkLabel: "Security Headers",
      title: "Missing CSP header",
      severity: "Medium",
      affected: "https://shop.example.org/",
      summary: "No CSP.",
      impact: "Injected scripts run unrestricted.",
      status: "Open",
    },
  ],
};

describe("AnalysisListPage", () => {
  it("shows empty state when there are no analyses", () => {
    render(<AnalysisListPage analyses={[]} navigate={navigate} />);
    expect(screen.getByText(/no analyses yet/i)).toBeInTheDocument();
    expect(screen.getByText(/scan your first site/i)).toBeInTheDocument();
  });

  it("navigates to new analysis from empty state button", () => {
    const nav = vi.fn();
    render(<AnalysisListPage analyses={[]} navigate={nav} />);
    fireEvent.click(screen.getByText(/scan your first site/i));
    expect(nav).toHaveBeenCalledWith("/analysis/new");
  });

  it("navigates to new analysis from header button", () => {
    const nav = vi.fn();
    render(<AnalysisListPage analyses={[analysis]} navigate={nav} />);
    fireEvent.click(screen.getByText(/create new analysis/i));
    expect(nav).toHaveBeenCalledWith("/analysis/new");
  });

  it("renders an analysis card with site name", () => {
    render(<AnalysisListPage analyses={[analysis]} navigate={navigate} />);
    expect(screen.getByText("shop.example.org")).toBeInTheDocument();
  });

  it("navigates to analysis detail from a scan row", () => {
    const nav = vi.fn();
    render(<AnalysisListPage analyses={[analysis]} navigate={nav} />);
    // Groups are expanded by default, so the scan row is visible.
    fireEvent.click(screen.getByText(/1 open/i).closest("button")!);
    expect(nav).toHaveBeenCalledWith("/analysis/42");
  });

  it("bundles scans for the same site under one group", () => {
    const olderScan: Analysis = {
      ...analysis,
      id: "43",
      createdAt: "2026-06-01T09:00:00Z",
      findings: [],
    };
    render(<AnalysisListPage analyses={[analysis, olderScan]} navigate={navigate} />);
    // One site header, two scans.
    expect(screen.getAllByText("shop.example.org")).toHaveLength(1);
    expect(screen.getByText(/2 scans/i)).toBeInTheDocument();
  });

  it("renders a group per site", () => {
    const second: Analysis = { ...analysis, id: "99", siteId: "8", siteName: "other.example.org", url: "https://other.example.org", findings: [] };
    render(<AnalysisListPage analyses={[analysis, second]} navigate={navigate} />);
    expect(screen.getByText("shop.example.org")).toBeInTheDocument();
    expect(screen.getByText("other.example.org")).toBeInTheDocument();
  });

  it("collapses and expands a site group", () => {
    render(<AnalysisListPage analyses={[analysis]} navigate={navigate} />);
    const header = screen.getByText("shop.example.org").closest("button")!;
    // Expanded by default: scan row visible.
    expect(screen.getByText(/1 open/i)).toBeInTheDocument();
    fireEvent.click(header);
    expect(screen.queryByText(/1 open/i)).not.toBeInTheDocument();
  });

  it("shows the fixed / newly introduced delta for a completed scan", async () => {
    mockGetScanComparison.mockResolvedValue(
      comparison({
        comparable: true,
        findings: [
          {
            changeStatus: "Fixed",
            severity: "Medium",
            check: "headers",
            title: "Missing CSP header",
            affected: "https://shop.example.org/",
            suggestedFix: "Add a CSP.",
            effort: { level: "Low", estimate: "1-2 hours" },
          },
          {
            changeStatus: "Fixed",
            severity: "Medium",
            check: "headers",
            title: "Missing HSTS header",
            affected: "https://shop.example.org/",
            suggestedFix: "Add HSTS.",
            effort: { level: "Low", estimate: "1-2 hours" },
          },
          {
            changeStatus: "Newly introduced",
            severity: "Low",
            check: "cookies",
            title: "Cookie without SameSite",
            affected: "https://shop.example.org/",
            suggestedFix: "Set SameSite.",
            effort: { level: "Low", estimate: "1-2 hours" },
          },
        ],
      }),
    );

    render(<AnalysisListPage analyses={[analysis]} navigate={navigate} />);

    expect(await screen.findByText(/Fixed 2 Medium/)).toBeInTheDocument();
    expect(screen.getByText(/New 1 Low/)).toBeInTheDocument();
    expect(mockGetScanComparison).toHaveBeenCalledWith(42);
  });

  it("labels the first scan of a site as a baseline", async () => {
    mockGetScanComparison.mockResolvedValue(comparison({ comparable: false }));
    render(<AnalysisListPage analyses={[analysis]} navigate={navigate} />);
    await waitFor(() =>
      expect(screen.getByText(/baseline scan/i)).toBeInTheDocument(),
    );
  });
});