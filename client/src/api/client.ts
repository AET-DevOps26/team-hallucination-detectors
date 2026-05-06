import axios from "axios";

const baseURL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export const apiClient = axios.create({
  baseURL,
  timeout: 5000,
  headers: {
    "Content-Type": "application/json",
  },
});

export interface HelloResponse {
  message: string;
}

export async function fetchHello(): Promise<HelloResponse> {
  const { data } = await apiClient.get<HelloResponse>("/api/v1/hello");
  return data;
}
