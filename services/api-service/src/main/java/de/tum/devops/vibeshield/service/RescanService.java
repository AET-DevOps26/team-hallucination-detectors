package de.tum.devops.vibeshield.service;

import de.tum.devops.vibeshield.exception.ConflictException;
import de.tum.devops.vibeshield.exception.NotFoundException;
import de.tum.devops.vibeshield.generated.model.FindingStatus;
import de.tum.devops.vibeshield.generated.model.ScanStatus;
import de.tum.devops.vibeshield.model.Finding;
import de.tum.devops.vibeshield.model.Scan;
import de.tum.devops.vibeshield.repository.FindingRepository;
import de.tum.devops.vibeshield.repository.ScanRepository;
import de.tum.devops.vibeshield.repository.WebsiteRepository;
import de.tum.devops.vibeshield.rescan.ComparisonFinding;
import de.tum.devops.vibeshield.rescan.ScanChangeStatus;
import de.tum.devops.vibeshield.rescan.ScanComparison;
import de.tum.devops.vibeshield.rescan.ScanComparisonSummary;
import de.tum.devops.vibeshield.security.AuthenticatedUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Rescan and current-vs-previous comparison logic for Epic 6. */
@Service
public class RescanService {

    private static final Set<ScanStatus> IN_FLIGHT = Set.of(ScanStatus.PENDING, ScanStatus.RUNNING);

    private final ScanRepository scanRepository;
    private final FindingRepository findingRepository;
    private final WebsiteRepository websiteRepository;

    public RescanService(ScanRepository scanRepository, FindingRepository findingRepository,
                         WebsiteRepository websiteRepository) {
        this.scanRepository = scanRepository;
        this.findingRepository = findingRepository;
        this.websiteRepository = websiteRepository;
    }

    @Transactional
    public Scan rescan(AuthenticatedUser user, Long scanId) {
        Scan original = requireOwnedScan(user, scanId);
        if (scanRepository.existsByWebsiteIdAndStatusIn(original.getWebsiteId(), IN_FLIGHT)) {
            throw scanInProgress();
        }
        try {
            return scanRepository.saveAndFlush(new Scan(
                    original.getWebsiteId(),
                    List.copyOf(original.getRequestedChecks()),
                    original.getCrawlDepth(),
                    original.isIncludeSubdomains(),
                    Instant.now()
            ));
        } catch (DataIntegrityViolationException exception) {
            throw scanInProgress();
        }
    }

    @Transactional(readOnly = true)
    public ScanComparison compareWithPrevious(AuthenticatedUser user, Long scanId) {
        Scan current = requireOwnedScan(user, scanId);
        if (current.getStatus() != ScanStatus.COMPLETED) {
            return emptyComparison(current.getId(), null, false,
                    "Comparison is available after this scan completes.");
        }

        return scanRepository.findFirstByWebsiteIdAndStatusAndIdLessThanOrderByCreatedAtDescIdDesc(
                        current.getWebsiteId(), ScanStatus.COMPLETED, current.getId())
                .map(previous -> compare(current, previous))
                .orElseGet(() -> emptyComparison(current.getId(), null, false,
                        "No previous completed scan exists for this website yet."));
    }

    private ScanComparison compare(Scan current, Scan previous) {
        List<Finding> currentFindings = findingRepository.findAllByScanId(current.getId());
        List<Finding> previousFindings = findingRepository.findAllByScanId(previous.getId());
        Map<String, Finding> currentByFingerprint = currentFindings.stream()
                .collect(Collectors.toMap(this::fingerprint, Function.identity(), this::preferMoreSevere, LinkedHashMap::new));
        Map<String, Finding> previousByFingerprint = previousFindings.stream()
                .collect(Collectors.toMap(this::fingerprint, Function.identity(), this::preferMoreSevere, LinkedHashMap::new));

        List<ComparisonFinding> changed = new ArrayList<>();
        for (Map.Entry<String, Finding> previousEntry : previousByFingerprint.entrySet()) {
            Finding currentFinding = currentByFingerprint.get(previousEntry.getKey());
            if (currentFinding == null) {
                changed.add(toComparison(previousEntry.getValue(), ScanChangeStatus.FIXED, null));
            } else {
                changed.add(toComparison(currentFinding, ScanChangeStatus.STILL_PRESENT, null));
            }
        }
        for (Map.Entry<String, Finding> currentEntry : currentByFingerprint.entrySet()) {
            if (!previousByFingerprint.containsKey(currentEntry.getKey())) {
                changed.add(toComparison(currentEntry.getValue(), ScanChangeStatus.NEWLY_INTRODUCED, null));
            }
        }

        List<ComparisonFinding> actionPlan = orderedActionPlan(currentFindings, previousByFingerprint);

        return new ScanComparison(
                current.getId(),
                previous.getId(),
                true,
                "Compared with the previous completed scan for this website.",
                new ScanComparisonSummary(
                        count(changed, ScanChangeStatus.FIXED),
                        count(changed, ScanChangeStatus.STILL_PRESENT),
                        count(changed, ScanChangeStatus.NEWLY_INTRODUCED)
                ),
                changed.stream()
                        .sorted(Comparator
                                .comparing((ComparisonFinding finding) -> changeRank(finding.changeStatus()))
                                .thenComparingInt(finding -> FindingPrioritization.severityRank(finding.severity()))
                                .thenComparing(ComparisonFinding::title))
                        .toList(),
                actionPlan
        );
    }

