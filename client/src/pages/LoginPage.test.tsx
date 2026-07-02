import { render, screen, fireEvent } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { LoginPage } from "./LoginPage";

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
};

describe("LoginPage", () => {
  it("renders login form by default", () => {
    render(<LoginPage {...baseProps} />);
    expect(screen.getByText("Sign in")).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
  });

  it("shows register button text in register mode", () => {
    render(<LoginPage {...baseProps} authMode="register" />);
    expect(screen.getByText("Create account")).toBeInTheDocument();
  });

  it("displays auth error when provided", () => {
    render(<LoginPage {...baseProps} authError="Invalid credentials" />);
    expect(screen.getByText("Invalid credentials")).toBeInTheDocument();
  });

  it("displays success message when provided", () => {
    render(<LoginPage {...baseProps} authMessage="Reset link sent!" />);
    expect(screen.getByText("Reset link sent!")).toBeInTheDocument();
  });

  it("disables submit button while loading", () => {
    render(<LoginPage {...baseProps} authStatus="loading" />);
    expect(screen.getByRole("button", { name: /working/i })).toBeDisabled();
  });

  it("calls onAuthModeChange when Register tab is clicked", () => {
    const onAuthModeChange = vi.fn();
    render(<LoginPage {...baseProps} onAuthModeChange={onAuthModeChange} />);
    fireEvent.click(screen.getByRole("button", { name: /register/i }));
    expect(onAuthModeChange).toHaveBeenCalledWith("register");
  });

  it("calls onAuthModeChange when Forgot password is clicked", () => {
    const onAuthModeChange = vi.fn();
    render(<LoginPage {...baseProps} onAuthModeChange={onAuthModeChange} />);
    fireEvent.click(screen.getByText(/forgot your password/i));
    expect(onAuthModeChange).toHaveBeenCalledWith("forgot-password");
  });

  it("renders forgot-password form with email field only", () => {
    render(<LoginPage {...baseProps} authMode="forgot-password" />);
    expect(screen.getByText("Reset your password")).toBeInTheDocument();
    expect(screen.getByText(/send reset link/i)).toBeInTheDocument();
    expect(screen.queryByLabelText(/password/i)).not.toBeInTheDocument();
  });

  it("calls onAuthSubmit on form submit", () => {
    const onAuthSubmit = vi.fn((e) => e.preventDefault());
    render(<LoginPage {...baseProps} onAuthSubmit={onAuthSubmit} />);
    fireEvent.submit(screen.getByRole("button", { name: /sign in/i }).closest("form")!);
    expect(onAuthSubmit).toHaveBeenCalled();
  });
});