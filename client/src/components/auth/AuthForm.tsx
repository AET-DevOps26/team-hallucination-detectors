import { FormEvent } from "react";
import { Alert } from "../ui/Alert";
import { Button } from "../ui/Button";
import { TextField } from "../ui/TextField";
import { AuthMode } from "../../types/domain";

export type AuthFormProps = {
  authError: string;
  authMessage: string;
  authMode: AuthMode;
  authStatus: "idle" | "loading";
  email: string;
  onAuthModeChange: (mode: AuthMode) => void;
  onAuthSubmit: (event: FormEvent<HTMLFormElement>) => void;
  password: string;
  setEmail: (value: string) => void;
  setPassword: (value: string) => void;
  /** Optional context line shown under the heading (e.g. the pending scan URL). */
  subtitle?: string;
};

/**
 * The login / register / forgot-password form, shared by the standalone
 * /login page and the scan-gating AuthModal so the two never drift apart.
 */
export function AuthForm({
  authError,
  authMessage,
  authMode,
  authStatus,
  email,
  onAuthModeChange,
  onAuthSubmit,
  password,
  setEmail,
  setPassword,
  subtitle,
}: AuthFormProps) {
  const loading = authStatus === "loading";

  if (authMode === "forgot-password") {
    return (
      <div>
        <h2 className="text-xl font-semibold text-fg">Reset your password</h2>
        <p className="mt-1 text-sm text-muted">
          Enter your email address and we'll send a reset link.
        </p>
        <form className="mt-5 space-y-4" onSubmit={onAuthSubmit}>
          <TextField autoComplete="email" label="Email" onChange={setEmail} required type="email" value={email} />
          {authError && <Alert tone="error">{authError}</Alert>}
          {authMessage && <Alert tone="success">{authMessage}</Alert>}
          <Button fullWidth loading={loading} size="lg" type="submit">
            {loading ? "Sending..." : "Send reset link"}
          </Button>
          <Button fullWidth onClick={() => onAuthModeChange("login")} variant="ghost">
            Back to login
          </Button>
        </form>
      </div>
    );
  }

  return (
    <div>
      <h2 className="text-xl font-semibold text-fg">Account access</h2>
      {subtitle && <p className="mt-1 truncate text-sm text-muted">{subtitle}</p>}
      <div className="mt-4 grid grid-cols-2 gap-1 rounded-lg bg-elevated p-1">
        {(["login", "register"] as AuthMode[]).map((mode) => (
          <button
            className={`rounded-md px-3 py-2 text-sm font-semibold transition ${
              authMode === mode
                ? "bg-surface text-fg shadow-sm"
                : "text-muted hover:text-fg"
            }`}
            key={mode}
            onClick={() => onAuthModeChange(mode)}
            type="button"
          >
            {mode === "login" ? "Login" : "Register"}
          </button>
        ))}
      </div>

      <form className="mt-5 space-y-4" onSubmit={onAuthSubmit}>
        <TextField autoComplete="email" label="Email" onChange={setEmail} required type="email" value={email} />
        <TextField
          autoComplete={authMode === "login" ? "current-password" : "new-password"}
          label="Password"
          onChange={setPassword}
          required
          type="password"
          value={password}
        />
        {authError && <Alert tone="error">{authError}</Alert>}
        {authMessage && <Alert tone="success">{authMessage}</Alert>}
        <Button fullWidth loading={loading} size="lg" type="submit">
          {loading ? "Working..." : authMode === "login" ? "Sign in" : "Create account"}
        </Button>
        {authMode === "login" && (
          <Button fullWidth onClick={() => onAuthModeChange("forgot-password")} variant="ghost">
            Forgot your password?
          </Button>
        )}
      </form>
    </div>
  );
}
