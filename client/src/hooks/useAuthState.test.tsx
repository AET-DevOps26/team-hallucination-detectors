import { act, renderHook, waitFor } from "@testing-library/react";
import type { FormEvent } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { sessionStorageKey } from "../constants/session";
import { useAuthState } from "./useAuthState";

// devAuthenticated depends on VITE_DEV_AUTHENTICATED, which is only set via a
// local, gitignored .env file. Forcing it to false here keeps this suite
// identical locally and in CI, where that file never exists.
vi.mock("../constants/auth", () => ({ devAuthenticated: false, devSession: undefined }));
vi.mock("../api/client");
const client = vi.mocked(await import("../api/client"));

describe("useAuthState", () => {
  const navigate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    window.localStorage.clear();
    // Default so tests with a pre-stored token don't crash the mount-effect's
    // validation call; tests exercising that call override it explicitly.
    client.fetchCurrentUser.mockResolvedValue({ email: "dev@example.com" });
  });

  function render() {
    return renderHook(() => useAuthState({ navigate }));
  }

  async function submit(result: ReturnType<typeof render>["result"]) {
    const event = { preventDefault: vi.fn() } as unknown as FormEvent<HTMLFormElement>;
    await act(async () => {
      await result.current.handleAuthSubmit(event);
    });
  }

  it("starts with no session when nothing is stored", () => {
    const { result } = render();

    expect(result.current.session).toBeNull();
    expect(result.current.authMode).toBe("login");
  });

  it("restores a previously stored session and masks its token", () => {
    window.localStorage.setItem(
      sessionStorageKey,
      JSON.stringify({ username: "dev", email: "dev@example.com", token: "abcdefghijklmnop" }),
    );

    const { result } = render();

    expect(result.current.session?.username).toBe("dev");
    expect(result.current.maskedToken).toBe("abcdef...klmnop");
  });

  it("clears a stored session whose token the auth service rejects", async () => {
    window.localStorage.setItem(
      sessionStorageKey,
      JSON.stringify({ username: "dev", email: "dev@example.com", token: "stale-token" }),
    );
    client.fetchCurrentUser.mockRejectedValue(new Error("401"));

    const { result } = render();

    await waitFor(() => expect(result.current.session).toBeNull());
    expect(window.localStorage.getItem(sessionStorageKey)).toBeNull();
  });

  it("logs in, persists the session, and clears the password", async () => {
    client.loginUser.mockResolvedValue({ token: "new-token", email: "dev@example.org" });
    const { result } = render();

    act(() => {
      result.current.setEmail("dev@example.org");
      result.current.setPassword("hunter2");
    });
    await submit(result);

    expect(client.loginUser).toHaveBeenCalledWith("dev@example.org", "hunter2");
    expect(result.current.session).toEqual({
      username: "dev",
      email: "dev@example.org",
      token: "new-token",
    });
    expect(result.current.password).toBe("");
    expect(JSON.parse(window.localStorage.getItem(sessionStorageKey)!)).toEqual(
      result.current.session,
    );
  });

  it("surfaces the backend's error message when login fails", async () => {
    client.loginUser.mockRejectedValue(new Error("Invalid credentials."));
    const { result } = render();

    act(() => {
      result.current.setEmail("dev@example.org");
      result.current.setPassword("wrong");
    });
    await submit(result);

    expect(result.current.authError).toBe("Invalid credentials.");
    expect(result.current.session).toBeNull();
    expect(navigate).not.toHaveBeenCalled();
  });

  it("registers, then switches back to login mode with a confirmation message", async () => {
    client.registerUser.mockResolvedValue(undefined);
    const { result } = render();

    act(() => {
      result.current.setAuthMode("register");
      result.current.setEmail("new@example.org");
      result.current.setPassword("hunter2");
    });
    await submit(result);

    expect(client.registerUser).toHaveBeenCalledWith("new@example.org", "hunter2");
    expect(result.current.authMode).toBe("login");
    expect(result.current.authMessage).toContain("Registration successful");
    expect(result.current.password).toBe("");
  });

  it("requests a password reset and switches back to login mode", async () => {
    client.forgotPassword.mockResolvedValue(undefined);
    const { result } = render();

    act(() => {
      result.current.setAuthMode("forgot-password");
      result.current.setEmail("dev@example.org");
    });
    await submit(result);

    expect(client.forgotPassword).toHaveBeenCalledWith("dev@example.org");
    expect(result.current.authMode).toBe("login");
    expect(result.current.authMessage).toContain("reset link");
  });

  it("logs out: clears session, form fields, and navigates to /login", () => {
    window.localStorage.setItem(
      sessionStorageKey,
      JSON.stringify({ username: "dev", email: "dev@example.com", token: "abc" }),
    );
    const { result } = render();

    act(() => {
      result.current.setEmail("leftover@example.org");
      result.current.handleLogout();
    });

    expect(result.current.session).toBeNull();
    expect(result.current.email).toBe("");
    expect(window.localStorage.getItem(sessionStorageKey)).toBeNull();
    expect(navigate).toHaveBeenCalledWith("/login");
  });
});
