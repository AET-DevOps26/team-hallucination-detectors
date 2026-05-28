import { sessionStorageKey } from "../constants/session";
import { Session } from "../types/domain";
import { devAuthenticated, devSession } from "../constants/auth";

export function readStoredSession(): Session | null {
  if (devAuthenticated) return devSession;

  const stored = window.localStorage.getItem(sessionStorageKey);
  if (!stored) return null;

  try {
    const session = JSON.parse(stored) as Session;
    return session.username ? session : null;
  } catch {
    window.localStorage.removeItem(sessionStorageKey);
    return null;
  }
}

export function storeSession(session: Session) {
  window.localStorage.setItem(sessionStorageKey, JSON.stringify(session));
}

export function clearSession() {
  window.localStorage.removeItem(sessionStorageKey);
}

export function maskToken(token?: string) {
  if (!token) return "Token not returned";
  if (token.length <= 12) return token;
  return `${token.slice(0, 6)}...${token.slice(-6)}`;
}
