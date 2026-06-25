package de.tum.devops.vibeshield.rescan;

import java.util.List;

/** Computed comparison between a completed scan and the previous completed scan for the same website. */
public record ScanComparison(
        Long scanId,
        Long previousScanId,
        boolean comparable,
        String message,
        ScanComparisonSummary summary,
        List<ComparisonFinding> findings,
        List<ComparisonFinding> actionPlan
) {
}
