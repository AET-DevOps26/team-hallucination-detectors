import { renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useApiHealth } from "./useApiHealth";

vi.mock("../api/client");
const client = vi.mocked(await import("../api/client"));

describe("useApiHealth", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("starts in the loading state", () => {
    client.fetchHello.mockReturnValue(new Promise(() => {})); // never resolves
    const { result } = renderHook(() => useApiHealth());

    expect(result.current).toEqual({ status: "loading" });
  });

  it("reports success with the backend's message", async () => {
    client.fetchHello.mockResolvedValue({ message: "pong" });

    const { result } = renderHook(() => useApiHealth());

    await waitFor(() =>
      expect(result.current).toEqual({ status: "success", message: "pong" }),
    );
  });

  it("reports the error message when the health check fails", async () => {
    client.fetchHello.mockRejectedValue(new Error("Could not reach the API."));

    const { result } = renderHook(() => useApiHealth());

    await waitFor(() =>
      expect(result.current).toEqual({ status: "error", error: "Could not reach the API." }),
    );
  });

  it("falls back to a generic error message for non-Error rejections", async () => {
    client.fetchHello.mockRejectedValue("network down");

    const { result } = renderHook(() => useApiHealth());

    await waitFor(() =>
      expect(result.current).toEqual({ status: "error", error: "Request failed" }),
    );
  });
});
