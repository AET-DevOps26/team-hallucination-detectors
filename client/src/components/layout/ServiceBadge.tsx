import { ApiState } from "../../types/domain";

type ServiceBadgeProps = {
  state: ApiState;
};

export function ServiceBadge({ state }: ServiceBadgeProps) {
  return (
    <div className="flex items-center gap-3 rounded-md border border-zinc-300 bg-white px-4 py-3 text-sm">
      <span
        className={`h-2.5 w-2.5 rounded-full ${
          state.status === "success"
            ? "bg-emerald-500"
            : state.status === "error"
              ? "bg-red-500"
              : "bg-amber-500"
        }`}
      />
      <span className="font-medium">API service</span>
      <span className="text-zinc-500">
        {state.status === "loading" && "Checking"}
        {state.status === "success" && "Reachable"}
        {state.status === "error" && "Unavailable"}
      </span>
    </div>
  );
}
