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
