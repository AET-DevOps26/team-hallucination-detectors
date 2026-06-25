package de.tum.devops.vibeshield.report;

import java.util.List;

/** Safe-to-launch summary for a completed scan. */
public record LaunchChecklist(String status, int blockingIssues, List<LaunchChecklistItem> items) {
}
