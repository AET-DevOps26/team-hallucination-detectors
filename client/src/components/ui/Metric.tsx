type MetricProps = {
  label: string;
  value: string;
};

export function Metric({ label, value }: MetricProps) {
  return (
    <div className="rounded-md border border-zinc-300 bg-white px-4 py-3">
      <p className="text-xl font-semibold">{value}</p>
      <p className="text-xs font-medium uppercase text-zinc-500">{label}</p>
    </div>
  );
}

export function MiniMetric({ label, value }: MetricProps) {
  return (
    <div className="rounded-md border border-zinc-200 px-3 py-2">
      <p className="font-semibold">{value}</p>
      <p className="text-xs text-zinc-500">{label}</p>
    </div>
  );
}
