import { FormEvent, useState } from "react";
import { Button } from "../components/ui/Button";
import { validateTargetUrl } from "../utils/url";

type HomePageProps = {
  /** Kicks off the scan-intent flow (auth-gates when logged out). */
  onScan: (url: string) => Promise<void>;
  hasSession: boolean;
  navigate: (path: string) => void;
};

const STEPS = [
  {
    title: "Paste your URL",
    body: "Point VibeShield at any site you shipped with Lovable, Cursor, v0, Bolt, or Replit.",
    icon: "M13.19 8.688a4.5 4.5 0 0 1 1.242 7.244l-4.5 4.5a4.5 4.5 0 0 1-6.364-6.364l1.757-1.757m13.35-.622 1.757-1.757a4.5 4.5 0 0 0-6.364-6.364l-4.5 4.5a4.5 4.5 0 0 0 1.242 7.244",
  },
  {
    title: "We scan the surface",
    body: "HTTPS, security headers, exposed admin paths, leaked keys, and sensitive files — in seconds.",
    icon: "M2.036 12.322a1.012 1.012 0 0 1 0-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178Z",
  },
  {
    title: "Get paste-ready fixes",
    body: "Every finding comes with a prompt you hand back to your AI builder to repair it.",
    icon: "M9.813 15.904 9 18.75l-.813-2.846a4.5 4.5 0 0 0-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 0 0 3.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 0 0 3.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 0 0-3.09 3.09Z",
  },
];

export function HomePage({ onScan, hasSession, navigate }: HomePageProps) {
  const [url, setUrl] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const result = validateTargetUrl(url);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    setError("");
    setSubmitting(true);
    try {
      await onScan(result.url);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not start the scan.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="relative flex-1 overflow-hidden">
      {/* Ambient animated background — decorative, self-contained. */}
      <div aria-hidden="true" className="pointer-events-none absolute inset-0 -z-10">
        <div className="absolute left-1/2 top-[-6rem] h-72 w-72 -translate-x-1/2 rounded-full bg-primary/25 blur-3xl animate-pulse-ring" />
        <div className="absolute bottom-[-4rem] left-[10%] h-56 w-56 rounded-full bg-primary/10 blur-3xl animate-pulse-ring [animation-delay:600ms]" />
        <div className="absolute bottom-[2rem] right-[8%] h-64 w-64 rounded-full bg-sky-400/10 blur-3xl animate-pulse-ring [animation-delay:1200ms]" />
      </div>

      <section className="mx-auto flex max-w-3xl flex-col items-center px-4 pb-16 pt-16 text-center sm:pt-24">
        <span className="mb-6 inline-flex items-center gap-2 rounded-full border border-line bg-surface/70 px-3 py-1 text-xs font-semibold text-muted backdrop-blur animate-fade-in">
          <span className="h-1.5 w-1.5 rounded-full bg-primary" />
          Security scanning for AI-built sites
        </span>

        <h1 className="animate-slide-up text-4xl font-semibold tracking-tight text-fg sm:text-5xl">
          Find what your AI builder{" "}
          <span className="text-primary">left exposed</span>.
        </h1>
        <p className="mt-4 max-w-xl animate-slide-up text-lg text-muted [animation-delay:80ms]">
          Leaked API keys, missing HTTPS, open admin pages, unprotected files.
          VibeShield scans your live site and hands you a fix prompt for each issue.
        </p>

        <form
          className="mt-9 w-full max-w-xl animate-slide-up [animation-delay:160ms]"
          onSubmit={handleSubmit}
        >
          <div className="flex flex-col gap-2 rounded-2xl border border-line bg-surface p-2 shadow-card transition focus-within:border-primary focus-within:shadow-glow sm:flex-row sm:items-center">
            <div className="flex flex-1 items-center gap-2 px-3">
              <svg className="h-5 w-5 shrink-0 text-muted" fill="none" stroke="currentColor" strokeWidth={1.7} viewBox="0 0 24 24">
                <path d="M13.19 8.688a4.5 4.5 0 0 1 1.242 7.244l-4.5 4.5a4.5 4.5 0 0 1-6.364-6.364l1.757-1.757m13.35-.622 1.757-1.757a4.5 4.5 0 0 0-6.364-6.364l-4.5 4.5a4.5 4.5 0 0 0 1.242 7.244" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
              <input
                aria-label="Website URL"
                autoComplete="url"
                autoFocus
                className="w-full bg-transparent py-3 text-base text-fg outline-none placeholder:text-muted"
                onChange={(event) => {
                  setUrl(event.target.value);
                  if (error) setError("");
                }}
                placeholder="https://your-site.com"
                type="text"
                value={url}
              />
            </div>
            <Button loading={submitting} size="lg" type="submit">
              {submitting ? "Starting…" : "Scan my site"}
            </Button>
          </div>
          {error && (
            <p className="mt-3 animate-fade-in text-sm font-medium text-red-600 dark:text-red-400" role="alert">
              {error}
            </p>
          )}
          <p className="mt-3 text-xs text-muted">
            {hasSession ? (
              <button
                className="font-semibold text-primary hover:underline"
                onClick={() => navigate("/analysis/new")}
                type="button"
              >
                Need to choose specific checks? Open advanced options →
              </button>
            ) : (
              "Free scan · You'll sign in before results are saved."
            )}
          </p>
        </form>
      </section>

      <section className="mx-auto max-w-5xl px-4 pb-20">
        <div className="grid gap-4 sm:grid-cols-3">
          {STEPS.map((step, index) => (
            <div
              className="animate-slide-up rounded-xl border border-line bg-surface/70 p-5 backdrop-blur"
              key={step.title}
              style={{ animationDelay: `${240 + index * 80}ms` }}
            >
              <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <svg className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth={1.7} viewBox="0 0 24 24">
                  <path d={step.icon} strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </span>
              <h3 className="mt-4 font-semibold text-fg">{step.title}</h3>
              <p className="mt-1 text-sm text-muted">{step.body}</p>
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}
