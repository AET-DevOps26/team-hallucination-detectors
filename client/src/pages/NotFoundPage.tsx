import { Button } from "../components/ui/Button";

type NotFoundPageProps = {
  navigate: (path: string) => void;
};

export function NotFoundPage({ navigate }: NotFoundPageProps) {
  return (
    <main className="flex flex-1 items-center justify-center py-16">
      <section className="w-full max-w-md animate-slide-up rounded-2xl border border-line bg-surface p-8 text-center shadow-card">
        <p className="text-sm font-semibold uppercase tracking-wide text-primary">
          Error 404
        </p>
        <h2 className="mt-3 text-3xl font-semibold text-fg">Page not found</h2>
        <p className="mt-3 text-sm text-muted">
          The page you are looking for doesn&apos;t exist or may have been moved.
        </p>
        <Button className="mt-6" onClick={() => navigate("/profile")} size="lg">
          Back to dashboard
        </Button>
      </section>
    </main>
  );
}
