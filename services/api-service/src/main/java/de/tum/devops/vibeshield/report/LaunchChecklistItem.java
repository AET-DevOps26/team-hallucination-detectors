package de.tum.devops.vibeshield.report;

/** One go-live readiness check derived from selected scan checks and open findings. */
public record LaunchChecklistItem(String label, String result, boolean checked, String reason) {
}
