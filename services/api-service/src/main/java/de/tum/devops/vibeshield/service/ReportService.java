package de.tum.devops.vibeshield.service;

import de.tum.devops.vibeshield.exception.NotFoundException;
import de.tum.devops.vibeshield.generated.model.FindingStatus;
import de.tum.devops.vibeshield.generated.model.ScanCheck;
import de.tum.devops.vibeshield.generated.model.ScanStatus;
import de.tum.devops.vibeshield.generated.model.Severity;
import de.tum.devops.vibeshield.model.Finding;
import de.tum.devops.vibeshield.model.Scan;
import de.tum.devops.vibeshield.model.Website;
import de.tum.devops.vibeshield.report.EffortEstimate;
import de.tum.devops.vibeshield.report.ExecutiveSummary;
import de.tum.devops.vibeshield.report.LaunchChecklist;
import de.tum.devops.vibeshield.report.LaunchChecklistItem;
import de.tum.devops.vibeshield.report.ReportData;
import de.tum.devops.vibeshield.report.ReportFinding;
import de.tum.devops.vibeshield.report.ReportNextStep;
import de.tum.devops.vibeshield.report.ReportSite;
import de.tum.devops.vibeshield.repository.FindingRepository;
import de.tum.devops.vibeshield.repository.ScanRepository;
import de.tum.devops.vibeshield.repository.WebsiteRepository;
import de.tum.devops.vibeshield.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Builds read-only report projections from current scan, website, and finding state. */
@Service
public class ReportService {

    private final ScanRepository scanRepository;
    private final WebsiteRepository websiteRepository;
    private final FindingRepository findingRepository;

    public ReportService(ScanRepository scanRepository, WebsiteRepository websiteRepository,
                         FindingRepository findingRepository) {
        this.scanRepository = scanRepository;
        this.websiteRepository = websiteRepository;
        this.findingRepository = findingRepository;
    }

    @Transactional(readOnly = true)
    public ReportData buildReport(AuthenticatedUser user, Long scanId) {
        Scan scan = scanRepository.findById(scanId)
                .orElseThrow(() -> new NotFoundException("SCAN_NOT_FOUND", "Scan not found."));
        Website website = websiteRepository.findByIdAndOwnerId(scan.getWebsiteId(), user.userId())
                .orElseThrow(() -> new NotFoundException("SCAN_NOT_FOUND", "Scan not found."));
        List<ReportFinding> findings = orderedFindings(findingRepository.findAllByScanId(scanId));
        LaunchChecklist checklist = checklist(scan, findings);
        ExecutiveSummary summary = summary(findings);

        return new ReportData(
                scan.getId(),
                new ReportSite(website.getId(), website.getName(), website.getUrl()),
                scan.getStatus(),
                scan.getCreatedAt().atOffset(ZoneOffset.UTC),
                scan.getCompletedAt() == null ? null : scan.getCompletedAt().atOffset(ZoneOffset.UTC),
                Instant.now().atOffset(ZoneOffset.UTC),
                checklist,
                summary,
                findings
        );
    }

    private List<ReportFinding> orderedFindings(List<Finding> findings) {
        List<Finding> ordered = findings.stream()
                .sorted(Comparator
                        .comparing((Finding finding) -> finding.getStatus() == FindingStatus.OPEN ? 0 : 1)
                        .thenComparingInt(finding -> FindingPrioritization.severityRank(finding.getSeverity()))
                        .thenComparingInt(finding -> FindingPrioritization.checkRank(finding.getCheckType()))
                        .thenComparing(Finding::getId))
                .toList();

        int order = 1;
        java.util.ArrayList<ReportFinding> reportFindings = new java.util.ArrayList<>();
        for (Finding finding : ordered) {
            Integer suggestedFixOrder = finding.getStatus() == FindingStatus.OPEN ? order++ : null;
            reportFindings.add(new ReportFinding(
                    finding.getId(),
                    finding.getSeverity(),
                    finding.getCheckType(),
                    finding.getTitle(),
                    finding.getAffected(),
                    finding.getExplanation(),
                    finding.getSuggestedFix(),
                    finding.getStatus(),
                    suggestedFixOrder,
                    FindingPrioritization.effortFor(finding)
            ));
        }
        return List.copyOf(reportFindings);
    }

