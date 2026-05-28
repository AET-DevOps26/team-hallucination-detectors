type NavButtonProps = {
  active: boolean;
  children: string;
  onClick: () => void;
};

export function NavButton({ active, children, onClick }: NavButtonProps) {
  return (
    <button
      className={`rounded-md px-4 py-2 text-sm font-semibold ${
        active
          ? "bg-zinc-900 text-white"
          : "border border-zinc-300 bg-white text-zinc-700 hover:border-teal-500"
      }`}
      onClick={onClick}
      type="button"
    >
      {children}
    </button>
  );
}
