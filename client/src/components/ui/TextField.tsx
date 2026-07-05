type TextFieldProps = {
  autoComplete: string;
  label: string;
  onChange: (value: string) => void;
  required?: boolean;
  type: string;
  value: string;
};

export function TextField({
  autoComplete,
  label,
  onChange,
  required,
  type,
  value,
}: TextFieldProps) {
  return (
    <label className="block">
      <span className="text-sm font-medium text-muted">{label}</span>
      <input
        autoComplete={autoComplete}
        className="mt-2 w-full rounded-lg border border-line bg-surface px-3 py-3 text-base text-fg outline-none transition placeholder:text-muted focus:border-primary focus:ring-2 focus:ring-primary/20"
        onChange={(event) => onChange(event.target.value)}
        required={required}
        type={type}
        value={value}
      />
    </label>
  );
}
