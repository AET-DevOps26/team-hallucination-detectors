import { useEffect, useState } from "react";
import { fetchHello } from "./api/client";

type State =
  | { status: "loading" }
  | { status: "success"; message: string }
  | { status: "error"; error: string };

export default function App() {
  const [state, setState] = useState<State>({ status: "loading" });

  useEffect(() => {
    let cancelled = false;
    fetchHello()
      .then((res) => {
        if (!cancelled) setState({ status: "success", message: res.message });
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        const error = err instanceof Error ? err.message : "Request failed";
        setState({ status: "error", error });
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="min-h-full flex items-center justify-center bg-slate-50 p-6">
      <div className="w-full max-w-md rounded-2xl bg-white shadow-md p-8 text-center">
        <h1 className="text-2xl font-semibold text-slate-900 mb-4">VibeShield</h1>
        {state.status === "loading" && (
          <p className="text-slate-500" role="status">
            Loading…
          </p>
        )}
        {state.status === "success" && (
          <p className="text-slate-800 text-lg">{state.message}</p>
        )}
        {state.status === "error" && (
          <p className="text-red-600" role="alert">
            Could not reach the API: {state.error}
          </p>
        )}
      </div>
    </div>
  );
}
