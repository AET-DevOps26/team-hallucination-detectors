type ProfileRowProps = {
  label: string;
  value: string;
};

export function ProfileRow({ label, value }: ProfileRowProps) {
  return (
    <div className="min-w-0 rounded-lg bg-elevated p-3">
      <p className="text-sm text-muted">{label}</p>
      <p className="mt-1 truncate font-semibold text-fg">{value}</p>
    </div>
  );
}
