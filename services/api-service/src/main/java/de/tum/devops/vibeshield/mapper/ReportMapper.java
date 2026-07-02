package de.tum.devops.vibeshield.mapper;

import de.tum.devops.vibeshield.generated.model.EffortEstimate;
import de.tum.devops.vibeshield.generated.model.ExecutiveSummary;
import de.tum.devops.vibeshield.generated.model.LaunchChecklist;
import de.tum.devops.vibeshield.generated.model.LaunchChecklistItem;
import de.tum.devops.vibeshield.generated.model.ReportData;
import de.tum.devops.vibeshield.generated.model.ReportFinding;
import de.tum.devops.vibeshield.generated.model.ReportNextStep;
import de.tum.devops.vibeshield.generated.model.ReportSite;

import java.net.URI;

/** Converts computed report projections into the response models generated from the contract. */
public final class ReportMapper {

    private ReportMapper() {
    }

    public static ReportData toModel(de.tum.devops.vibeshield.report.ReportData report) {
        return new ReportData()
                .scanId(report.scanId())
                .site(toModel(report.site()))
                .scanStatus(report.scanStatus())
                .scanCreatedAt(report.scanCreatedAt())
                .scanCompletedAt(report.scanCompletedAt())
                .generatedAt(report.generatedAt())
                .safeToLaunch(toModel(report.safeToLaunch()))
                .executiveSummary(toModel(report.executiveSummary()))
                .findings(report.findings().stream().map(ReportMapper::toModel).toList());
    }

    private static ReportSite toModel(de.tum.devops.vibeshield.report.ReportSite site) {
        return new ReportSite()
                .id(site.id())
                .name(site.name())
                .url(URI.create(site.url()));
    }

    private static LaunchChecklist toModel(de.tum.devops.vibeshield.report.LaunchChecklist checklist) {
        return new LaunchChecklist()
                .status(LaunchChecklist.StatusEnum.fromValue(checklist.status()))
                .blockingIssues(checklist.blockingIssues())
                .items(checklist.items().stream().map(ReportMapper::toModel).toList());
    }

    private static LaunchChecklistItem toModel(de.tum.devops.vibeshield.report.LaunchChecklistItem item) {
        return new LaunchChecklistItem()
                .label(item.label())
                .result(LaunchChecklistItem.ResultEnum.fromValue(item.result()))
                .checked(item.checked())
                .reason(item.reason());
    }

    private static ExecutiveSummary toModel(de.tum.devops.vibeshield.report.ExecutiveSummary summary) {
        return new ExecutiveSummary()
                .riskLevel(ExecutiveSummary.RiskLevelEnum.fromValue(summary.riskLevel()))
                .totalFindings(summary.totalFindings())
                .openFindings(summary.openFindings())
                .recommendedNextSteps(summary.recommendedNextSteps().stream().map(ReportMapper::toModel).toList());
    }

    private static ReportNextStep toModel(de.tum.devops.vibeshield.report.ReportNextStep step) {
        return new ReportNextStep()
                .order(step.order())
                .title(step.title())
                .severity(step.severity())
                .affected(step.affected())
                .effort(toModel(step.effort()))
                .action(step.action());
    }

    private static ReportFinding toModel(de.tum.devops.vibeshield.report.ReportFinding finding) {
        return new ReportFinding()
                .id(finding.id())
                .severity(finding.severity())
                .check(finding.check())
                .title(finding.title())
                .affected(finding.affected())
                .explanation(finding.explanation())
                .suggestedFix(finding.suggestedFix())
                .status(finding.status())
                .suggestedFixOrder(finding.suggestedFixOrder())
                .effort(toModel(finding.effort()));
    }

    private static EffortEstimate toModel(de.tum.devops.vibeshield.report.EffortEstimate effort) {
        return new EffortEstimate()
                .level(EffortEstimate.LevelEnum.fromValue(effort.level()))
                .estimate(effort.estimate());
    }
}
