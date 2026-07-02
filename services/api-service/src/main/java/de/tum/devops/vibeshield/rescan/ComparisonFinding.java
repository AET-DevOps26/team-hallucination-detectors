package de.tum.devops.vibeshield.rescan;

import de.tum.devops.vibeshield.generated.model.ScanCheck;
import de.tum.devops.vibeshield.generated.model.Severity;
import de.tum.devops.vibeshield.report.EffortEstimate;

/** Finding comparison row for current-vs-previous scan review. */
public record ComparisonFinding(
        Long findingId,
        ScanChangeStatus changeStatus,
        Severity severity,
        ScanCheck check,
        String title,
        String affected,
        String suggestedFix,
        Integer suggestedFixOrder,
        EffortEstimate effort
) {
}
