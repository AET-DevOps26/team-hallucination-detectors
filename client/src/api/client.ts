import axios from "axios";

const apiBaseURL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";
const authBaseURL = import.meta.env.VITE_AUTH_BASE_URL ?? "http://localhost:8081";

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
 * Turns an axios failure into an Error whose message is safe to show the user.
 * Prefers the backend's own message (e.g. "Email is already registered",
 * "Invalid email or password"), then falls back to network/timeout/status hints.
 */
function normalizeAuthError(error: unknown): Error {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    const backendMessage = error.response?.data?.message;
    if (backendMessage) {
      return new Error(backendMessage);
    }
    if (error.code === "ECONNABORTED") {
      return new Error("The request timed out. Please try again.");
    }
    if (!error.response) {
      return new Error(
        "Could not reach the authentication service. Please check your connection and try again."
      );
    }
    return new Error(`Authentication request failed (HTTP ${error.response.status}).`);
  }

  return error instanceof Error
    ? error
    : new Error("An unexpected error occurred. Please try again.");
}

authClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => Promise.reject(normalizeAuthError(error))
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
  await authClient.post("/api/v1/auth/register", { email, password });
}

export async function loginUser(email: string, password: string): Promise<AuthResponse> {
  const { data } = await authClient.post<AuthResponse>("/api/v1/auth/login", {
    email,
    password,
  });

  return data;
}

export async function fetchCurrentUser(token: string): Promise<CurrentUserResponse> {
  const { data } = await authClient.get<CurrentUserResponse>("/api/v1/auth/me", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

  return data;
}
