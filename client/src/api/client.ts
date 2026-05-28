import axios from "axios";

const baseURL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";
const authBaseURL =
  import.meta.env.VITE_AUTH_BASE_URL ?? "http://localhost:8081";
const loginPath = import.meta.env.VITE_AUTH_LOGIN_PATH ?? "/api/v1/auth/login";

export const apiClient = axios.create({
  baseURL,
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

export interface HelloResponse {
  message: string;
}

export interface LoginCredentials {
  username: string;
  password: string;
}

export interface LoginResult {
  token?: string;
  username: string;
  raw: unknown;
}

export async function fetchHello(): Promise<HelloResponse> {
  const { data } = await apiClient.get<HelloResponse>("/api/v1/hello");
  return data;
}

function getToken(data: unknown): string | undefined {
  if (!data || typeof data !== "object") return undefined;

  const record = data as Record<string, unknown>;
  const tokenKeys = ["token", "accessToken", "jwt", "idToken"];
  const token = tokenKeys
    .map((key) => record[key])
    .find((value): value is string => typeof value === "string");

  if (token) return token;

  const nested = record.data;
  if (nested && typeof nested === "object") {
    return getToken(nested);
  }

  return undefined;
}

export async function login(
  credentials: LoginCredentials,
): Promise<LoginResult> {
  const { data } = await authClient.post(loginPath, credentials);
  return {
    token: getToken(data),
    username: credentials.username,
    raw: data,
  };
}
