import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { SeveritySummary } from "./SeveritySummary";

const counts = [
  { severity: "Critical" as const, count: 2 },
  { severity: "High" as const, count: 0 },
];

describe("SeveritySummary", () => {
  it("renders one tile per severity with its count", () => {
    render(<SeveritySummary counts={counts} />);

    expect(screen.getByText("Critical").previousElementSibling).toHaveTextContent("2");
    expect(screen.getByText("High").previousElementSibling).toHaveTextContent("0");
  });

  it("uses severity-specific styling in the default (light) variant", () => {
    render(<SeveritySummary counts={counts} />);

    const tile = screen.getByText("Critical").closest("div");
    expect(tile?.className).toContain("border-red-700");
  });

  it("uses uniform dark-variant styling regardless of severity", () => {
    render(<SeveritySummary counts={counts} variant="dark" />);

    const tile = screen.getByText("Critical").closest("div");
    expect(tile?.className).toContain("bg-white/10");
    expect(tile?.className).not.toContain("border-red-700");
  });
});
