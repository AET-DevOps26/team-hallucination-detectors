import { Modal } from "../ui/Modal";
import { AuthForm, AuthFormProps } from "./AuthForm";
import { hostnameOf } from "../../utils/url";

type AuthModalProps = AuthFormProps & {
  open: boolean;
  onClose: () => void;
  /** The URL the user wants to scan; shown as context and scanned after login. */
  pendingUrl?: string;
};

/**
 * Login/register gate shown over the hero when an unauthenticated user starts a
 * scan. On successful login the pending scan auto-starts (handled in App via a
 * session-change effect), so this component only has to present the form.
 */
export function AuthModal({ open, onClose, pendingUrl, ...formProps }: AuthModalProps) {
  return (
    <Modal labelledBy="auth-modal-title" onClose={onClose} open={open}>
      <div className="mb-4 flex items-center gap-3">
        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-primary">
          <svg className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth={1.7} viewBox="0 0 24 24">
            <path d="M12 3l7 3v6c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6l7-3z" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </span>
        <div id="auth-modal-title">
          <p className="text-xs font-semibold uppercase tracking-wide text-primary">
            One step away
          </p>
          <p className="text-sm text-muted">
            {pendingUrl
              ? `Sign in to scan ${hostnameOf(pendingUrl)}`
              : "Sign in to run your scan"}
          </p>
        </div>
      </div>
      <AuthForm {...formProps} />
    </Modal>
  );
}
