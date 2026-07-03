import { FormEvent, useState } from "react";
import { resetPassword } from "../api/client";
import { Alert } from "../components/ui/Alert";
import { Button } from "../components/ui/Button";
import { TextField } from "../components/ui/TextField";

type ResetPasswordPageProps = {
  token: string;
  navigate: (path: string) => void;
};

export function ResetPasswordPage({ token, navigate }: ResetPasswordPageProps) {
  const [newPassword, setNewPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [status, setStatus] = useState<"idle" | "loading" | "done">("idle");
  const [error, setError] = useState("");

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError("");

    if (newPassword !== confirm) {
      setError("Passwords do not match.");
      return;
    }

    setStatus("loading");
    try {
      await resetPassword(token, newPassword);
      setStatus("done");
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to reset password. The link may be expired or already used.");
      setStatus("idle");
    }
  }

  if (status === "done") {
    return (
      <main className="mx-auto flex w-full max-w-md flex-col justify-center py-16">
        <div className="animate-slide-up rounded-2xl border border-line bg-surface p-8 shadow-card">
          <h2 className="text-xl font-semibold text-fg">Password updated</h2>
          <p className="mt-2 text-sm text-muted">Your password has been changed successfully.</p>
          <Button className="mt-5" fullWidth onClick={() => navigate("/login")} size="lg">
            Go to login
          </Button>
        </div>
      </main>
    );
  }

  return (
    <main className="mx-auto flex w-full max-w-md flex-col justify-center py-16">
      <div className="animate-slide-up rounded-2xl border border-line bg-surface p-8 shadow-card">
        <h2 className="text-xl font-semibold text-fg">Set a new password</h2>
        <form className="mt-5 space-y-4" onSubmit={handleSubmit}>
          <TextField autoComplete="new-password" label="New password" onChange={setNewPassword} required type="password" value={newPassword} />
          <TextField autoComplete="new-password" label="Confirm new password" onChange={setConfirm} required type="password" value={confirm} />
          {error && <Alert tone="error">{error}</Alert>}
          <Button fullWidth loading={status === "loading"} size="lg" type="submit">
            {status === "loading" ? "Updating..." : "Update password"}
          </Button>
        </form>
      </div>
    </main>
  );
}
