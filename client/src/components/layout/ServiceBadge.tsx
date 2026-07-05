import { ApiState } from "../../types/domain";

type ServiceBadgeProps = {
  state: ApiState;
  /** Compact form (dot + label) for the navbar. */
  compact?: boolean;
};

const dotClass = (status: ApiState["status"]) =>
  status === "success"
    ? "bg-emerald-500"
    : status === "error"
      ? "bg-red-500"
      : "bg-amber-500 animate-pulse";

export function ServiceBadge({ state, compact = false }: ServiceBadgeProps) {
  const label =
    state.status === "loading"
      ? "Checking"
      : state.status === "success"
        ? "Reachable"
        : "Unavailable";

  if (compact) {
    return (
      <span
        className="hidden items-center gap-2 rounded-lg border border-line bg-surface px-2.5 py-1.5 text-xs font-medium text-muted md:inline-flex"
        title={`API service: ${label}`}
      >
        <span className={`h-2 w-2 rounded-full ${dotClass(state.status)}`} />
        API
      </span>
    );
  }

  return (
    <div className="flex items-center gap-3 rounded-lg border border-line bg-surface px-4 py-3 text-sm">
      <span className={`h-2.5 w-2.5 rounded-full ${dotClass(state.status)}`} />
      <span className="font-medium text-fg">API service</span>
      <span className="text-muted">{label}</span>
    </div>
  );
}
