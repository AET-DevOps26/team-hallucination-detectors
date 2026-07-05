import { createContext, useContext } from "react";

/**
 * Which LLM backend VibeShield's GenAI calls use. All are OpenAI-compatible:
 * - "logos": the TUM Logos gateway provided by the course.
 * - "openai": OpenAI's own API.
 * - "selfhosted": a model we host ourselves (Ollama), running in-cluster.
 * The choice is surfaced as a toggle in the "Generate fix prompt" panel and sent
 * with each GenAI request so the langchain-service knows which provider to call.
 */
export type LlmProvider = "logos" | "openai" | "selfhosted";

export const LLM_PROVIDERS: { value: LlmProvider; label: string }[] = [
  { value: "logos", label: "TUM Logos" },
  { value: "openai", label: "OpenAI" },
  { value: "selfhosted", label: "Self-hosted (Ollama)" },
];

export const LLM_PROVIDER_STORAGE_KEY = "vibeshield.llmProvider";
export const DEFAULT_LLM_PROVIDER: LlmProvider = "logos";

export function readStoredProvider(): LlmProvider {
  if (typeof window === "undefined") return DEFAULT_LLM_PROVIDER;
  const stored = window.localStorage.getItem(LLM_PROVIDER_STORAGE_KEY);
  return stored === "openai" || stored === "logos" || stored === "selfhosted"
    ? stored
    : DEFAULT_LLM_PROVIDER;
}

export type LlmProviderContextValue = {
  provider: LlmProvider;
  setProvider: (provider: LlmProvider) => void;
};

export const LlmProviderContext = createContext<LlmProviderContextValue | undefined>(
  undefined,
);

export function useLlmProvider(): LlmProviderContextValue {
  const ctx = useContext(LlmProviderContext);
  if (!ctx) {
    throw new Error("useLlmProvider must be used within an LlmProviderProvider");
  }
  return ctx;
}
