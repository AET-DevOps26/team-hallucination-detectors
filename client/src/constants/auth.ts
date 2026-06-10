import { Session } from "../types/domain";

export const devAuthenticated =
  import.meta.env.DEV && import.meta.env.VITE_DEV_AUTHENTICATED === "true";

export const devSession: Session = {
  username: "dev-user",
  email: "dev-user@localhost",
  token: "dev-session-token",
};
