package de.tum.devops.vibeshield.mapper;

import de.tum.devops.vibeshield.model.Finding;
import de.tum.devops.vibeshield.model.Scan;
import de.tum.devops.vibeshield.rescan.ComparisonFinding;
import de.tum.devops.vibeshield.rescan.ScanComparison;
import de.tum.devops.vibeshield.rescan.ScanComparisonSummary;

import java.time.ZoneOffset;

/** Converts scan/finding entities into the response models generated from the contract. */
public final class ScanMapper {

    private ScanMapper() {
    }

    public static de.tum.devops.vibeshield.generated.model.Scan toModel(Scan entity, Integer findingCount) {
        return new de.tum.devops.vibeshield.generated.model.Scan()
                .id(entity.getId())
                .websiteId(entity.getWebsiteId())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC))
                .completedAt(entity.getCompletedAt() == null
                        ? null : entity.getCompletedAt().atOffset(ZoneOffset.UTC))
                .errorMessage(entity.getErrorMessage())
                .findingCount(findingCount);
    }

    public static de.tum.devops.vibeshield.generated.model.Finding toModel(Finding entity) {
        return new de.tum.devops.vibeshield.generated.model.Finding()
                .id(entity.getId())
                .scanId(entity.getScanId())
                .check(entity.getCheckType())
                .title(entity.getTitle())
                .severity(entity.getSeverity())
                .affected(entity.getAffected())
                .explanation(entity.getExplanation())
                .suggestedFix(entity.getSuggestedFix())
                .status(entity.getStatus());
    }

    public static de.tum.devops.vibeshield.generated.model.ScanComparison toModel(ScanComparison comparison) {
        return new de.tum.devops.vibeshield.generated.model.ScanComparison()
                .scanId(comparison.scanId())
                .previousScanId(comparison.previousScanId())
                .comparable(comparison.comparable())
                .message(comparison.message())
                .summary(toModel(comparison.summary()))
                .findings(comparison.findings().stream().map(ScanMapper::toModel).toList())
                .actionPlan(comparison.actionPlan().stream().map(ScanMapper::toModel).toList());
    }

    private static de.tum.devops.vibeshield.generated.model.ScanComparisonSummary toModel(
            ScanComparisonSummary summary) {
        return new de.tum.devops.vibeshield.generated.model.ScanComparisonSummary()
                .fixed(summary.fixed())
                .stillPresent(summary.stillPresent())
                .newlyIntroduced(summary.newlyIntroduced());
    }

    private static de.tum.devops.vibeshield.generated.model.ComparisonFinding toModel(
            ComparisonFinding finding) {
        return new de.tum.devops.vibeshield.generated.model.ComparisonFinding()
                .findingId(finding.findingId())
                .changeStatus(de.tum.devops.vibeshield.generated.model.ScanChangeStatus.fromValue(
                        finding.changeStatus().getValue()))
                .severity(finding.severity())
                .check(finding.check())
                .title(finding.title())
                .affected(finding.affected())
                .suggestedFix(finding.suggestedFix())
                .suggestedFixOrder(finding.suggestedFixOrder())
                .effort(new de.tum.devops.vibeshield.generated.model.EffortEstimate()
                        .level(de.tum.devops.vibeshield.generated.model.EffortEstimate.LevelEnum.fromValue(
                                finding.effort().level()))
                        .estimate(finding.effort().estimate()));
    }
}