    private List<ComparisonFinding> orderedActionPlan(List<Finding> currentFindings,
                                                      Map<String, Finding> previousByFingerprint) {
        List<Finding> ordered = currentFindings.stream()
                .filter(finding -> finding.getStatus() == FindingStatus.OPEN)
                .sorted(findingComparator())
                .toList();
        List<ComparisonFinding> actionPlan = new ArrayList<>();
        int order = 1;
        for (Finding finding : ordered) {
            ScanChangeStatus status = previousByFingerprint.containsKey(fingerprint(finding))
                    ? ScanChangeStatus.STILL_PRESENT
                    : ScanChangeStatus.NEWLY_INTRODUCED;
            actionPlan.add(toComparison(finding, status, order++));
        }
        return List.copyOf(actionPlan);
    }

    private Comparator<Finding> findingComparator() {
        return Comparator
                .comparingInt((Finding finding) -> FindingPrioritization.severityRank(finding.getSeverity()))
                .thenComparingInt(finding -> effortRank(FindingPrioritization.effortFor(finding).level()))
                .thenComparingInt(finding -> FindingPrioritization.checkRank(finding.getCheckType()))
                .thenComparing(Finding::getTitle);
    }

    private int effortRank(String effort) {
        return switch (effort) {
            case "Low" -> 0;
            case "Medium" -> 1;
            case "High" -> 2;
            default -> 3;
        };
    }

    private ComparisonFinding toComparison(Finding finding, ScanChangeStatus status, Integer order) {
        return new ComparisonFinding(
                finding.getId(),
                status,
                finding.getSeverity(),
                finding.getCheckType(),
                finding.getTitle(),
                finding.getAffected(),
                finding.getSuggestedFix(),
                order,
                FindingPrioritization.effortFor(finding)
        );
    }

    private ScanComparison emptyComparison(Long scanId, Long previousScanId, boolean comparable, String message) {
        return new ScanComparison(
                scanId,
                previousScanId,
                comparable,
                message,
                new ScanComparisonSummary(0, 0, 0),
                List.of(),
                List.of()
        );
    }

    private String fingerprint(Finding finding) {
        return normalize(finding.getCheckType().getValue())
                + "|" + normalize(finding.getAffected())
                + "|" + normalize(finding.getTitle());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private Finding preferMoreSevere(Finding first, Finding second) {
        return FindingPrioritization.severityRank(first.getSeverity())
                <= FindingPrioritization.severityRank(second.getSeverity()) ? first : second;
    }

    private int count(List<ComparisonFinding> findings, ScanChangeStatus status) {
        return (int) findings.stream()
                .filter(finding -> finding.changeStatus() == status)
                .count();
    }

    private int changeRank(ScanChangeStatus status) {
        return switch (status) {
            case NEWLY_INTRODUCED -> 0;
            case STILL_PRESENT -> 1;
            case FIXED -> 2;
        };
    }

    private Scan requireOwnedScan(AuthenticatedUser user, Long scanId) {
        Scan scan = scanRepository.findById(scanId)
                .orElseThrow(() -> new NotFoundException("SCAN_NOT_FOUND", "Scan not found."));
        websiteRepository.findByIdAndOwnerId(scan.getWebsiteId(), user.userId())
                .orElseThrow(() -> new NotFoundException("SCAN_NOT_FOUND", "Scan not found."));
        return scan;
    }

    private ConflictException scanInProgress() {
        return new ConflictException("SCAN_IN_PROGRESS",
                "A scan for this website is already pending or running.");
    }
}
