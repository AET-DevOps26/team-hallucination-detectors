import { FormEvent, useEffect, useMemo, useState } from "react";
import { fetchCurrentUser, forgotPassword, loginUser, registerUser } from "../api/client";
import { devAuthenticated } from "../constants/auth";
import { AuthMode, Session } from "../types/domain";
import { clearSession, maskToken, readStoredSession, storeSession } from "../utils/session";

type UseAuthStateOptions = {
  navigate: (path: string) => void;
};

/** Display name shown in the navbar/profile; the backend identifies users by email. */
function usernameFromEmail(email: string): string {
  return email.split("@")[0] || email;
}

export function useAuthState({ navigate }: UseAuthStateOptions) {
  const [authMode, setAuthMode] = useState<AuthMode>("login");
  const [session, setSession] = useState<Session | null>(readStoredSession);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [authMessage, setAuthMessage] = useState("");
  const [authError, setAuthError] = useState("");
  const [authStatus, setAuthStatus] = useState<"idle" | "loading">("idle");
  const maskedToken = useMemo(() => maskToken(session?.token), [session]);

  // Validate the stored token against the auth service on mount; drop stale sessions.
  useEffect(() => {
    if (devAuthenticated) return;

    const stored = readStoredSession();
    if (!stored?.token) return;

    fetchCurrentUser(stored.token).catch(() => {
      clearSession();
      setSession(null);
    });
  }, []);

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
        const response = await loginUser(email, password);
        persistSession({
          username: usernameFromEmail(response.email),
          email: response.email,
          token: response.token,
        });
        setPassword("");
        navigate("/profile");
      } else if (authMode === "register") {
        await registerUser(email, password);
        setAuthMessage("Registration successful. You can now log in.");
        setAuthMode("login");
        setPassword("");
      } else if (authMode === "forgot-password") {
        await forgotPassword(email);
        setAuthMessage("If that email is registered, a reset link has been sent. Check the service logs for the token (email delivery is not yet configured).");
        setAuthMode("login");
      }
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
  };
}
