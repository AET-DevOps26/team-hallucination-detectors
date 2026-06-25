package de.tum.devops.vibeshield.report;

import java.util.List;

/** Compact risk and action summary for non-technical sharing. */
public record ExecutiveSummary(
        String riskLevel,
        int totalFindings,
        int openFindings,
        List<ReportNextStep> recommendedNextSteps
) {
}
