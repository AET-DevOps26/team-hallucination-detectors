import { createContext, useContext } from "react";

export type ToastTone = "success" | "error" | "info";

export type Toast = {
  id: number;
  message: string;
  tone: ToastTone;
};

export type ToastContextValue = {
  toast: (message: string, tone?: ToastTone) => void;
};

export const ToastContext = createContext<ToastContextValue | null>(null);

export function useToast(): ToastContextValue {
  const context = useContext(ToastContext);
  if (!context) {
    // Fail soft: outside a provider (e.g. isolated tests), toasts are no-ops.
    return { toast: () => {} };
  }
  return context;
}
