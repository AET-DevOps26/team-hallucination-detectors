import axios, { AxiosError, AxiosResponse } from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";
import { sessionStorageKey } from "../constants/session";

// devAuthenticated depends on VITE_DEV_AUTHENTICATED, which is only set via a
// local, gitignored .env file. Forcing it to false here keeps this suite
// identical locally and in CI, where that file never exists.
vi.mock("../constants/auth", () => ({ devAuthenticated: false, devSession: undefined }));
import {
  ApiError,
  apiClient,
  authClient,
  fetchCurrentUser,
  fetchHello,
  forgotPassword,
  loginUser,
  registerUser,
  resetPassword,
} from "./client";

/**
 * Overriding the axios instance's adapter (rather than mocking `axios` or the
 * exported functions) lets these tests exercise the real request/response
 * interceptors — including `normalizeError` — exactly as production does.
 */
function respondOnce(client: typeof apiClient, data: unknown, status = 200) {
  client.defaults.adapter = vi.fn().mockImplementation(
    (config) =>
      Promise.resolve({
        data,
        status,
        statusText: "OK",
        headers: {},
        config,
      } satisfies AxiosResponse),
  );
}

function rejectOnce(
  client: typeof apiClient,
  options: { status?: number; data?: unknown; code?: string; noResponse?: boolean },
) {
  client.defaults.adapter = vi.fn().mockImplementation((config) => {
    const response = options.noResponse
      ? undefined
      : ({
          status: options.status ?? 500,
          data: options.data ?? {},
          statusText: "",
          headers: {},
          config,
        } as AxiosResponse);
    return Promise.reject(
      new AxiosError("request failed", options.code, config, {}, response),
    );
  });
}

describe("apiClient/authClient error normalization", () => {
  afterEach(() => {
    // Restore each instance's real HTTP adapter so later tests (and other
    // suites importing this module) aren't left talking to the stub.
    apiClient.defaults.adapter = axios.defaults.adapter;
    authClient.defaults.adapter = axios.defaults.adapter;
  });

  it("prefers the backend's unified error message and code", async () => {
    rejectOnce(apiClient, {
      status: 409,
      data: { code: "WEBSITE_ALREADY_REGISTERED", message: "You already registered this URL." },
    });

    await expect(fetchHello()).rejects.toMatchObject({
      name: "ApiError",
      message: "You already registered this URL.",
      code: "WEBSITE_ALREADY_REGISTERED",
      status: 409,
    });
  });

  it("reports a timeout with a friendly message", async () => {
    rejectOnce(apiClient, { code: "ECONNABORTED", noResponse: true });

    await expect(fetchHello()).rejects.toMatchObject({
      message: "The request timed out. Please try again.",
    });
  });

  it("reports an unreachable service distinctly from a timeout", async () => {
    rejectOnce(apiClient, { noResponse: true });

    await expect(fetchHello()).rejects.toMatchObject({
      message: "Could not reach the API. Please check your connection and try again.",
    });
  });

  it("labels the auth service distinctly from the API in unreachable errors", async () => {
    rejectOnce(authClient, { noResponse: true });

    await expect(loginUser("a@b.com", "pw")).rejects.toMatchObject({
      message: "Could not reach the authentication service. Please check your connection and try again.",
    });
  });

  it("falls back to a generic HTTP status message when the backend sends no body message", async () => {
    rejectOnce(apiClient, { status: 503, data: {} });

    await expect(fetchHello()).rejects.toMatchObject({
      message: "Request failed (HTTP 503).",
      status: 503,
    });
  });

  it("wraps a non-axios failure without losing the original message", async () => {
    apiClient.defaults.adapter = vi.fn().mockRejectedValue(new Error("boom"));

    await expect(fetchHello()).rejects.toMatchObject({ name: "ApiError", message: "boom" });
  });
});

