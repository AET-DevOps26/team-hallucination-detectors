type SpinnerProps = {
  className?: string;
  /** Diameter in Tailwind size units via className; defaults to a small inline spinner. */
  size?: "sm" | "md" | "lg";
};

const sizeClass: Record<NonNullable<SpinnerProps["size"]>, string> = {
  sm: "h-4 w-4 border-2",
  md: "h-6 w-6 border-2",
  lg: "h-9 w-9 border-[3px]",
};

export function Spinner({ className = "", size = "sm" }: SpinnerProps) {
  return (
    <span
      aria-hidden="true"
      className={`inline-block animate-spin rounded-full border-current border-r-transparent align-[-0.125em] ${sizeClass[size]} ${className}`}
      role="status"
    />
  );
}
