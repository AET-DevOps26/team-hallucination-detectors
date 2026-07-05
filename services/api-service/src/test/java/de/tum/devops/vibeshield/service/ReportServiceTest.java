package de.tum.devops.vibeshield.service;

import de.tum.devops.vibeshield.exception.NotFoundException;
import de.tum.devops.vibeshield.generated.model.FindingStatus;
import de.tum.devops.vibeshield.generated.model.ScanCheck;
import de.tum.devops.vibeshield.generated.model.Severity;
import de.tum.devops.vibeshield.model.Finding;
import de.tum.devops.vibeshield.model.Scan;
import de.tum.devops.vibeshield.model.Website;
import de.tum.devops.vibeshield.report.LaunchChecklistItem;
import de.tum.devops.vibeshield.report.ReportData;
import de.tum.devops.vibeshield.report.ReportFinding;
import de.tum.devops.vibeshield.repository.FindingRepository;
import de.tum.devops.vibeshield.repository.ScanRepository;
import de.tum.devops.vibeshield.repository.WebsiteRepository;
import de.tum.devops.vibeshield.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** Unit tests for report assembly: ownership guards, finding ordering, executive summary, launch checklist. */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final AuthenticatedUser USER = new AuthenticatedUser(7L, "dev@vibeshield.dev");

    @Mock
    private ScanRepository scanRepository;

    @Mock
    private WebsiteRepository websiteRepository;

    @Mock
    private FindingRepository findingRepository;

    private ReportService service() {
        return new ReportService(scanRepository, websiteRepository, findingRepository);
    }

    private Website website(long id) {
        Website website = new Website(USER.userId(), "https://shop.example.org", "Shop", Instant.now());
        ReflectionTestUtils.setField(website, "id", id);
        return website;
    }

    private Scan scan(long id, List<ScanCheck> requestedChecks) {
        Scan scan = new Scan(1L, requestedChecks, 2, false, Instant.now());
        ReflectionTestUtils.setField(scan, "id", id);
        return scan;
    }

    private Finding finding(long id, long scanId, ScanCheck check, Severity severity, String title) {
        Finding finding = new Finding(scanId, check, title, severity, "https://shop.example.org/" + id,
                "explanation", "suggested fix " + id);
        ReflectionTestUtils.setField(finding, "id", id);
        return finding;
    }

    private void markFixed(Finding finding) {
        ReflectionTestUtils.setField(finding, "status", FindingStatus.FIXED);
    }

    // ── ownership / not-found ───────────────────────────────────────────────────

    @Test
    void buildReport_throwsNotFound_whenScanDoesNotExist() {
        when(scanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().buildReport(USER, 99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void buildReport_throwsNotFound_whenWebsiteNotOwnedByCaller() {
        Scan scan = scan(1L, List.of(ScanCheck.HTTPS));
        when(scanRepository.findById(1L)).thenReturn(Optional.of(scan));
        when(websiteRepository.findByIdAndOwnerId(1L, USER.userId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().buildReport(USER, 1L))
                .isInstanceOf(NotFoundException.class);
    }

    // ── finding ordering ────────────────────────────────────────────────────────

    @Test
    void buildReport_ordersOpenFindingsBeforeFixedRegardlessOfSeverity() {
        Scan scan = scan(1L, List.of(ScanCheck.HTTPS));
        scan.markCompleted(Instant.now());
        Finding fixedCritical = finding(1L, 1L, ScanCheck.HTTPS, Severity.CRITICAL, "Fixed critical");
        markFixed(fixedCritical);
        Finding openLow = finding(2L, 1L, ScanCheck.HTTPS, Severity.LOW, "Open low");
        when(scanRepository.findById(1L)).thenReturn(Optional.of(scan));
        when(websiteRepository.findByIdAndOwnerId(1L, USER.userId())).thenReturn(Optional.of(website(1L)));
        when(findingRepository.findAllByScanId(1L)).thenReturn(List.of(fixedCritical, openLow));

        List<ReportFinding> findings = service().buildReport(USER, 1L).findings();

        assertThat(findings).extracting(ReportFinding::id).containsExactly(2L, 1L);
    }

    @Test
    void buildReport_ordersBySeverityThenCheckThenId_withinTheSameStatus() {
        Scan scan = scan(1L, List.of(ScanCheck.HTTPS, ScanCheck.HEADERS));
        Finding lowHttps = finding(3L, 1L, ScanCheck.HTTPS, Severity.LOW, "Low HTTPS");
        Finding highHeaders = finding(1L, 1L, ScanCheck.HEADERS, Severity.HIGH, "High headers");
        Finding highHttps = finding(2L, 1L, ScanCheck.HTTPS, Severity.HIGH, "High HTTPS");
        when(scanRepository.findById(1L)).thenReturn(Optional.of(scan));
        when(websiteRepository.findByIdAndOwnerId(1L, USER.userId())).thenReturn(Optional.of(website(1L)));
        // Deliberately out of expected order to prove the comparator, not insertion order, wins.
        when(findingRepository.findAllByScanId(1L)).thenReturn(List.of(lowHttps, highHeaders, highHttps));

        List<ReportFinding> findings = service().buildReport(USER, 1L).findings();

        // High severity before low; among the two High findings, HTTPS (checkRank 0) before Headers (checkRank 1).
        assertThat(findings).extracting(ReportFinding::id).containsExactly(2L, 1L, 3L);
    }

    @Test
    void buildReport_assignsSuggestedFixOrderOnlyToOpenFindings() {
        Scan scan = scan(1L, List.of(ScanCheck.HTTPS));
        Finding open1 = finding(1L, 1L, ScanCheck.HTTPS, Severity.HIGH, "Open 1");
        Finding open2 = finding(2L, 1L, ScanCheck.HTTPS, Severity.LOW, "Open 2");
        Finding fixed = finding(3L, 1L, ScanCheck.HTTPS, Severity.CRITICAL, "Fixed");
        markFixed(fixed);
        when(scanRepository.findById(1L)).thenReturn(Optional.of(scan));
        when(websiteRepository.findByIdAndOwnerId(1L, USER.userId())).thenReturn(Optional.of(website(1L)));
        when(findingRepository.findAllByScanId(1L)).thenReturn(List.of(open1, open2, fixed));

        List<ReportFinding> findings = service().buildReport(USER, 1L).findings();

        assertThat(findings).extracting(ReportFinding::id, ReportFinding::suggestedFixOrder)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, 1),
                        org.assertj.core.groups.Tuple.tuple(2L, 2),
                        org.assertj.core.groups.Tuple.tuple(3L, null));
    }

    @Test
    void buildReport_mapsSecretsFindingsToHighEffort() {
        Scan scan = scan(1L, List.of(ScanCheck.SECRETS));
        Finding secretsFinding = finding(1L, 1L, ScanCheck.SECRETS, Severity.MEDIUM, "Exposed API key");
        when(scanRepository.findById(1L)).thenReturn(Optional.of(scan));
        when(websiteRepository.findByIdAndOwnerId(1L, USER.userId())).thenReturn(Optional.of(website(1L)));
        when(findingRepository.findAllByScanId(1L)).thenReturn(List.of(secretsFinding));

        ReportFinding reportFinding = service().buildReport(USER, 1L).findings().get(0);

        assertThat(reportFinding.effort().level()).isEqualTo("High");
    }

    // ── executive summary ───────────────────────────────────────────────────────

    @Test
    void buildReport_riskLevelReflectsHighestSeverityAmongOpenFindingsOnly() {
        Scan scan = scan(1L, List.of(ScanCheck.HTTPS));
        Finding fixedCritical = finding(1L, 1L, ScanCheck.HTTPS, Severity.CRITICAL, "Fixed critical");
        markFixed(fixedCritical);
        Finding openMedium = finding(2L, 1L, ScanCheck.HTTPS, Severity.MEDIUM, "Open medium");
        when(scanRepository.findById(1L)).thenReturn(Optional.of(scan));
        when(websiteRepository.findByIdAndOwnerId(1L, USER.userId())).thenReturn(Optional.of(website(1L)));
        when(findingRepository.findAllByScanId(1L)).thenReturn(List.of(fixedCritical, openMedium));

        ReportData report = service().buildReport(USER, 1L);

        assertThat(report.executiveSummary().riskLevel()).isEqualTo("Medium");
        assertThat(report.executiveSummary().totalFindings()).isEqualTo(2);
        assertThat(report.executiveSummary().openFindings()).isEqualTo(1);
    }

    @Test
    void buildReport_recommendedNextStepsCapAtFiveOpenFindingsInPriorityOrder() {
        Scan scan = scan(1L, List.of(ScanCheck.HTTPS));
        List<Finding> sixOpenFindings = new java.util.ArrayList<>();
        for (long i = 1; i <= 6; i++) {
            sixOpenFindings.add(finding(i, 1L, ScanCheck.HTTPS, Severity.LOW, "Finding " + i));
        }
        when(scanRepository.findById(1L)).thenReturn(Optional.of(scan));
        when(websiteRepository.findByIdAndOwnerId(1L, USER.userId())).thenReturn(Optional.of(website(1L)));
        when(findingRepository.findAllByScanId(1L)).thenReturn(sixOpenFindings);

        ReportData report = service().buildReport(USER, 1L);

        assertThat(report.executiveSummary().recommendedNextSteps()).hasSize(5);
        assertThat(report.executiveSummary().recommendedNextSteps().get(0).order()).isEqualTo(1);
    }

    // ── launch checklist ────────────────────────────────────────────────────────

    @Test
    void buildReport_checklistMarksUnselectedChecksAsNotSelected() {
        Scan scan = scan(1L, List.of(ScanCheck.HTTPS));
        scan.markCompleted(Instant.now());
        when(scanRepository.findById(1L)).thenReturn(Optional.of(scan));
        when(websiteRepository.findByIdAndOwnerId(1L, USER.userId())).thenReturn(Optional.of(website(1L)));
        when(findingRepository.findAllByScanId(1L)).thenReturn(List.of());

        List<LaunchChecklistItem> items = service().buildReport(USER, 1L).safeToLaunch().items();

        LaunchChecklistItem headersItem = items.stream()
                .filter(item -> item.label().equals("Security header checks"))
                .findFirst().orElseThrow();
        assertThat(headersItem.result()).isEqualTo("Not selected");
        assertThat(headersItem.checked()).isFalse();
    }

    @Test
    void buildReport_checklistStatusIsSafeToLaunch_whenCompletedWithNoOpenFindings() {
        Scan scan = scan(1L, List.of(ScanCheck.HTTPS));
        scan.markCompleted(Instant.now());
        when(scanRepository.findById(1L)).thenReturn(Optional.of(scan));
        when(websiteRepository.findByIdAndOwnerId(1L, USER.userId())).thenReturn(Optional.of(website(1L)));
        when(findingRepository.findAllByScanId(1L)).thenReturn(List.of());

        ReportData report = service().buildReport(USER, 1L);

        assertThat(report.safeToLaunch().status()).isEqualTo("Safe to launch");
        assertThat(report.safeToLaunch().blockingIssues()).isZero();
    }

    @Test
    void buildReport_checklistStatusIsNotSafeToLaunch_whenScanNotCompleted() {
        Scan scan = scan(1L, List.of(ScanCheck.HTTPS)); // left Pending
        when(scanRepository.findById(1L)).thenReturn(Optional.of(scan));
        when(websiteRepository.findByIdAndOwnerId(1L, USER.userId())).thenReturn(Optional.of(website(1L)));
        when(findingRepository.findAllByScanId(1L)).thenReturn(List.of());

        ReportData report = service().buildReport(USER, 1L);

        assertThat(report.safeToLaunch().status()).isEqualTo("Not safe to launch");
    }

    @Test
    void buildReport_checklistStatusIsNotSafeToLaunch_whenAnyOpenCriticalFindingExists() {
        Scan scan = scan(1L, List.of(ScanCheck.HTTPS));
        scan.markCompleted(Instant.now());
        Finding critical = finding(1L, 1L, ScanCheck.HTTPS, Severity.CRITICAL, "Critical finding");
        when(scanRepository.findById(1L)).thenReturn(Optional.of(scan));
        when(websiteRepository.findByIdAndOwnerId(1L, USER.userId())).thenReturn(Optional.of(website(1L)));
        when(findingRepository.findAllByScanId(1L)).thenReturn(List.of(critical));

        ReportData report = service().buildReport(USER, 1L);

        assertThat(report.safeToLaunch().status()).isEqualTo("Not safe to launch");
    }

    @Test
    void buildReport_checklistStatusIsNeedsAttention_whenOpenFindingBlocksASelectedCheck() {
        Scan scan = scan(1L, List.of(ScanCheck.HTTPS));
        scan.markCompleted(Instant.now());
        Finding lowFinding = finding(1L, 1L, ScanCheck.HTTPS, Severity.LOW, "Minor HTTPS issue");
        when(scanRepository.findById(1L)).thenReturn(Optional.of(scan));
        when(websiteRepository.findByIdAndOwnerId(1L, USER.userId())).thenReturn(Optional.of(website(1L)));
        when(findingRepository.findAllByScanId(1L)).thenReturn(List.of(lowFinding));

        ReportData report = service().buildReport(USER, 1L);

        assertThat(report.safeToLaunch().status()).isEqualTo("Needs attention");
        assertThat(report.safeToLaunch().blockingIssues()).isEqualTo(1);
        LaunchChecklistItem httpsItem = report.safeToLaunch().items().stream()
                .filter(item -> item.label().equals("HTTPS and mixed-content checks"))
                .findFirst().orElseThrow();
        assertThat(httpsItem.result()).isEqualTo("Needs attention");
        assertThat(httpsItem.reason()).contains("Low");
    }
}