describe("apiClient request interceptor", () => {
  afterEach(() => {
    window.localStorage.removeItem(sessionStorageKey);
    apiClient.defaults.adapter = axios.defaults.adapter;
  });

  it("attaches the stored session's bearer token to every request", async () => {
    window.localStorage.setItem(
      sessionStorageKey,
      JSON.stringify({ username: "dev", token: "secret-token" }),
    );
    let capturedAuthHeader: unknown;
    apiClient.defaults.adapter = vi.fn().mockImplementation((config) => {
      capturedAuthHeader = config.headers?.Authorization;
      return Promise.resolve({
        data: { message: "ok" },
        status: 200,
        statusText: "OK",
        headers: {},
        config,
      } satisfies AxiosResponse);
    });

    await fetchHello();

    expect(capturedAuthHeader).toBe("Bearer secret-token");
  });

  it("sends no Authorization header when there is no stored session", async () => {
    let capturedAuthHeader: unknown = "unset";
    apiClient.defaults.adapter = vi.fn().mockImplementation((config) => {
      capturedAuthHeader = config.headers?.Authorization;
      return Promise.resolve({
        data: { message: "ok" },
        status: 200,
        statusText: "OK",
        headers: {},
        config,
      } satisfies AxiosResponse);
    });

    await fetchHello();

    expect(capturedAuthHeader).toBeUndefined();
  });
});

describe("auth API functions", () => {
  afterEach(() => {
    authClient.defaults.adapter = axios.defaults.adapter;
  });

  it("registerUser posts to /register and resolves without a value", async () => {
    let seenUrl: string | undefined;
    let seenBody: unknown;
    authClient.defaults.adapter = vi.fn().mockImplementation((config) => {
      seenUrl = config.url;
      seenBody = JSON.parse(config.data);
      return Promise.resolve({
        data: undefined,
        status: 200,
        statusText: "OK",
        headers: {},
        config,
      } satisfies AxiosResponse);
    });

    await registerUser("a@b.com", "hunter2");

    expect(seenUrl).toBe("/register");
    expect(seenBody).toEqual({ email: "a@b.com", password: "hunter2" });
  });

  it("loginUser posts credentials and returns the token/email", async () => {
    respondOnce(authClient, { token: "tok", email: "a@b.com" });

    await expect(loginUser("a@b.com", "hunter2")).resolves.toEqual({
      token: "tok",
      email: "a@b.com",
    });
  });

  it("fetchCurrentUser sends the given token as a bearer header, not the stored session", async () => {
    let capturedAuthHeader: unknown;
    authClient.defaults.adapter = vi.fn().mockImplementation((config) => {
      capturedAuthHeader = config.headers?.Authorization;
      return Promise.resolve({
        data: { email: "a@b.com" },
        status: 200,
        statusText: "OK",
        headers: {},
        config,
      } satisfies AxiosResponse);
    });

    await fetchCurrentUser("explicit-token");

    expect(capturedAuthHeader).toBe("Bearer explicit-token");
  });

  it("forgotPassword posts the email to /forgot-password", async () => {
    let seenUrl: string | undefined;
    authClient.defaults.adapter = vi.fn().mockImplementation((config) => {
      seenUrl = config.url;
      return Promise.resolve({
        data: undefined,
        status: 200,
        statusText: "OK",
        headers: {},
        config,
      } satisfies AxiosResponse);
    });

    await forgotPassword("a@b.com");

    expect(seenUrl).toBe("/forgot-password");
  });

  it("resetPassword posts the token and new password to /reset-password", async () => {
    let seenBody: unknown;
    authClient.defaults.adapter = vi.fn().mockImplementation((config) => {
      seenBody = JSON.parse(config.data);
      return Promise.resolve({
        data: undefined,
        status: 200,
        statusText: "OK",
        headers: {},
        config,
      } satisfies AxiosResponse);
    });

    await resetPassword("reset-token", "NewPassword1!");

    expect(seenBody).toEqual({ token: "reset-token", newPassword: "NewPassword1!" });
  });
});

describe("ApiError", () => {
  it("carries the backend code and status alongside the message", () => {
    const error = new ApiError("Not found.", "NOT_FOUND", 404);

    expect(error.message).toBe("Not found.");
    expect(error.code).toBe("NOT_FOUND");
    expect(error.status).toBe(404);
    expect(error.name).toBe("ApiError");
    expect(error).toBeInstanceOf(Error);
  });
});
