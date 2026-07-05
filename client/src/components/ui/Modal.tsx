import { ReactNode, useEffect, useRef } from "react";

type ModalProps = {
  open: boolean;
  onClose: () => void;
  children: ReactNode;
  labelledBy?: string;
  /** Max width utility, e.g. "max-w-md". */
  widthClass?: string;
};

/**
 * Accessible dialog: backdrop click + Esc to close, focus moved into the panel
 * on open and restored on close, background scroll locked while open.
 */
export function Modal({
  open,
  onClose,
  children,
  labelledBy,
  widthClass = "max-w-md",
}: ModalProps) {
  const panelRef = useRef<HTMLDivElement>(null);
  const previouslyFocused = useRef<HTMLElement | null>(null);
  // Kept in a ref so the setup effect only depends on `open`, not on the
  // caller's (often inline, identity-changing-every-render) onClose — otherwise
  // the effect re-runs on every keystroke inside the modal and steals focus
  // back to the first focusable element.
  const onCloseRef = useRef(onClose);
  onCloseRef.current = onClose;

  useEffect(() => {
    if (!open) return;

    previouslyFocused.current = document.activeElement as HTMLElement | null;
    const { overflow } = document.body.style;
    document.body.style.overflow = "hidden";

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") onCloseRef.current();
    }
    document.addEventListener("keydown", onKeyDown);

    // Move focus into the dialog for keyboard/screen-reader users.
    const focusable = panelRef.current?.querySelector<HTMLElement>(
      'input, button, [href], select, textarea, [tabindex]:not([tabindex="-1"])',
    );
    focusable?.focus();

    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = overflow;
      previouslyFocused.current?.focus?.();
    };
  }, [open]);

  if (!open) return null;

  return (
    <div
      aria-labelledby={labelledBy}
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      role="dialog"
    >
      <button
        aria-label="Close dialog"
        className="absolute inset-0 animate-fade-in cursor-default bg-black/50 backdrop-blur-sm"
        onClick={onClose}
        tabIndex={-1}
        type="button"
      />
      <div
        className={`relative w-full ${widthClass} animate-scale-in rounded-2xl border border-line bg-surface p-6 shadow-pop`}
        ref={panelRef}
      >
        {children}
      </div>
    </div>
  );
}
