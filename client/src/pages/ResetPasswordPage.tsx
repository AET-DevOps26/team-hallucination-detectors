import { FormEvent, useState } from "react";
import { resetPassword } from "../api/client";
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
      <main className="mx-auto grid w-full max-w-5xl gap-6">
        <section className="rounded-md border border-zinc-300 bg-white p-5">
          <h2 className="text-xl font-semibold">Password updated</h2>
          <p className="mt-2 text-sm text-zinc-600">Your password has been changed successfully.</p>
          <button
            className="mt-5 w-full rounded-md bg-teal-700 px-4 py-3 font-semibold text-white transition hover:bg-teal-800"
            onClick={() => navigate("/login")}
            type="button"
          >
            Go to login
          </button>
        </section>
      </main>
    );
  }

  return (
    <main className="mx-auto grid w-full max-w-5xl gap-6">
      <section className="rounded-md border border-zinc-300 bg-white p-5">
        <h2 className="text-xl font-semibold">Set a new password</h2>

        <form className="mt-5 space-y-4" onSubmit={handleSubmit}>
          <TextField autoComplete="new-password" label="New password" onChange={setNewPassword} required type="password" value={newPassword} />
          <TextField autoComplete="new-password" label="Confirm new password" onChange={setConfirm} required type="password" value={confirm} />
          {error && <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
          <button
            className="w-full rounded-md bg-teal-700 px-4 py-3 font-semibold text-white transition hover:bg-teal-800 disabled:cursor-not-allowed disabled:bg-zinc-400"
            disabled={status === "loading"}
            type="submit"
          >
            {status === "loading" ? "Updating..." : "Update password"}
          </button>
        </form>
      </section>
    </main>
  );
}
