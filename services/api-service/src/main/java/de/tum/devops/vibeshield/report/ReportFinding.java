package de.tum.devops.vibeshield.report;

import de.tum.devops.vibeshield.generated.model.FindingStatus;
import de.tum.devops.vibeshield.generated.model.ScanCheck;
import de.tum.devops.vibeshield.generated.model.Severity;

/** Finding enriched with reporting-only prioritization metadata. */
public record ReportFinding(
        Long id,
        Severity severity,
        ScanCheck check,
        String title,
        String affected,
        String explanation,
        String suggestedFix,
        FindingStatus status,
        Integer suggestedFixOrder,
        EffortEstimate effort
) {
}
