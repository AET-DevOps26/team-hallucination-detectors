import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { ApiState } from "../../types/domain";
import { ServiceBadge } from "./ServiceBadge";

describe("ServiceBadge", () => {
  it("shows 'Checking' while loading", () => {
    render(<ServiceBadge state={{ status: "loading" }} />);

    expect(screen.getByText("Checking")).toBeInTheDocument();
  });

  it("shows 'Reachable' on success", () => {
    const state: ApiState = { status: "success", message: "pong" };
    render(<ServiceBadge state={state} />);

    expect(screen.getByText("Reachable")).toBeInTheDocument();
  });

  it("shows 'Unavailable' on error", () => {
    const state: ApiState = { status: "error", error: "timeout" };
    render(<ServiceBadge state={state} />);

    expect(screen.getByText("Unavailable")).toBeInTheDocument();
  });

  it("only renders one status label at a time", () => {
    render(<ServiceBadge state={{ status: "success", message: "pong" }} />);

    expect(screen.queryByText("Checking")).not.toBeInTheDocument();
    expect(screen.queryByText("Unavailable")).not.toBeInTheDocument();
  });
});
