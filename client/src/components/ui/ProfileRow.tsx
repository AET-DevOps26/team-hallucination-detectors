type ProfileRowProps = {
  label: string;
  value: string;
};

export function ProfileRow({ label, value }: ProfileRowProps) {
  return (
    <div className="min-w-0 rounded-md bg-zinc-50 p-3">
      <p className="text-sm text-zinc-500">{label}</p>
      <p className="mt-1 truncate font-semibold">{value}</p>
    </div>
  );
}
