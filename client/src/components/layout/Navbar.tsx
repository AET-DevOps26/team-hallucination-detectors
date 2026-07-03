import { ApiState, Session } from "../../types/domain";
import { ThemeToggle } from "../ui/ThemeToggle";
import { Theme } from "../../hooks/useTheme";
import { ServiceBadge } from "./ServiceBadge";

type NavbarProps = {
  navigate: (path: string) => void;
  onLogout: () => void;
  route: string;
  session: Session | null;
  theme: Theme;
  onToggleTheme: () => void;
  apiState: ApiState;
};

function Logo({ onClick }: { onClick: () => void }) {
  return (
    <button
      className="group flex items-center gap-2.5 text-lg font-semibold tracking-tight text-fg transition hover:opacity-90"
      onClick={onClick}
      type="button"
    >
      <span className="relative flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-primary to-primary-hover text-primary-fg shadow-sm ring-1 ring-inset ring-white/15 transition group-hover:shadow-glow">
        <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" aria-hidden="true">
          {/* Shield outline */}
          <path
            d="M12 2.5l7 2.6v5.6c0 4.7-3 7.9-7 9.3-4-1.4-7-4.6-7-9.3V5.1l7-2.6z"
            fill="currentColor"
            fillOpacity={0.14}
            stroke="currentColor"
            strokeWidth={1.6}
            strokeLinejoin="round"
          />
          {/* Scan pulse — the "vibe" running through the shield */}
          <path
            d="M8 12.2h2l1.4-3 1.6 5 1.2-2h2"
            stroke="currentColor"
            strokeWidth={1.7}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </span>
      <span>
        Vibe<span className="text-primary">Shield</span>
      </span>
    </button>
  );
}

export function Navbar({
  navigate,
  onLogout,
  session,
  theme,
  onToggleTheme,
  apiState,
}: NavbarProps) {
  return (
    <nav className="sticky top-0 z-30 w-full border-b border-line bg-bg/80 backdrop-blur">
      <div className="mx-auto flex w-full max-w-7xl flex-wrap items-center justify-between gap-3 px-5 py-3 sm:px-6 lg:px-8">
        <div className="flex flex-wrap items-center gap-2 lg:gap-4">
          <Logo onClick={() => navigate("/")} />
        </div>

        <div className="flex items-center gap-2">
          {session && <ServiceBadge compact state={apiState} />}
          <ThemeToggle onToggle={onToggleTheme} theme={theme} />
          {session ? (
            <>
              <span className="hidden text-sm font-semibold text-fg sm:block">
                {session.username}
              </span>
              <button
                className="rounded-lg bg-elevated px-3 py-2 text-sm font-semibold text-fg transition hover:bg-line active:scale-95"
                onClick={onLogout}
                type="button"
              >
                Log out
              </button>
            </>
          ) : (
            <button
              className="rounded-lg border border-line px-4 py-2 text-sm font-semibold text-fg transition hover:bg-elevated active:scale-95"
              onClick={() => navigate("/login")}
              type="button"
            >
              Sign in
            </button>
          )}
        </div>
      </div>
    </nav>
  );
}
