import { useCallback, useMemo, useState } from "react";
import type { ReactNode } from "react";
import {
  LLM_PROVIDER_STORAGE_KEY,
  LlmProvider,
  LlmProviderContext,
  readStoredProvider,
} from "./useLlmProvider";

/**
 * App-wide provider for the selected LLM backend. Kept in its own file (separate
 * from the hook and constants) so React Fast Refresh stays happy.
 */
export function LlmProviderProvider({ children }: { children: ReactNode }) {
  const [provider, setProviderState] = useState<LlmProvider>(readStoredProvider);

  const setProvider = useCallback((next: LlmProvider) => {
    setProviderState(next);
    window.localStorage.setItem(LLM_PROVIDER_STORAGE_KEY, next);
  }, []);

  const value = useMemo(() => ({ provider, setProvider }), [provider, setProvider]);

  return (
    <LlmProviderContext.Provider value={value}>{children}</LlmProviderContext.Provider>
  );
}
