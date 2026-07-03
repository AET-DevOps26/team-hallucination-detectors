import { render, screen, fireEvent } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { NotFoundPage } from "./NotFoundPage";

describe("NotFoundPage", () => {
  it("renders the 404 message", () => {
    render(<NotFoundPage navigate={vi.fn()} />);
    expect(screen.getByText(/error 404/i)).toBeInTheDocument();
    expect(screen.getByText(/page not found/i)).toBeInTheDocument();
  });

  it("navigates to the dashboard when the button is clicked", () => {
    const nav = vi.fn();
    render(<NotFoundPage navigate={nav} />);
    fireEvent.click(screen.getByText(/back to dashboard/i));
    expect(nav).toHaveBeenCalledWith("/analysis");
  });
});
