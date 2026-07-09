import { FormEvent } from "react";
import { AuthForm } from "../components/auth/AuthForm";
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
};

export function LoginPage(props: LoginPageProps) {
  return (
    <main className="mx-auto flex w-full max-w-md flex-col justify-center py-10 sm:py-16">
      <div className="animate-slide-up rounded-2xl border border-line bg-surface p-6 shadow-card sm:p-8">
        <AuthForm {...props} />
      </div>
    </main>
  );
}
