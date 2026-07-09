type MetricProps = {
  label: string;
  value: string;
};

export function Metric({ label, value }: MetricProps) {
  return (
    <div className="rounded-xl border border-line bg-surface px-4 py-3">
      <p className="text-2xl font-semibold text-fg">{value}</p>
      <p className="mt-0.5 text-xs font-medium uppercase tracking-wide text-muted">
        {label}
      </p>
    </div>
  );
}

export function MiniMetric({ label, value }: MetricProps) {
  return (
    <div className="rounded-lg border border-line bg-surface px-3 py-2">
      <p className="font-semibold text-fg">{value}</p>
      <p className="text-xs text-muted">{label}</p>
    </div>
  );
}
