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
      <span className="text-sm font-medium text-zinc-700">{label}</span>
      <input
        autoComplete={autoComplete}
        className="mt-2 w-full rounded-md border border-zinc-300 px-3 py-3 text-base outline-none transition focus:border-teal-600 focus:ring-2 focus:ring-teal-100"
        onChange={(event) => onChange(event.target.value)}
        required={required}
        type={type}
        value={value}
      />
    </label>
  );
}
