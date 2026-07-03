type NavButtonProps = {
  active: boolean;
  children: string;
  onClick: () => void;
};

export function NavButton({ active, children, onClick }: NavButtonProps) {
  return (
    <button
      className={`rounded-lg px-3.5 py-2 text-sm font-semibold transition ${
        active
          ? "bg-primary/10 text-primary"
          : "text-muted hover:bg-elevated hover:text-fg"
      }`}
      onClick={onClick}
      type="button"
    >
      {children}
    </button>
  );
}
