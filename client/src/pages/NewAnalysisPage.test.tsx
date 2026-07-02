import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { NewAnalysisPage } from "./NewAnalysisPage";
import type { Site } from "../types/domain";

const navigate = vi.fn();
const createAnalysis = vi.fn().mockResolvedValue(undefined);

const site: Site = { id: "7", name: "shop.example.org", url: "https://shop.example.org" };

const baseProps = {
  createAnalysis,
  navigate,
  sites: [],
};

describe("NewAnalysisPage", () => {
  it("renders the form with Start analysis button", () => {
    render(<NewAnalysisPage {...baseProps} />);
    expect(screen.getByText("Start analysis")).toBeInTheDocument();
    expect(screen.getByText("Cancel")).toBeInTheDocument();
  });

  it("shows validation error for invalid URL", async () => {
    render(<NewAnalysisPage {...baseProps} />);
    const input = screen.getByPlaceholderText("https://example.com");
    fireEvent.change(input, { target: { value: "not-a-url" } });
    fireEvent.submit(input.closest("form")!);
    await waitFor(() =>
      expect(screen.getByText("Enter a valid URL.")).toBeInTheDocument()
    );
    expect(createAnalysis).not.toHaveBeenCalled();
  });

  it("shows validation error for non-http protocol", async () => {
    render(<NewAnalysisPage {...baseProps} />);
    const input = screen.getByPlaceholderText("https://example.com");
    fireEvent.change(input, { target: { value: "ftp://example.com" } });
    fireEvent.submit(input.closest("form")!);
    await waitFor(() =>
      expect(screen.getByText("Enter a valid http or https URL.")).toBeInTheDocument()
    );
  });

  it("shows validation error when no scans selected", async () => {
    render(<NewAnalysisPage {...baseProps} />);
    const input = screen.getByPlaceholderText("https://example.com");
    fireEvent.change(input, { target: { value: "https://example.com" } });

    // Uncheck all default scans
    screen.getAllByRole("checkbox").forEach((cb) => {
      if ((cb as HTMLInputElement).checked) fireEvent.click(cb);
    });

    fireEvent.submit(input.closest("form")!);
    await waitFor(() =>
      expect(screen.getByText("Choose at least one scan.")).toBeInTheDocument()
    );
  });

  it("calls createAnalysis with correct input on valid submit", async () => {
    const create = vi.fn().mockResolvedValue(undefined);
    render(<NewAnalysisPage {...baseProps} createAnalysis={create} />);
    const input = screen.getByPlaceholderText("https://example.com");
    fireEvent.change(input, { target: { value: "https://example.com/" } });
    fireEvent.submit(input.closest("form")!);
    await waitFor(() => expect(create).toHaveBeenCalled());
    const call = create.mock.calls[0][0];
    expect(call.url).toBe("https://example.com"); // trailing slash stripped
    expect(call.selectedScans.length).toBeGreaterThan(0);
  });

  it("surfaces backend error on failed submit", async () => {
    const create = vi.fn().mockRejectedValue(new Error("A scan is already running."));
    render(<NewAnalysisPage {...baseProps} createAnalysis={create} />);
    const input = screen.getByPlaceholderText("https://example.com");
    fireEvent.change(input, { target: { value: "https://example.com" } });
    fireEvent.submit(input.closest("form")!);
    await waitFor(() =>
      expect(screen.getByText("A scan is already running.")).toBeInTheDocument()
    );
  });

  it("navigates back on Cancel click", () => {
    const nav = vi.fn();
    render(<NewAnalysisPage {...baseProps} navigate={nav} />);
    fireEvent.click(screen.getByText("Cancel"));
    expect(nav).toHaveBeenCalledWith("/analysis");
  });

  it("pre-fills URL from first known site", () => {
    render(<NewAnalysisPage {...baseProps} sites={[site]} />);
    const input = screen.getByPlaceholderText("https://example.com") as HTMLInputElement;
    expect(input.value).toBe("https://shop.example.org");
  });

  it("shows selected scope panel reflecting current URL", () => {
    render(<NewAnalysisPage {...baseProps} />);
    const input = screen.getByPlaceholderText("https://example.com");
    fireEvent.change(input, { target: { value: "https://mysite.com" } });
    expect(screen.getByText("https://mysite.com")).toBeInTheDocument();
  });
});