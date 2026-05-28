import { useEffect } from "react";

type LandingRedirectProps = {
  hasSession: boolean;
  navigate: (path: string) => void;
};

export function LandingRedirect({ hasSession, navigate }: LandingRedirectProps) {
  useEffect(() => {
    navigate(hasSession ? "/profile" : "/login");
  }, [hasSession, navigate]);

  return null;
}
