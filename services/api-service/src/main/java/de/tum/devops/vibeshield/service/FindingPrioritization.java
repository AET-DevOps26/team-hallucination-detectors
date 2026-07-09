package de.tum.devops.vibeshield.service;

import de.tum.devops.vibeshield.generated.model.ScanCheck;
import de.tum.devops.vibeshield.generated.model.Severity;
import de.tum.devops.vibeshield.model.Finding;
import de.tum.devops.vibeshield.report.EffortEstimate;

/** Shared deterministic prioritization for reports and rescan action plans. */
final class FindingPrioritization {

    private FindingPrioritization() {
    }

    static EffortEstimate effortFor(Finding finding) {
        if (finding.getCheckType() == ScanCheck.SECRETS) {
            return new EffortEstimate("High", "Half day to 1 day");
        }
        if (finding.getCheckType() == ScanCheck.SENSITIVE_FILES || finding.getCheckType() == ScanCheck.ADMIN_PATHS) {
            return new EffortEstimate("Medium", "1-3 hours");
        }
        if (finding.getSeverity() == Severity.CRITICAL || finding.getSeverity() == Severity.HIGH) {
            return new EffortEstimate("Medium", "1-2 hours");
        }
        if (finding.getCheckType() == ScanCheck.HEADERS) {
            return new EffortEstimate("Low", "30-90 minutes");
        }
        return new EffortEstimate("Low", "30-60 minutes");
    }

    static int severityRank(Severity severity) {
        return switch (severity) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
            case INFO -> 4;
        };
    }

    static int checkRank(ScanCheck check) {
        return switch (check) {
            case HTTPS -> 0;
            case HEADERS -> 1;
            case CORS -> 2;
            case SECRETS -> 3;
            case SENSITIVE_FILES -> 4;
            case ADMIN_PATHS -> 5;
            case CRAWL -> 6;
        };
    }
}
