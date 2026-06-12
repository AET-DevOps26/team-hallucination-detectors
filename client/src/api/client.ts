import axios from "axios";
import { readStoredSession } from "../utils/session";

const apiBaseURL = import.meta.env.VITE_API_BASE_URL ?? "";
const authBaseURL = import.meta.env.VITE_AUTH_BASE_URL ?? "/auth";

export const apiClient = axios.create({
  baseURL: apiBaseURL,
  timeout: 5000,
  headers: {
    "Content-Type": "application/json",
  },
});

export const authClient = axios.create({
  baseURL: authBaseURL,
  timeout: 5000,
  headers: {
    "Content-Type": "application/json",
  },
});

/** Shape of the unified backend error body: { code, message, details }. */
interface ApiErrorResponse {
  code?: string;
  message?: string;
  details?: unknown;
}

/**
 * Error with the backend's stable error code attached, so callers can branch on
 * contract codes (e.g. WEBSITE_ALREADY_REGISTERED) while still having a message
 * that is safe to show the user.
 */
export class ApiError extends Error {
  constructor(
    message: string,
    readonly code?: string,
    readonly status?: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

/**
 * Turns an axios failure into an ApiError whose message is safe to show the user.
 * Prefers the backend's own message (e.g. "You already registered this URL."),
 * then falls back to network/timeout/status hints.
 */
function normalizeError(error: unknown, serviceLabel: string): ApiError {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    const body = error.response?.data;
    if (body?.message) {
      return new ApiError(body.message, body.code, error.response?.status);
    }
    if (error.code === "ECONNABORTED") {
      return new ApiError("The request timed out. Please try again.");
    }
    if (!error.response) {
      return new ApiError(
        `Could not reach the ${serviceLabel}. Please check your connection and try again.`,
      );
    }
    return new ApiError(
      `Request failed (HTTP ${error.response.status}).`,
      undefined,
      error.response.status,
    );
  }

  return error instanceof Error
    ? new ApiError(error.message)
    : new ApiError("An unexpected error occurred. Please try again.");
}

// Every API call carries the stored JWT; the backend rejects anything without it.
apiClient.interceptors.request.use((config) => {
  const token = readStoredSession()?.token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => Promise.reject(normalizeError(error, "API")),
);

authClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => Promise.reject(normalizeError(error, "authentication service")),
);

export interface HelloResponse {
  message: string;
}

export interface AuthResponse {
  token: string;
  email: string;
}

export interface CurrentUserResponse {
  email: string;
}

export async function fetchHello(): Promise<HelloResponse> {
  const { data } = await apiClient.get<HelloResponse>("/api/v1/hello");
  return data;
}

export async function registerUser(email: string, password: string): Promise<void> {
  await authClient.post("/register", { email, password });
}

export async function loginUser(email: string, password: string): Promise<AuthResponse> {
  const { data } = await authClient.post<AuthResponse>("/login", {
    email,
    password,
  });

  return data;
}

export async function fetchCurrentUser(token: string): Promise<CurrentUserResponse> {
  const { data } = await authClient.get<CurrentUserResponse>("/me", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  return data;
}