package de.tum.devops.vibeshield.report;

import de.tum.devops.vibeshield.generated.model.ScanStatus;

import java.time.OffsetDateTime;
import java.util.List;

/** Complete computed report payload. It is not persisted; it reflects current scan data. */
public record ReportData(
        Long scanId,
        ReportSite site,
        ScanStatus scanStatus,
        OffsetDateTime scanCreatedAt,
        OffsetDateTime scanCompletedAt,
        OffsetDateTime generatedAt,
        LaunchChecklist safeToLaunch,
        ExecutiveSummary executiveSummary,
        List<ReportFinding> findings
) {
}
