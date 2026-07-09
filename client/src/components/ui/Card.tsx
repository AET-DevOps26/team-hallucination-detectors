import { HTMLAttributes } from "react";

type CardProps = HTMLAttributes<HTMLDivElement> & {
  /** Adds default inner padding; disable for cards with their own header bands. */
  padded?: boolean;
  /** Lifts the card slightly on hover — for interactive/clickable cards. */
  interactive?: boolean;
};

export function Card({
  padded = true,
  interactive = false,
  className = "",
  children,
  ...rest
}: CardProps) {
  return (
    <div
      className={`rounded-xl border border-line bg-surface shadow-card ${
        padded ? "p-5" : ""
      } ${
        interactive
          ? "cursor-pointer transition hover:-translate-y-0.5 hover:border-primary hover:shadow-pop"
          : ""
      } ${className}`}
      {...rest}
    >
      {children}
    </div>
  );
}
