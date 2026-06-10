import { useState } from "react";
import { initialAnalyses, initialSites } from "../constants/mockData";
import { Analysis, FindingStatus, NewAnalysisInput, Site } from "../types/domain";
import { buildPendingFindings } from "../utils/analysis";

type UseAnalysisStateOptions = {
  navigate: (path: string) => void;
  route: string;
};

export function useAnalysisState({ navigate, route }: UseAnalysisStateOptions) {
  const [sites, setSites] = useState<Site[]>(initialSites);
  const [analyses, setAnalyses] = useState<Analysis[]>(initialAnalyses);
  const [selectedFindingId, setSelectedFindingId] = useState("");
  const [resolutionReason, setResolutionReason] = useState("");
  const currentAnalysisId =
    route !== "/analysis/new" ? route.match(/^\/analysis\/([^/]+)$/)?.[1] : undefined;
  const currentAnalysis = analyses.find((analysis) => analysis.id === currentAnalysisId);

  function createAnalysis(input: NewAnalysisInput) {
    const site = getOrCreateSite(input.url);
    const analysis: Analysis = {
      id: `analysis-${Date.now()}`,
      siteId: site.id,
      siteName: site.name,
      url: site.url,
      createdAt: "May 21, 2026 22:15",
      status: "Pending",
      selectedScans: input.selectedScans,
      crawlDepth: input.crawlDepth,
      includeSubdomains: input.includeSubdomains,
      findings: buildPendingFindings(site.url, input.selectedScans),
    };
    setAnalyses((current) => [analysis, ...current]);
    navigate(`/analysis/${analysis.id}`);
  }

  function getOrCreateSite(url: string) {
    const matchingSite = sites.find((site) => site.url === url);
    if (matchingSite) return matchingSite;
    const site = { id: `site-${Date.now()}`, name: new URL(url).hostname, url };
    setSites((current) => [site, ...current]);
    return site;
  }

  function updateFinding(analysisId: string, findingId: string, status: FindingStatus) {
    setAnalyses((current) =>
      current.map((analysis) =>
        analysis.id !== analysisId
          ? analysis
          : {
              ...analysis,
              findings: analysis.findings.map((finding) =>
                finding.id === findingId
                  ? {
                      ...finding,
                      status,
                      reason: status === "Open" ? undefined : resolutionReason || undefined,
                    }
                  : finding,
              ),
            },
      ),
    );
    setResolutionReason("");
  }

  return {
    analyses,
    createAnalysis,
    currentAnalysis,
    currentAnalysisId,
    resolutionReason,
    selectedFindingId,
    setResolutionReason,
    setSelectedFindingId,
    sites,
    updateFinding,
  };
}
