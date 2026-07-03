import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { Session } from "../../types/domain";
import { Navbar } from "./Navbar";

const session: Session = { username: "dev-user", email: "dev@example.org", token: "tok" };

describe("Navbar", () => {
  it("shows a Login link and no username/logout when logged out", () => {
    render(<Navbar navigate={vi.fn()} onLogout={vi.fn()} route="/login" session={null} />);

    expect(screen.getByRole("button", { name: "Login" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Log out" })).not.toBeInTheDocument();
  });

  it("shows the username and a Log out button when logged in", () => {
    render(<Navbar navigate={vi.fn()} onLogout={vi.fn()} route="/profile" session={session} />);

    expect(screen.getByRole("button", { name: "dev-user" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Log out" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Login" })).not.toBeInTheDocument();
  });

  it("calls onLogout when Log out is clicked", () => {
    const onLogout = vi.fn();
    render(<Navbar navigate={vi.fn()} onLogout={onLogout} route="/profile" session={session} />);

    fireEvent.click(screen.getByRole("button", { name: "Log out" }));

    expect(onLogout).toHaveBeenCalled();
  });

  it("navigates to /profile or /login for the brand button depending on session", () => {
    const navigate = vi.fn();
    const { rerender } = render(
      <Navbar navigate={navigate} onLogout={vi.fn()} route="/login" session={null} />,
    );
    fireEvent.click(screen.getByRole("button", { name: "VibeShield" }));
    expect(navigate).toHaveBeenLastCalledWith("/login");

    rerender(<Navbar navigate={navigate} onLogout={vi.fn()} route="/profile" session={session} />);
    fireEvent.click(screen.getByRole("button", { name: "VibeShield" }));
    expect(navigate).toHaveBeenLastCalledWith("/profile");
  });

  it.each([
    ["/analysis", true],
    ["/analysis/42", true],
    ["/analysis/new", false], // its own tab, must not also light up Analysis
    ["/profile", false],
  ])("marks the Analysis tab active for route %s: %s", (route, expectedActive) => {
    render(<Navbar navigate={vi.fn()} onLogout={vi.fn()} route={route} session={session} />);

    const analysisTab = screen.getByRole("button", { name: "Analysis" });
    expect(analysisTab.className.includes("bg-zinc-900")).toBe(expectedActive);
  });
});
