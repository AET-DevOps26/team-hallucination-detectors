import { ScanBox, ScanOverrides } from "../components/analysis/ScanBox";
import { DitherShader } from "../components/ui/DitherShader";

export type { ScanOverrides } from "../components/analysis/ScanBox";

type HomePageProps = {
  /** Kicks off the scan-intent flow (auth-gates when logged out). */
  onScan: (url: string, overrides?: ScanOverrides) => Promise<void>;
  hasSession: boolean;
  navigate: (path: string) => void;
};

export function HomePage({ onScan, hasSession, navigate }: HomePageProps) {
  return (
    <main className="relative -mt-6 flex-1">
      {/* Ambient dither shader + gradient wash — decorative, self-contained.
          Full-bleed: breaks out of the max-w-7xl page container to span the
          whole viewport width (root clips overflow-x so w-screen can't scroll). */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute left-1/2 top-0 z-0 h-[46rem] w-screen -translate-x-1/2"
      >
        <div className="absolute inset-x-0 top-0 h-[46rem]">
          <DitherShader
            cornerColorLeft="#6366f1"
            cornerColorRight="#ec4899"
            cornerGlow={0.6}
            cornerIntensity={0.45}
            edgeFade={0.15}
            gradientColor1="#0d9488"
            gradientColor2="#38bdf8"
            opacity={0.6}
            patternDensity={2.2}
            pixelSize={3}
            speed={3}
            variant="circle"
          />
        </div>
        <div className="absolute inset-x-0 top-0 h-[46rem] bg-gradient-to-b from-transparent via-transparent to-bg" />
        <div className="absolute left-1/2 top-[-8rem] h-80 w-[36rem] -translate-x-1/2 rounded-full bg-primary/15 blur-3xl" />
      </div>

      <section className="relative z-10 mx-auto flex max-w-3xl flex-col items-center px-4 pb-12 pt-20 text-center sm:pt-32">
        <h1 className="animate-slide-up text-4xl font-semibold tracking-tight text-fg sm:text-6xl">
          Find what your AI builder{" "}
          <span className="bg-gradient-to-r from-primary via-teal-400 to-sky-400 bg-clip-text text-transparent">
            left exposed
          </span>
          .
        </h1>

        <div className="mt-12 w-full max-w-2xl animate-slide-up [animation-delay:120ms]">
          <ScanBox
            autoFocus
            footer={
              hasSession ? (
                <p className="mt-5 text-xs text-muted">
                  <button
                    className="font-semibold text-primary hover:underline"
                    onClick={() => navigate("/analysis")}
                    type="button"
                  >
                    View your past scans →
                  </button>
                </p>
              ) : null
            }
            onScan={onScan}
          />
        </div>
      </section>
    </main>
  );
}
