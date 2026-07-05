import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { FindingDetail } from "./FindingDetail";

describe("FindingDetail", () => {
  it("renders the label and value", () => {
    render(<FindingDetail label="What happened" value="No CSP header was found." />);

    expect(screen.getByText("What happened")).toBeInTheDocument();
    expect(screen.getByText("No CSP header was found.")).toBeInTheDocument();
  });
});
