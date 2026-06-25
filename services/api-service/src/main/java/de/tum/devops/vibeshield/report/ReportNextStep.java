package de.tum.devops.vibeshield.report;

import de.tum.devops.vibeshield.generated.model.Severity;

/** A prioritized action item in the executive summary. */
public record ReportNextStep(
        Integer order,
        String title,
        Severity severity,
        String affected,
        EffortEstimate effort,
        String action
) {
}
