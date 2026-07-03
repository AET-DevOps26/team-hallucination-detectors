type SkeletonProps = {
  className?: string;
};

/** Shimmering placeholder block used while content loads. */
export function Skeleton({ className = "" }: SkeletonProps) {
  return (
    <div
      className={`relative overflow-hidden rounded-md bg-elevated ${className}`}
    >
      <div className="absolute inset-0 -translate-x-full animate-shimmer bg-gradient-to-r from-transparent via-fg/10 to-transparent" />
    </div>
  );
}
