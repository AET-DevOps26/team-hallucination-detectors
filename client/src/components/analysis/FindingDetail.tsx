type FindingDetailProps = {
  label: string;
  value: string;
};

export function FindingDetail({ label, value }: FindingDetailProps) {
  return (
    <div>
      <p className="text-sm font-medium text-muted">{label}</p>
      <p className="mt-1 text-fg">{value}</p>
    </div>
  );
}
