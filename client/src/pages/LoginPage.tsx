import { FormEvent } from "react";
import { TextField } from "../components/ui/TextField";
import { AuthMode } from "../types/domain";

type LoginPageProps = {
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
  setUsername: (value: string) => void;
  username: string;
};

export function LoginPage({
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
  setUsername,
  username,
}: LoginPageProps) {
  return (
    <main className="mx-auto grid w-full max-w-5xl gap-6">
      <section className="rounded-md border border-zinc-300 bg-white p-5">
        <h2 className="text-xl font-semibold">Account access</h2>
        <div className="mt-4 grid grid-cols-3 rounded-md bg-zinc-100 p-1">
          {(["login", "register", "reset"] as AuthMode[]).map((mode) => (
            <button
              className={`rounded px-3 py-2 text-sm font-semibold ${
                authMode === mode ? "bg-white text-zinc-950 shadow-sm" : "text-zinc-600"
              }`}
              key={mode}
              onClick={() => onAuthModeChange(mode)}
              type="button"
            >
              {mode === "login" ? "Login" : mode === "register" ? "Register" : "Reset"}
            </button>
          ))}
        </div>

        <form className="mt-5 space-y-4" onSubmit={onAuthSubmit}>
          {authMode !== "reset" && (
            <TextField autoComplete="username" label="Username" onChange={setUsername} required type="text" value={username} />
          )}
          {(authMode === "register" || authMode === "reset") && (
            <TextField autoComplete="email" label="Email" onChange={setEmail} required type="email" value={email} />
          )}
          {authMode !== "reset" && (
            <TextField autoComplete={authMode === "login" ? "current-password" : "new-password"} label="Password" onChange={setPassword} required type="password" value={password} />
          )}
          {authError && <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{authError}</p>}
          {authMessage && <p className="rounded-md bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{authMessage}</p>}
          <button className="w-full rounded-md bg-teal-700 px-4 py-3 font-semibold text-white transition hover:bg-teal-800 disabled:cursor-not-allowed disabled:bg-zinc-400" disabled={authStatus === "loading"} type="submit">
            {authStatus === "loading" ? "Working..." : authMode === "login" ? "Sign in" : authMode === "register" ? "Create account" : "Send reset link"}
          </button>
        </form>
      </section>
    </main>
  );
}
