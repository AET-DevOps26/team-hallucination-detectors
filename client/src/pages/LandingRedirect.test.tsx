import { render } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { LandingRedirect } from "./LandingRedirect";

describe("LandingRedirect", () => {
  it("redirects to /profile when there is a session", () => {
    const navigate = vi.fn();

    render(<LandingRedirect hasSession navigate={navigate} />);

    expect(navigate).toHaveBeenCalledWith("/profile");
  });

  it("redirects to /login when there is no session", () => {
    const navigate = vi.fn();

    render(<LandingRedirect hasSession={false} navigate={navigate} />);

    expect(navigate).toHaveBeenCalledWith("/login");
  });

  it("renders nothing", () => {
    const { container } = render(<LandingRedirect hasSession navigate={vi.fn()} />);

    expect(container).toBeEmptyDOMElement();
  });
});
