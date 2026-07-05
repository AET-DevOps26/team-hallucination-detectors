import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { AuthModal } from "./AuthModal";

const baseProps = {
  authError: "",
  authMessage: "",
  authMode: "login" as const,
  authStatus: "idle" as const,
  email: "",
  onAuthModeChange: vi.fn(),
  onAuthSubmit: vi.fn(),
  password: "",
  setEmail: vi.fn(),
  setPassword: vi.fn(),
  onClose: vi.fn(),
};

describe("AuthModal", () => {
  it("renders nothing when closed", () => {
    const { container } = render(<AuthModal {...baseProps} open={false} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("renders the auth form and pending scan host when open", () => {
    render(<AuthModal {...baseProps} open pendingUrl="https://shop.example.org/cart" />);
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByText(/scan shop.example.org/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /sign in/i })).toBeInTheDocument();
  });
});
