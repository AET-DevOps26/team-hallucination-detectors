import { ScanBox, ScanOverrides } from "../components/analysis/ScanBox";
import { Button } from "../components/ui/Button";
import { defaultScanSelection } from "../constants/scans";
import { NewAnalysisInput, Site } from "../types/domain";

type NewAnalysisPageProps = {
  createAnalysis: (input: NewAnalysisInput) => Promise<void>;
  navigate: (path: string) => void;
  sites: Site[];
};

export function NewAnalysisPage({
  createAnalysis,
  navigate,
  sites,
}: NewAnalysisPageProps) {
  async function handleScan(url: string, overrides?: ScanOverrides) {
    await createAnalysis({
      url,
      selectedScans: overrides?.selectedScans ?? defaultScanSelection,
      crawlDepth: overrides?.crawlDepth ?? 0,
      includeSubdomains: overrides?.includeSubdomains ?? false,
    });
  }

  return (
    <main className="relative -mt-6 flex-1">
      <section className="relative z-10 mx-auto flex max-w-2xl flex-col items-center px-4 pb-24 pt-20 text-center sm:pt-32 animate-fade-in">
        <h2 className="text-2xl font-semibold text-fg sm:text-3xl">
          Create new analysis
        </h2>
        <p className="mt-2 text-muted">
          Enter the target URL and, if you like, tune the checks under advanced options.
        </p>
        <div className="mt-10 w-full text-left">
          <ScanBox
          footer={
            <div className="mt-5">
              <Button
                onClick={() => navigate("/analysis")}
                size="lg"
                type="button"
                variant="secondary"
              >
                Cancel
              </Button>
            </div>
          }
          initialUrl={sites[0]?.url ?? ""}
          onScan={handleScan}
          sites={sites}
          submitLabel="Start analysis"
          />
        </div>
      </section>
    </main>
  );
}
