import { FormEvent, useMemo, useState } from "react";
import { login, LoginResult } from "../api/client";
import { AuthMode, Session } from "../types/domain";
import { clearSession, maskToken, readStoredSession, storeSession } from "../utils/session";

type UseAuthStateOptions = {
  navigate: (path: string) => void;
};

export function useAuthState({ navigate }: UseAuthStateOptions) {
  const [authMode, setAuthMode] = useState<AuthMode>("login");
  const [session, setSession] = useState<Session | null>(readStoredSession);
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [authMessage, setAuthMessage] = useState("");
  const [authError, setAuthError] = useState("");
  const [authStatus, setAuthStatus] = useState<"idle" | "loading">("idle");
  const maskedToken = useMemo(() => maskToken(session?.token), [session]);

  function persistSession(nextSession: Session) {
    storeSession(nextSession);
    setSession(nextSession);
  }

  async function handleAuthSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setAuthError("");
    setAuthMessage("");
    setAuthStatus("loading");

    try {
      if (authMode === "login") {
        const result: LoginResult = await login({ username, password });
        persistSession({
          username: result.username,
          email: email || `${result.username}@example.com`,
          token: result.token,
        });
      } else if (authMode === "register") {
        persistSession({ username, email });
        setAuthMessage("Account created locally.");
      } else {
        setAuthMessage(`Password reset instructions prepared for ${email}.`);
        return;
      }
      setPassword("");
      navigate("/profile");
    } catch (err: unknown) {
      const error =
        err instanceof Error
          ? err.message
          : "Authentication failed. Check the auth service.";
      setAuthError(error);
    } finally {
      setAuthStatus("idle");
    }
  }

  function handleLogout() {
    clearSession();
    setSession(null);
    setUsername("");
    setEmail("");
    setPassword("");
    setAuthError("");
    setAuthMessage("");
    navigate("/login");
  }

  return {
    authError,
    authMessage,
    authMode,
    authStatus,
    email,
    handleAuthSubmit,
    handleLogout,
    maskedToken,
    password,
    session,
    setAuthMode,
    setEmail,
    setPassword,
    setUsername,
    username,
  };
}
