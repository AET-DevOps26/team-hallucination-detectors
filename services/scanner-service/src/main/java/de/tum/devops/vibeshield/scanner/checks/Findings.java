package de.tum.devops.vibeshield.scanner.checks;

import de.tum.devops.vibeshield.scanner.generated.model.ScanCheck;
import de.tum.devops.vibeshield.scanner.generated.model.ScannerFinding;
import de.tum.devops.vibeshield.scanner.generated.model.Severity;

/**
 * Builds contract findings. Explanations and fixes are written for the product's
 * target user — non-technical vibecoders — not for security professionals.
 */
final class Findings {

    private Findings() {
    }

    static ScannerFinding finding(ScanCheck check, String title, Severity severity,
                                  String affected, String explanation, String suggestedFix) {
        return new ScannerFinding()
                .check(check)
                .title(title)
                .severity(severity)
                .affected(affected)
                .explanation(explanation)
                .suggestedFix(suggestedFix);
    }
}
