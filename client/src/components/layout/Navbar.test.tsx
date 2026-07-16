import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { ApiState, Session } from "../../types/domain";
import { Navbar } from "./Navbar";

const session: Session = { username: "dev-user", email: "dev@example.org", token: "tok" };
const apiState: ApiState = { status: "success", message: "ok" };

function renderNavbar(overrides: Partial<Parameters<typeof Navbar>[0]> = {}) {
  return render(
    <Navbar
      navigate={vi.fn()}
      onLogout={vi.fn()}
      route="/login"
      session={null}
      theme="light"
      onToggleTheme={vi.fn()}
      apiState={apiState}
      {...overrides}
    />,
  );
}

describe("Navbar", () => {
  it("shows a Sign in button and no username/logout when logged out", () => {
    renderNavbar({ session: null });

    expect(screen.getByRole("button", { name: "Sign in" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Log out" })).not.toBeInTheDocument();
  });

  it("shows the username and a Log out button when logged in", () => {
    renderNavbar({ session });

    expect(screen.getByText("dev-user")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Log out" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Sign in" })).not.toBeInTheDocument();
  });

  it("calls onLogout when Log out is clicked", () => {
    const onLogout = vi.fn();
    renderNavbar({ session, onLogout });

    fireEvent.click(screen.getByRole("button", { name: "Log out" }));

    expect(onLogout).toHaveBeenCalled();
  });

  it("navigates to / when the brand button is clicked", () => {
    const navigate = vi.fn();
    renderNavbar({ navigate });

    fireEvent.click(screen.getByRole("button", { name: "VibeShield" }));

    expect(navigate).toHaveBeenLastCalledWith("/");
  });

  it("only shows the API service badge when logged in", () => {
    const { rerender } = renderNavbar({ session: null });
    expect(screen.queryByTitle(/API service/)).not.toBeInTheDocument();

    rerender(
      <Navbar
        navigate={vi.fn()}
        onLogout={vi.fn()}
        route="/analysis"
        session={session}
        theme="light"
        onToggleTheme={vi.fn()}
        apiState={apiState}
      />,
    );
    expect(screen.getByTitle(/API service/)).toBeInTheDocument();
  });
});
