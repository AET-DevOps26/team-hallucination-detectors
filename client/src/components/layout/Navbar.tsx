import { ApiState, Session } from "../../types/domain";
import { NavButton } from "../ui/NavButton";
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
      className="flex items-center gap-2 text-lg font-semibold text-fg transition hover:opacity-80"
      onClick={onClick}
      type="button"
    >
      <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-fg">
        <svg className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth={1.8} viewBox="0 0 24 24">
          <path d="M12 3l7 3v6c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6l7-3z" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </span>
      VibeShield
    </button>
  );
}

export function Navbar({
  navigate,
  onLogout,
  route,
  session,
  theme,
  onToggleTheme,
  apiState,
}: NavbarProps) {
  const analysisRouteActive =
    route === "/analysis" ||
    (route.startsWith("/analysis/") && route !== "/analysis/new");

  return (
    <nav className="sticky top-0 z-30 -mx-5 flex flex-wrap items-center justify-between gap-3 border-b border-line bg-bg/80 px-5 py-3 backdrop-blur sm:-mx-6 sm:px-6 lg:-mx-8 lg:px-8">
      <div className="flex flex-wrap items-center gap-2 lg:gap-4">
        <Logo onClick={() => navigate("/")} />
        {session && (
          <div className="flex flex-wrap gap-1">
            <NavButton active={route === "/profile"} onClick={() => navigate("/profile")}>
              Profile
            </NavButton>
            <NavButton active={analysisRouteActive} onClick={() => navigate("/analysis")}>
              Analysis
            </NavButton>
            <NavButton active={route === "/analysis/new"} onClick={() => navigate("/analysis/new")}>
              New analysis
            </NavButton>
          </div>
        )}
      </div>

      <div className="flex items-center gap-2">
        {session && <ServiceBadge compact state={apiState} />}
        <ThemeToggle onToggle={onToggleTheme} theme={theme} />
        {session ? (
          <>
            <button
              className="hidden rounded-lg border border-line bg-surface px-3 py-2 text-sm font-semibold text-fg transition hover:border-primary hover:text-primary sm:block"
              onClick={() => navigate("/profile")}
              type="button"
            >
              {session.username}
            </button>
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
            className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-fg shadow-sm transition hover:bg-primary-hover hover:shadow-glow active:scale-95"
            onClick={() => navigate("/login")}
            type="button"
          >
            Sign in
          </button>
        )}
      </div>
    </nav>
  );
}
