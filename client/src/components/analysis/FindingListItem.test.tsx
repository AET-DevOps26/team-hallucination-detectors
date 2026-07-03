import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { Finding } from "../../types/domain";
import { FindingListItem } from "./FindingListItem";

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

describe("FindingListItem", () => {
  it("renders the finding's title, category, severity, and status", () => {
    render(
      <FindingListItem finding={finding} onSelectFinding={vi.fn()} selected={false} />,
    );

    expect(screen.getByText(finding.title)).toBeInTheDocument();
    expect(screen.getByText(finding.checkLabel)).toBeInTheDocument();
    expect(screen.getByText(finding.severity)).toBeInTheDocument();
    expect(screen.getByText(finding.status)).toBeInTheDocument();
    expect(screen.getByText(finding.affected)).toBeInTheDocument();
  });

  it("calls onSelectFinding with the finding's id when clicked", () => {
    const onSelectFinding = vi.fn();
    render(
      <FindingListItem finding={finding} onSelectFinding={onSelectFinding} selected={false} />,
    );

    fireEvent.click(screen.getByRole("button"));

    expect(onSelectFinding).toHaveBeenCalledWith("finding-1");
  });

  it("shows a New badge only when isNew is true", () => {
    const { rerender } = render(
      <FindingListItem finding={finding} onSelectFinding={vi.fn()} selected={false} />,
    );
    expect(screen.queryByText("New")).not.toBeInTheDocument();

    rerender(
      <FindingListItem finding={finding} isNew onSelectFinding={vi.fn()} selected={false} />,
    );
    expect(screen.getByText("New")).toBeInTheDocument();
  });

  it("applies selected styling when selected", () => {
    render(
      <FindingListItem finding={finding} onSelectFinding={vi.fn()} selected />,
    );

    expect(screen.getByRole("button").className).toContain("border-teal-600");
  });
});
