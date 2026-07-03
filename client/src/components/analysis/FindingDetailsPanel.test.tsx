import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { Analysis, Finding } from "../../types/domain";
import { FindingDetailsPanel } from "./FindingDetailsPanel";

const analysis: Analysis = {
  id: "analysis-1",
  siteId: "site-1",
  siteName: "Shop",
  url: "https://shop.example.org",
  createdAt: "2026-06-10T12:00:00Z",
  status: "Completed",
  selectedScans: ["headers"],
  crawlDepth: 0,
  includeSubdomains: false,
  findings: [],
};

const finding: Finding = {
  id: "finding-1",
  title: "Missing Content-Security-Policy header",
  severity: "Medium",
  status: "Open",
  affected: "https://shop.example.org/",
  summary: "No CSP header was found.",
  impact: "Increases the blast radius of injected scripts.",
  check: "headers",
  checkLabel: "Security header checks",
};

describe("FindingDetailsPanel", () => {
  it("renders the finding's core details", () => {
    render(
      <FindingDetailsPanel
        analysis={analysis}
        finding={finding}
        onUpdateFinding={vi.fn()}
        resolutionReason=""
        setResolutionReason={vi.fn()}
      />,
    );

    expect(screen.getByText(finding.title)).toBeInTheDocument();
    expect(screen.getByText(finding.summary)).toBeInTheDocument();
    expect(screen.getByText(finding.impact)).toBeInTheDocument();
    expect(screen.getByText(finding.checkLabel)).toBeInTheDocument();
  });

  it("only renders the Reason section when the finding has one", () => {
    const { rerender } = render(
      <FindingDetailsPanel
        analysis={analysis}
        finding={finding}
        onUpdateFinding={vi.fn()}
        resolutionReason=""
        setResolutionReason={vi.fn()}
      />,
    );
    expect(screen.queryByText("Reason")).not.toBeInTheDocument();

    rerender(
      <FindingDetailsPanel
        analysis={analysis}
        finding={{ ...finding, reason: "False positive on staging." }}
        onUpdateFinding={vi.fn()}
        resolutionReason=""
        setResolutionReason={vi.fn()}
      />,
    );
    expect(screen.getByText("Reason")).toBeInTheDocument();
    expect(screen.getByText("False positive on staging.")).toBeInTheDocument();
  });

  it("calls onUpdateFinding with the analysis id, finding id, and Fixed status", () => {
    const onUpdateFinding = vi.fn();
    render(
      <FindingDetailsPanel
        analysis={analysis}
        finding={finding}
        onUpdateFinding={onUpdateFinding}
        resolutionReason=""
        setResolutionReason={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Mark fixed" }));

    expect(onUpdateFinding).toHaveBeenCalledWith("analysis-1", "finding-1", "Fixed");
  });

  it("calls onUpdateFinding with Ignored and Open for the other actions", () => {
    const onUpdateFinding = vi.fn();
    render(
      <FindingDetailsPanel
        analysis={analysis}
        finding={finding}
        onUpdateFinding={onUpdateFinding}
        resolutionReason=""
        setResolutionReason={vi.fn()}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Ignore" }));
    fireEvent.click(screen.getByRole("button", { name: "Reopen" }));

    expect(onUpdateFinding).toHaveBeenNthCalledWith(1, "analysis-1", "finding-1", "Ignored");
    expect(onUpdateFinding).toHaveBeenNthCalledWith(2, "analysis-1", "finding-1", "Open");
  });

  it("reports textarea edits through setResolutionReason", () => {
    const setResolutionReason = vi.fn();
    render(
      <FindingDetailsPanel
        analysis={analysis}
        finding={finding}
        onUpdateFinding={vi.fn()}
        resolutionReason=""
        setResolutionReason={setResolutionReason}
      />,
    );

    fireEvent.change(screen.getByPlaceholderText("Short note for the audit trail"), {
      target: { value: "Rotated the key." },
    });

    expect(setResolutionReason).toHaveBeenCalledWith("Rotated the key.");
  });
});
