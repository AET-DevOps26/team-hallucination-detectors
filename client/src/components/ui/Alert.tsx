import { ReactNode } from "react";

type AlertTone = "error" | "success" | "info";

type AlertProps = {
  children: ReactNode;
  tone?: AlertTone;
  className?: string;
};

const tones: Record<AlertTone, string> = {
  error:
    "bg-red-50 text-red-700 border-red-200 dark:bg-red-500/10 dark:text-red-300 dark:border-red-500/30",
  success:
    "bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-500/10 dark:text-emerald-300 dark:border-emerald-500/30",
  info: "bg-primary/10 text-primary border-primary/30",
};

export function Alert({ children, tone = "error", className = "" }: AlertProps) {
  return (
    <p
      className={`animate-fade-in rounded-lg border px-3 py-2 text-sm ${tones[tone]} ${className}`}
      role={tone === "error" ? "alert" : "status"}
    >
      {children}
    </p>
  );
}
