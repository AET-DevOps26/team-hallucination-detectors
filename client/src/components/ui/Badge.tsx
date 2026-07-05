import { ReactNode } from "react";

type Tone = "neutral" | "primary" | "success" | "warn" | "danger";

type BadgeProps = {
  children: ReactNode;
  tone?: Tone;
  /** Pass raw classes (e.g. severity styles from constants) to fully override. */
  className?: string;
  size?: "sm" | "md";
};

const tones: Record<Tone, string> = {
  neutral: "bg-elevated text-muted border-line",
  primary: "bg-primary/10 text-primary border-primary/30",
  success:
    "bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-500/10 dark:text-emerald-300 dark:border-emerald-500/30",
  warn: "bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-500/10 dark:text-amber-300 dark:border-amber-500/30",
  danger:
    "bg-red-50 text-red-700 border-red-200 dark:bg-red-500/10 dark:text-red-300 dark:border-red-500/30",
};

export function Badge({
  children,
  tone = "neutral",
  className,
  size = "md",
}: BadgeProps) {
  const sizeClass = size === "sm" ? "px-1.5 py-0.5 text-[11px]" : "px-2.5 py-1 text-xs";
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full border font-semibold ${sizeClass} ${
        className ?? tones[tone]
      }`}
    >
      {children}
    </span>
  );
}
