import { Navbar } from "./components/layout/Navbar";
import { ServiceBadge } from "./components/layout/ServiceBadge";
import { useAnalysisState } from "./hooks/useAnalysisState";
import { useApiHealth } from "./hooks/useApiHealth";
import { useAppRouter } from "./hooks/useAppRouter";
import { useAuthState } from "./hooks/useAuthState";
import { useTeamState } from "./hooks/useTeamState";
import { AnalysisDetailPage } from "./pages/AnalysisDetailPage";
import { AnalysisListPage } from "./pages/AnalysisListPage";
import { LandingRedirect } from "./pages/LandingRedirect";
import { LoginPage } from "./pages/LoginPage";
import { NewAnalysisPage } from "./pages/NewAnalysisPage";
import { ProfilePage } from "./pages/ProfilePage";
import { ResetPasswordPage } from "./pages/ResetPasswordPage";

export default function App() {
  const router = useAppRouter();
  const apiState = useApiHealth();
  const auth = useAuthState({ navigate: router.navigate });
  const analysis = useAnalysisState({
    navigate: router.navigate,
    route: router.route,
    enabled: Boolean(auth.session),
  });
  const team = useTeamState();
  const isAuthenticated = Boolean(auth.session);

  return (
    <div className="min-h-full bg-zinc-100 text-zinc-950">
      <div className="mx-auto flex min-h-full w-full max-w-7xl flex-col px-5 pb-6 sm:px-6 lg:px-8">
        {isAuthenticated && (
          <>
            <Navbar
              onLogout={auth.handleLogout}
              route={router.route}
              navigate={router.navigate}
              session={auth.session}
            />
            <header className="mb-5 flex flex-col gap-4 border-b border-zinc-300 pb-5 pt-6 lg:flex-row lg:items-end lg:justify-between">
              <div>
                <p className="text-sm font-medium uppercase tracking-wide text-teal-700">
                  VibeShield
                </p>
                <h1 className="mt-2 text-3xl font-semibold">Security console</h1>
              </div>
              <ServiceBadge state={apiState} />
            </header>
          </>
        )}
        <AppRoute
          analysis={analysis}
          auth={auth}
          router={router}
          team={team}
        />
      </div>
    </div>
  );
}

type AppRouteProps = {
  analysis: ReturnType<typeof useAnalysisState>;
  auth: ReturnType<typeof useAuthState>;
  router: ReturnType<typeof useAppRouter>;
  team: ReturnType<typeof useTeamState>;
};

function AppRoute({ analysis, auth, router, team }: AppRouteProps) {
  if (router.route === "/reset-password") {
    const token = new URLSearchParams(window.location.search).get("token") ?? "";
    return <ResetPasswordPage token={token} navigate={router.navigate} />;
  }

  if (!auth.session) {
    return <LoginRoute auth={auth} />;
  }

  if (router.route === "/") {
    return (
      <LandingRedirect
        hasSession={Boolean(auth.session)}
        navigate={router.navigate}
      />
    );
  }
  if (router.route === "/login") return <LoginRoute auth={auth} />;
  if (router.route === "/profile") {
    return (
      <ProfilePage
        analyses={analysis.analyses}
        inviteMember={team.inviteMember}
        members={team.members}
        navigate={router.navigate}
        session={auth.session}
        sites={analysis.sites}
      />
    );
  }
  if (router.route === "/analysis") {
    return (
      <AnalysisListPage
        analyses={analysis.analyses}
        navigate={router.navigate}
      />
    );
  }
  if (router.route === "/analysis/new") {
    return (
      <NewAnalysisPage
        createAnalysis={analysis.createAnalysis}
        navigate={router.navigate}
        sites={analysis.sites}
      />
    );
  }
  if (analysis.currentAnalysisId) {
    return (
      <AnalysisDetailPage
        analysis={analysis.currentAnalysis}
        navigate={router.navigate}
        onSelectFinding={analysis.setSelectedFindingId}
        onUpdateFinding={analysis.updateFinding}
        resolutionReason={analysis.resolutionReason}
        selectedFindingId={analysis.selectedFindingId}
        setResolutionReason={analysis.setResolutionReason}
      />
    );
  }
  return (
    <LandingRedirect
      hasSession={Boolean(auth.session)}
      navigate={router.navigate}
    />
  );
}

function LoginRoute({ auth }: { auth: ReturnType<typeof useAuthState> }) {
  return (
    <LoginPage
      authError={auth.authError}
      authMessage={auth.authMessage}
      authMode={auth.authMode}
      authStatus={auth.authStatus}
      email={auth.email}
      onAuthModeChange={auth.setAuthMode}
      onAuthSubmit={auth.handleAuthSubmit}
      password={auth.password}
      setEmail={auth.setEmail}
      setPassword={auth.setPassword}
    />
  );
}
