import { useEffect, useRef, useState } from "react";
import { AuthModal } from "./components/auth/AuthModal";
import { Navbar } from "./components/layout/Navbar";
import { useToast } from "./components/ui/useToast";
import { defaultScanSelection } from "./constants/scans";
import { useAnalysisState } from "./hooks/useAnalysisState";
import { useApiHealth } from "./hooks/useApiHealth";
import { useAppRouter } from "./hooks/useAppRouter";
import { useAuthState } from "./hooks/useAuthState";
import { useTeamState } from "./hooks/useTeamState";
import { useTheme } from "./hooks/useTheme";
import { AnalysisDetailPage } from "./pages/AnalysisDetailPage";
import { AnalysisListPage } from "./pages/AnalysisListPage";
import { HomePage } from "./pages/HomePage";
import { LoginPage } from "./pages/LoginPage";
import { NewAnalysisPage } from "./pages/NewAnalysisPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { ProfilePage } from "./pages/ProfilePage";
import { ResetPasswordPage } from "./pages/ResetPasswordPage";
import { NewAnalysisInput } from "./types/domain";

export default function App() {
  const { theme, toggleTheme } = useTheme();
  const router = useAppRouter();
  const apiState = useApiHealth();
  const auth = useAuthState({ navigate: router.navigate });
  const analysis = useAnalysisState({
    navigate: router.navigate,
    route: router.route,
    enabled: Boolean(auth.session),
  });
  const team = useTeamState();
  const { toast } = useToast();

  const [authModalOpen, setAuthModalOpen] = useState(false);
  // The scan a logged-out user asked for; auto-started once they authenticate.
  const pendingScan = useRef<NewAnalysisInput | null>(null);
  // Refs keep the resume effect keyed only on session without re-running when
  // the (unmemoized) analysis hook or toast identity changes.
  const analysisRef = useRef(analysis);
  analysisRef.current = analysis;
  const toastRef = useRef(toast);
  toastRef.current = toast;
  const prevSession = useRef(auth.session);

  const isAuthenticated = Boolean(auth.session);

  function buildScan(url: string): NewAnalysisInput {
    return {
      url,
      selectedScans: defaultScanSelection,
      crawlDepth: 0,
      includeSubdomains: false,
    };
  }

  async function startScan(url: string) {
    const input = buildScan(url);
    if (auth.session) {
      await analysis.createAnalysis(input); // navigates to the detail page
      return;
    }
    pendingScan.current = input;
    setAuthModalOpen(true);
  }

  // Auto-resume a gated scan the moment the user logs in.
  useEffect(() => {
    const justLoggedIn = !prevSession.current && Boolean(auth.session);
    prevSession.current = auth.session;
    if (justLoggedIn && pendingScan.current) {
      const input = pendingScan.current;
      pendingScan.current = null;
      setAuthModalOpen(false);
      analysisRef.current.createAnalysis(input).catch((error) => {
        toastRef.current(
          error instanceof Error ? error.message : "Could not start the scan.",
          "error",
        );
      });
    }
  }, [auth.session]);

  function requireAuth() {
    pendingScan.current = null;
    setAuthModalOpen(true);
  }

  return (
    <div className="flex min-h-full flex-col bg-bg text-fg">
      <div className="mx-auto flex w-full max-w-7xl flex-1 flex-col px-5 pb-8 sm:px-6 lg:px-8">
        <Navbar
          apiState={apiState}
          navigate={router.navigate}
          onLogout={auth.handleLogout}
          onToggleTheme={toggleTheme}
          route={router.route}
          session={auth.session}
          theme={theme}
        />
        <div className="flex flex-1 flex-col pt-6">
          <AppRoute
            analysis={analysis}
            auth={auth}
            onScan={startScan}
            requireAuth={requireAuth}
            router={router}
            team={team}
          />
        </div>
      </div>

      <AuthModal
        authError={auth.authError}
        authMessage={auth.authMessage}
        authMode={auth.authMode}
        authStatus={auth.authStatus}
        email={auth.email}
        onAuthModeChange={auth.setAuthMode}
        onAuthSubmit={auth.handleAuthSubmit}
        onClose={() => setAuthModalOpen(false)}
        open={authModalOpen && !isAuthenticated}
        password={auth.password}
        pendingUrl={pendingScan.current?.url}
        setEmail={auth.setEmail}
        setPassword={auth.setPassword}
      />
    </div>
  );
}

type AppRouteProps = {
  analysis: ReturnType<typeof useAnalysisState>;
  auth: ReturnType<typeof useAuthState>;
  onScan: (url: string) => Promise<void>;
  requireAuth: () => void;
  router: ReturnType<typeof useAppRouter>;
  team: ReturnType<typeof useTeamState>;
};

function AppRoute({ analysis, auth, onScan, requireAuth, router, team }: AppRouteProps) {
  const { navigate, route } = router;

  if (route === "/reset-password") {
    const token = new URLSearchParams(window.location.search).get("token") ?? "";
    return <ResetPasswordPage navigate={navigate} token={token} />;
  }

  if (route === "/login") {
    return <LoginRoute auth={auth} />;
  }

  if (route === "/") {
    return (
      <HomePage hasSession={Boolean(auth.session)} navigate={navigate} onScan={onScan} />
    );
  }

  // Everything below requires a session: bounce home and open the auth modal.
  if (!auth.session) {
    return <RequireAuthRedirect navigate={navigate} onRequireAuth={requireAuth} />;
  }

  if (route === "/profile") {
    return (
      <ProfilePage
        analyses={analysis.analyses}
        inviteMember={team.inviteMember}
        members={team.members}
        navigate={navigate}
        session={auth.session}
        sites={analysis.sites}
      />
    );
  }
  if (route === "/analysis") {
    return <AnalysisListPage analyses={analysis.analyses} navigate={navigate} />;
  }
  if (route === "/analysis/new") {
    return (
      <NewAnalysisPage
        createAnalysis={analysis.createAnalysis}
        navigate={navigate}
        sites={analysis.sites}
      />
    );
  }
  if (analysis.currentAnalysisId) {
    return (
      <AnalysisDetailPage
        analysis={analysis.currentAnalysis}
        navigate={navigate}
        onRescan={analysis.rescanAnalysis}
        onSelectFinding={analysis.setSelectedFindingId}
        onUpdateFinding={analysis.updateFinding}
        resolutionReason={analysis.resolutionReason}
        selectedFindingId={analysis.selectedFindingId}
        setResolutionReason={analysis.setResolutionReason}
      />
    );
  }

  return <NotFoundPage navigate={navigate} />;
}

function RequireAuthRedirect({
  navigate,
  onRequireAuth,
}: {
  navigate: (path: string) => void;
  onRequireAuth: () => void;
}) {
  useEffect(() => {
    navigate("/");
    onRequireAuth();
  }, [navigate, onRequireAuth]);
  return null;
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
