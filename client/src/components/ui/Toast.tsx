import { ReactNode, useCallback, useMemo, useRef, useState } from "react";
import { Toast, ToastContext, ToastTone } from "./useToast";

const toneStyles: Record<ToastTone, string> = {
  success:
    "border-emerald-300 bg-emerald-50 text-emerald-800 dark:border-emerald-500/40 dark:bg-emerald-500/10 dark:text-emerald-200",
  error:
    "border-red-300 bg-red-50 text-red-800 dark:border-red-500/40 dark:bg-red-500/10 dark:text-red-200",
  info: "border-line bg-surface text-fg",
};

const toneDot: Record<ToastTone, string> = {
  success: "bg-emerald-500",
  error: "bg-red-500",
  info: "bg-primary",
};

/**
 * App-wide toast host. Kept separate from the useToast hook/context (in
 * useToast.ts) so React Fast Refresh stays happy — the same split the repo uses
 * for LlmProviderProvider.
 */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const nextId = useRef(1);

  const dismiss = useCallback((id: number) => {
    setToasts((current) => current.filter((item) => item.id !== id));
  }, []);

  const toast = useCallback(
    (message: string, tone: ToastTone = "info") => {
      const id = nextId.current++;
      setToasts((current) => [...current, { id, message, tone }]);
      window.setTimeout(() => dismiss(id), 3500);
    },
    [dismiss],
  );

  const value = useMemo(() => ({ toast }), [toast]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="pointer-events-none fixed inset-x-0 bottom-0 z-[60] flex flex-col items-center gap-2 p-4 sm:items-end">
        {toasts.map((item) => (
          <button
            className={`pointer-events-auto flex w-full max-w-sm animate-toast-in items-center gap-3 rounded-xl border px-4 py-3 text-sm font-medium shadow-pop ${toneStyles[item.tone]}`}
            key={item.id}
            onClick={() => dismiss(item.id)}
            type="button"
          >
            <span className={`h-2 w-2 shrink-0 rounded-full ${toneDot[item.tone]}`} />
            <span className="text-left">{item.message}</span>
          </button>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