    private ExecutiveSummary summary(List<ReportFinding> findings) {
        List<ReportFinding> openFindings = findings.stream()
                .filter(finding -> finding.status() == FindingStatus.OPEN)
                .toList();
        List<ReportNextStep> nextSteps = openFindings.stream()
                .limit(5)
                .map(finding -> new ReportNextStep(
                        finding.suggestedFixOrder(),
                        finding.title(),
                        finding.severity(),
                        finding.affected(),
                        finding.effort(),
                        finding.suggestedFix()
                ))
                .toList();
        return new ExecutiveSummary(riskLevel(openFindings), findings.size(), openFindings.size(), nextSteps);
    }

    private LaunchChecklist checklist(Scan scan, List<ReportFinding> findings) {
        List<ReportFinding> open = findings.stream()
                .filter(finding -> finding.status() == FindingStatus.OPEN)
                .toList();
        long critical = open.stream().filter(finding -> finding.severity() == Severity.CRITICAL).count();
        long high = open.stream().filter(finding -> finding.severity() == Severity.HIGH).count();
        boolean completed = scan.getStatus() == ScanStatus.COMPLETED;

        List<LaunchChecklistItem> items = new ArrayList<>();
        items.add(new LaunchChecklistItem("Scan execution", completed ? "Pass" : "Incomplete", true,
                completed ? "The scan completed and findings are available."
                        : "The scan has not completed yet, so launch readiness is not final."));
        for (ScanCheck check : List.of(
                ScanCheck.CRAWL,
                ScanCheck.HTTPS,
                ScanCheck.HEADERS,
                ScanCheck.ADMIN_PATHS,
                ScanCheck.SECRETS,
                ScanCheck.SENSITIVE_FILES
        )) {
            items.add(checklistItemFor(check, scan.getRequestedChecks().contains(check), open));
        }

        int blockingIssues = (int) items.stream()
                .filter(item -> item.checked() && "Needs attention".equals(item.result()))
                .count();
        return new LaunchChecklist(statusFor(blockingIssues, critical, high, completed), blockingIssues, items);
    }

    private LaunchChecklistItem checklistItemFor(ScanCheck check, boolean selected, List<ReportFinding> openFindings) {
        String label = checkLabel(check);
        if (!selected) {
            return new LaunchChecklistItem(label, "Not selected", false,
                    "This check was not selected for this scan, so no launch decision was made for it.");
        }

        List<ReportFinding> matching = openFindings.stream()
                .filter(finding -> finding.check() == check)
                .toList();
        if (matching.isEmpty()) {
            return new LaunchChecklistItem(label, "Pass", true,
                    "Checked and no open findings were detected for this category.");
        }

        String highestSeverity = matching.stream()
                .map(ReportFinding::severity)
                .min(Comparator.comparingInt(FindingPrioritization::severityRank))
                .map(Severity::getValue)
                .orElse("Open");
        return new LaunchChecklistItem(label, "Needs attention", true,
                "Checked and found " + matching.size() + " open " + plural("finding", matching.size())
                        + "; highest severity is " + highestSeverity + ".");
    }

    private String checkLabel(ScanCheck check) {
        return switch (check) {
            case CRAWL -> "Page, form, and endpoint discovery";
            case HTTPS -> "HTTPS and mixed-content checks";
            case HEADERS -> "Security header checks";
            case ADMIN_PATHS -> "Public admin and login paths";
            case SECRETS -> "Client bundle secrets";
            case SENSITIVE_FILES -> "Sensitive files and backups";
            case COOKIES -> "Cookie security attributes";
            case CORS -> "Cross-origin resource sharing policy";
        };
    }

    private String plural(String word, int count) {
        return count == 1 ? word : word + "s";
    }

    private String statusFor(int blockingIssues, long critical, long high, boolean completed) {
        if (!completed || critical > 0) {
            return "Not safe to launch";
        }
        if (high > 0 || blockingIssues > 0) {
            return "Needs attention";
        }
        if (blockingIssues > 0) {
            return "Safe with warnings";
        }
        return "Safe to launch";
    }

    private String riskLevel(List<ReportFinding> openFindings) {
        if (openFindings.stream().anyMatch(finding -> finding.severity() == Severity.CRITICAL)) {
            return "Critical";
        }
        if (openFindings.stream().anyMatch(finding -> finding.severity() == Severity.HIGH)) {
            return "High";
        }
        if (openFindings.stream().anyMatch(finding -> finding.severity() == Severity.MEDIUM)) {
            return "Medium";
        }
        if (openFindings.stream().anyMatch(finding -> finding.severity() == Severity.LOW)) {
            return "Low";
        }
        return "Low";
    }

}
