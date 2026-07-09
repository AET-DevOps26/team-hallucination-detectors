import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { HomePage } from "./HomePage";

const baseProps = {
  onScan: vi.fn().mockResolvedValue(undefined),
  hasSession: false,
  navigate: vi.fn(),
};

describe("HomePage", () => {
  it("renders the hero with a scan button", () => {
    render(<HomePage {...baseProps} />);
    expect(screen.getByText(/left exposed/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /scan my site/i })).toBeInTheDocument();
  });

  it("shows a validation error and does not scan for an invalid URL", async () => {
    const onScan = vi.fn().mockResolvedValue(undefined);
    render(<HomePage {...baseProps} onScan={onScan} />);
    fireEvent.change(screen.getByLabelText(/website url/i), {
      target: { value: "not-a-url" },
    });
    fireEvent.click(screen.getByRole("button", { name: /scan my site/i }));
    await waitFor(() =>
      expect(screen.getByText("Enter a valid URL.")).toBeInTheDocument(),
    );
    expect(onScan).not.toHaveBeenCalled();
  });

  it("calls onScan with the normalized URL on a valid submit", async () => {
    const onScan = vi.fn().mockResolvedValue(undefined);
    render(<HomePage {...baseProps} onScan={onScan} />);
    fireEvent.change(screen.getByLabelText(/website url/i), {
      target: { value: "https://example.com/" },
    });
    fireEvent.click(screen.getByRole("button", { name: /scan my site/i }));
    await waitFor(() => expect(onScan).toHaveBeenCalledWith("https://example.com"));
  });

  it("expands inline advanced options and scans with the chosen scope", async () => {
    const onScan = vi.fn().mockResolvedValue(undefined);
    render(<HomePage {...baseProps} onScan={onScan} />);

    fireEvent.click(screen.getByRole("button", { name: /advanced options/i }));
    expect(screen.getByText(/checks to run/i)).toBeInTheDocument();
    fireEvent.click(screen.getByLabelText(/include subdomains/i));

    fireEvent.change(screen.getByLabelText(/website url/i), {
      target: { value: "https://example.com/" },
    });
    fireEvent.click(screen.getByRole("button", { name: /scan my site/i }));

    await waitFor(() =>
      expect(onScan).toHaveBeenCalledWith(
        "https://example.com",
        expect.objectContaining({ includeSubdomains: true }),
      ),
    );
  });
});
