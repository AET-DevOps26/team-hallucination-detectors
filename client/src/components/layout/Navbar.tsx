import { Session } from "../../types/domain";
import { LLM_PROVIDERS, useLlmProvider } from "../../hooks/useLlmProvider";
import { NavButton } from "../ui/NavButton";

type NavbarProps = {
  navigate: (path: string) => void;
  onLogout: () => void;
  route: string;
  session: Session | null;
};

export function Navbar({ navigate, onLogout, route, session }: NavbarProps) {
  const { provider, setProvider } = useLlmProvider();
  const analysisRouteActive =
    route === "/analysis" ||
    (route.startsWith("/analysis/") && route !== "/analysis/new");

  return (
    <nav className="sticky top-0 z-20 -mx-5 flex flex-col gap-3 border-b border-zinc-300 bg-white/95 px-5 py-3 shadow-sm backdrop-blur sm:-mx-6 sm:px-6 lg:-mx-8 lg:flex-row lg:items-center lg:justify-between lg:px-8">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
        <button
          className="text-left text-lg font-semibold text-zinc-950"
          onClick={() => navigate(session ? "/profile" : "/login")}
          type="button"
        >
          VibeShield
        </button>
        <div className="flex flex-wrap gap-2">
          <NavButton active={route === "/profile"} onClick={() => navigate("/profile")}>
            Profile
          </NavButton>
          <NavButton active={analysisRouteActive} onClick={() => navigate("/analysis")}>
            Analysis
          </NavButton>
          <NavButton
            active={route === "/analysis/new"}
            onClick={() => navigate("/analysis/new")}
          >
            New analysis
          </NavButton>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <div
          aria-label="AI provider"
          className="flex items-center gap-0.5 rounded-md border border-zinc-300 p-0.5"
          role="group"
        >
          <span className="px-1.5 text-xs font-semibold text-zinc-400">AI</span>
          {LLM_PROVIDERS.map((p) => (
            <button
              aria-pressed={provider === p.value}
              className={`rounded px-2.5 py-1.5 text-xs font-semibold transition-colors ${
                provider === p.value
                  ? "bg-teal-700 text-white"
                  : "text-zinc-600 hover:text-zinc-900"
              }`}
              key={p.value}
              onClick={() => setProvider(p.value)}
              title={`Use ${p.label} for AI generation`}
              type="button"
            >
              {p.label}
            </button>
          ))}
        </div>
        {session ? (
          <>
            <button
              className="rounded-md border border-zinc-300 px-3 py-2 text-sm font-semibold text-zinc-700 hover:border-teal-500"
              onClick={() => navigate("/profile")}
              type="button"
            >
              {session.username}
            </button>
            <button
              className="rounded-md bg-zinc-900 px-3 py-2 text-sm font-semibold text-white hover:bg-zinc-700"
              onClick={onLogout}
              type="button"
            >
              Log out
            </button>
          </>
        ) : (
          <NavButton active={route === "/login"} onClick={() => navigate("/login")}>
            Login
          </NavButton>
        )}
      </div>
    </nav>
  );
}
