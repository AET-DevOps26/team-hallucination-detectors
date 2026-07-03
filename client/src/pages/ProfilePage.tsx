import { FormEvent, useState } from "react";
import { Badge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import { Metric } from "../components/ui/Metric";
import { ProfileRow } from "../components/ui/ProfileRow";
import { Analysis, Session, Site, TeamMember } from "../types/domain";

type ProfilePageProps = {
  analyses: Analysis[];
  inviteMember: (email: string) => void;
  members: TeamMember[];
  navigate: (path: string) => void;
  session: Session | null;
  sites: Site[];
};

export function ProfilePage({
  analyses,
  inviteMember,
  members,
  navigate,
  session,
  sites,
}: ProfilePageProps) {
  const [inviteEmail, setInviteEmail] = useState("");
  const totalScans = analyses.length;
  const openFindings = analyses.reduce(
    (count, analysis) =>
      count +
      analysis.findings.filter((finding) => finding.status === "Open").length,
    0,
  );

  function submitInvite(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const nextEmail = inviteEmail.trim();
    if (!nextEmail) return;
    inviteMember(nextEmail);
    setInviteEmail("");
  }

  if (!session) {
    return (
      <main className="rounded-xl border border-line bg-surface p-8 text-center shadow-card">
        <h2 className="text-2xl font-semibold text-fg">Profile unavailable</h2>
        <p className="mt-2 text-muted">Log in to see your profile overview.</p>
        <Button className="mt-5" onClick={() => navigate("/login")}>
          Go to login
        </Button>
      </main>
    );
  }

  return (
    <main className="animate-fade-in space-y-5">
      <section className="rounded-xl border border-line bg-surface p-6 shadow-card">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <h2 className="text-2xl font-semibold text-fg">Profile overview</h2>
            <p className="mt-1 text-muted">
              Account, projects, usage, and team access for {session.username}.
            </p>
          </div>
          <Badge tone="primary">Starter plan</Badge>
        </div>
        <div className="mt-5 grid gap-4 md:grid-cols-4">
          <Metric label="Registered sites" value={String(sites.length)} />
          <Metric label="Analyses" value={String(totalScans)} />
          <Metric label="Open findings" value={String(openFindings)} />
          <Metric label="Scan limit" value={`${totalScans}/100`} />
        </div>
        <div className="mt-5 grid gap-4 lg:grid-cols-2">
          <ProfileRow label="Username" value={session.username} />
          <ProfileRow label="Email" value={session.email ?? "Not set"} />
        </div>
      </section>

      <section className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_360px]">
        <SitesPanel analyses={analyses} navigate={navigate} sites={sites} />
        <TeamPanel
          inviteEmail={inviteEmail}
          members={members}
          setInviteEmail={setInviteEmail}
          submitInvite={submitInvite}
        />
      </section>
    </main>
  );
}

function SitesPanel({
  analyses,
  navigate,
  sites,
}: {
  analyses: Analysis[];
  navigate: (path: string) => void;
  sites: Site[];
}) {
  return (
    <div className="rounded-xl border border-line bg-surface p-6 shadow-card">
      <h2 className="text-xl font-semibold text-fg">Sites</h2>
      {sites.length === 0 ? (
        <p className="mt-4 rounded-lg border border-dashed border-line bg-elevated px-4 py-8 text-center text-sm text-muted">
          No sites registered yet. Run a scan to add one.
        </p>
      ) : (
        <div className="mt-4 overflow-hidden rounded-lg border border-line">
          {sites.map((site) => {
            const siteAnalyses = analyses.filter((analysis) => analysis.siteId === site.id);
            return (
              <div className="grid gap-3 border-b border-line p-4 last:border-b-0 md:grid-cols-[minmax(0,1fr)_130px_150px]" key={site.id}>
                <div className="min-w-0">
                  <p className="font-semibold text-fg">{site.name}</p>
                  <p className="truncate text-sm text-muted">{site.url}</p>
                </div>
                <p className="text-sm text-muted">{siteAnalyses.length} analyses</p>
                <Button onClick={() => navigate("/analysis")} size="sm" variant="secondary">
                  View scans
                </Button>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

function TeamPanel({
  inviteEmail,
  members,
  setInviteEmail,
  submitInvite,
}: {
  inviteEmail: string;
  members: TeamMember[];
  setInviteEmail: (email: string) => void;
  submitInvite: (event: FormEvent<HTMLFormElement>) => void;
}) {
  return (
    <div className="rounded-xl border border-line bg-surface p-6 shadow-card">
      <h2 className="text-xl font-semibold text-fg">Team</h2>
      <form className="mt-4 flex gap-2" onSubmit={submitInvite}>
        <input
          className="min-w-0 flex-1 rounded-lg border border-line bg-surface px-3 py-2 text-sm text-fg outline-none transition placeholder:text-muted focus:border-primary focus:ring-2 focus:ring-primary/20"
          onChange={(event) => setInviteEmail(event.target.value)}
          placeholder="teammate@example.com"
          type="email"
          value={inviteEmail}
        />
        <Button size="sm" type="submit" variant="secondary">
          Invite
        </Button>
      </form>
      <div className="mt-5 space-y-2">
        {members.map((member) => (
          <div className="flex items-center justify-between gap-3 rounded-lg bg-elevated px-3 py-2" key={member.id}>
            <span className="truncate text-sm text-fg">{member.email}</span>
            <Badge size="sm" tone={member.role === "Pending" ? "warn" : "neutral"}>
              {member.role}
            </Badge>
          </div>
        ))}
      </div>
    </div>
  );
}
