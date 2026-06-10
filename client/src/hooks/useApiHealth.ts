import { useEffect, useState } from "react";
import { fetchHello } from "../api/client";
import { ApiState } from "../types/domain";

export function useApiHealth() {
  const [apiState, setApiState] = useState<ApiState>({ status: "loading" });

  useEffect(() => {
    let cancelled = false;
    fetchHello()
      .then((res) => {
        if (!cancelled) setApiState({ status: "success", message: res.message });
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        const error = err instanceof Error ? err.message : "Request failed";
        setApiState({ status: "error", error });
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return apiState;
}
