import { act, renderHook } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import { useAppRouter } from "./useAppRouter";

describe("useAppRouter", () => {
  afterEach(() => {
    window.history.pushState({}, "", "/");
  });

  it("reads the initial route from the current location", () => {
    window.history.pushState({}, "", "/analysis/new");

    const { result } = renderHook(() => useAppRouter());

    expect(result.current.route).toBe("/analysis/new");
  });

  it("navigate() pushes history state and updates the route", () => {
    const { result } = renderHook(() => useAppRouter());

    act(() => {
      result.current.navigate("/profile");
    });

    expect(result.current.route).toBe("/profile");
    expect(window.location.pathname).toBe("/profile");
  });

  it("updates the route when the browser back/forward triggers popstate", () => {
    window.history.pushState({}, "", "/analysis");
    const { result } = renderHook(() => useAppRouter());

    act(() => {
      window.history.pushState({}, "", "/profile");
      window.dispatchEvent(new PopStateEvent("popstate"));
    });

    expect(result.current.route).toBe("/profile");
  });
});
