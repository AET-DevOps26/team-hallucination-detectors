package de.tum.devops.vibeshield.rescan;

/** Counts used by the comparison UI. */
public record ScanComparisonSummary(int fixed, int stillPresent, int newlyIntroduced) {
}
