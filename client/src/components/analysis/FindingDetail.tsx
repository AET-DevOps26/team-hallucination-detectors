type FindingDetailProps = {
  label: string;
  value: string;
};

export function FindingDetail({ label, value }: FindingDetailProps) {
  return (
    <div>
      <p className="text-sm font-medium text-zinc-500">{label}</p>
      <p className="mt-1 text-zinc-900">{value}</p>
    </div>
  );
}
