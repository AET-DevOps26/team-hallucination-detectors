import { FormEvent, useState } from "react";
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
      <main className="rounded-md border border-zinc-300 bg-white p-6">
        <h2 className="text-2xl font-semibold">Profile unavailable</h2>
        <p className="mt-2 text-zinc-600">Log in to see your profile overview.</p>
        <button
          className="mt-5 rounded-md bg-teal-700 px-4 py-3 font-semibold text-white hover:bg-teal-800"
          onClick={() => navigate("/login")}
          type="button"
        >
          Go to login
        </button>
      </main>
    );
  }

  return (
    <main className="space-y-5">
      <section className="rounded-md border border-zinc-300 bg-white p-5">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <h2 className="text-2xl font-semibold">Profile overview</h2>
            <p className="mt-1 text-zinc-600">
              Account, projects, usage, and team access for {session.username}.
            </p>
          </div>
          <span className="rounded-md bg-teal-100 px-3 py-1.5 text-sm font-semibold text-teal-800">
            Starter plan
          </span>
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
    <div className="rounded-md border border-zinc-300 bg-white p-5">
      <h2 className="text-xl font-semibold">Sites</h2>
      <div className="mt-4 overflow-hidden rounded-md border border-zinc-200">
        {sites.map((site) => {
          const siteAnalyses = analyses.filter((analysis) => analysis.siteId === site.id);
          return (
            <div className="grid gap-3 border-b border-zinc-200 p-4 last:border-b-0 md:grid-cols-[minmax(0,1fr)_130px_150px]" key={site.id}>
              <div className="min-w-0">
                <p className="font-semibold">{site.name}</p>
                <p className="truncate text-sm text-zinc-500">{site.url}</p>
              </div>
              <p className="text-sm text-zinc-600">{siteAnalyses.length} analyses</p>
              <button className="rounded-md border border-zinc-300 px-3 py-2 text-sm font-semibold text-zinc-700 hover:border-teal-500" onClick={() => navigate("/analysis")} type="button">
                View scans
              </button>
            </div>
          );
        })}
      </div>
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
    <div className="rounded-md border border-zinc-300 bg-white p-5">
      <h2 className="text-xl font-semibold">Team</h2>
      <form className="mt-4 flex gap-2" onSubmit={submitInvite}>
        <input className="min-w-0 flex-1 rounded-md border border-zinc-300 px-3 py-2 text-sm outline-none focus:border-teal-600 focus:ring-2 focus:ring-teal-100" onChange={(event) => setInviteEmail(event.target.value)} placeholder="teammate@example.com" type="email" value={inviteEmail} />
        <button className="rounded-md bg-zinc-900 px-3 py-2 text-sm font-semibold text-white hover:bg-zinc-700" type="submit">
          Invite
        </button>
      </form>
      <div className="mt-5 space-y-3">
        {members.map((member) => (
          <div className="flex items-center justify-between gap-3 rounded-md bg-zinc-50 px-3 py-2" key={member.id}>
            <span className="truncate text-sm">{member.email}</span>
            <span className="rounded bg-white px-2 py-1 text-xs font-semibold text-zinc-600">
              {member.role}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
