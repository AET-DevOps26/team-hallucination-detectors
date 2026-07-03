package de.tum.devops.vibeshield.service;

import de.tum.devops.vibeshield.exception.ConflictException;
import de.tum.devops.vibeshield.exception.NotFoundException;
import de.tum.devops.vibeshield.generated.model.FindingStatus;
import de.tum.devops.vibeshield.generated.model.ScanCheck;
import de.tum.devops.vibeshield.generated.model.ScanStatus;
import de.tum.devops.vibeshield.generated.model.Severity;
import de.tum.devops.vibeshield.model.Finding;
import de.tum.devops.vibeshield.model.Scan;
import de.tum.devops.vibeshield.model.Website;
import de.tum.devops.vibeshield.repository.FindingRepository;
import de.tum.devops.vibeshield.repository.ScanRepository;
import de.tum.devops.vibeshield.repository.WebsiteRepository;
import de.tum.devops.vibeshield.rescan.ComparisonFinding;
import de.tum.devops.vibeshield.rescan.ScanChangeStatus;
import de.tum.devops.vibeshield.rescan.ScanComparison;
import de.tum.devops.vibeshield.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Unit tests for rescan triggering and current-vs-previous comparison logic (Epic 6). */
@ExtendWith(MockitoExtension.class)
class RescanServiceTest {

    private static final AuthenticatedUser USER = new AuthenticatedUser(7L, "dev@vibeshield.dev");
    private static final Set<ScanStatus> IN_FLIGHT = Set.of(ScanStatus.PENDING, ScanStatus.RUNNING);

    @Mock
    private ScanRepository scanRepository;

    @Mock
    private FindingRepository findingRepository;

    @Mock
    private WebsiteRepository websiteRepository;

    private RescanService service() {
        return new RescanService(scanRepository, findingRepository, websiteRepository);
    }

    private Website website(long id) {
        Website website = new Website(USER.userId(), "https://shop.example.org", "Shop", Instant.now());
        ReflectionTestUtils.setField(website, "id", id);
        return website;
    }

    private Scan scan(long id, long websiteId, List<ScanCheck> checks, int crawlDepth, boolean includeSubdomains) {
        Scan scan = new Scan(websiteId, checks, crawlDepth, includeSubdomains, Instant.now());
        ReflectionTestUtils.setField(scan, "id", id);
        return scan;
    }

    private Finding finding(long id, long scanId, ScanCheck check, Severity severity, String title, String affected) {
        Finding finding = new Finding(scanId, check, title, severity, affected, "explanation", "fix");
        ReflectionTestUtils.setField(finding, "id", id);
        return finding;
    }

    private void markFixed(Finding finding) {
        ReflectionTestUtils.setField(finding, "status", FindingStatus.FIXED);
    }

    private void mockOwnedScan(Scan scan) {
        when(scanRepository.findById(scan.getId())).thenReturn(Optional.of(scan));
        when(websiteRepository.findByIdAndOwnerId(scan.getWebsiteId(), USER.userId()))
                .thenReturn(Optional.of(website(scan.getWebsiteId())));
    }

    // ── rescan() ─────────────────────────────────────────────────────────────

    @Test
    void rescan_throwsNotFound_whenScanNotOwnedByCaller() {
        Scan original = scan(1L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        when(scanRepository.findById(1L)).thenReturn(Optional.of(original));
        when(websiteRepository.findByIdAndOwnerId(10L, USER.userId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().rescan(USER, 1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void rescan_throwsConflict_whenAnotherScanIsPendingOrRunning() {
        Scan original = scan(1L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        mockOwnedScan(original);
        when(scanRepository.existsByWebsiteIdAndStatusIn(10L, IN_FLIGHT)).thenReturn(true);

        assertThatThrownBy(() -> service().rescan(USER, 1L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void rescan_copiesWebsiteChecksAndCrawlSettingsFromOriginal() {
        Scan original = scan(1L, 10L, List.of(ScanCheck.HTTPS, ScanCheck.HEADERS), 3, true);
        mockOwnedScan(original);
        when(scanRepository.existsByWebsiteIdAndStatusIn(10L, IN_FLIGHT)).thenReturn(false);
        when(scanRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Scan created = service().rescan(USER, 1L);

        assertThat(created.getWebsiteId()).isEqualTo(10L);
        assertThat(created.getRequestedChecks()).containsExactly(ScanCheck.HTTPS, ScanCheck.HEADERS);
        assertThat(created.getCrawlDepth()).isEqualTo(3);
        assertThat(created.isIncludeSubdomains()).isTrue();
        assertThat(created.getStatus()).isEqualTo(ScanStatus.PENDING);
    }

    @Test
    void rescan_mapsRaceConditionOnSaveToConflict() {
        Scan original = scan(1L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        mockOwnedScan(original);
        when(scanRepository.existsByWebsiteIdAndStatusIn(10L, IN_FLIGHT)).thenReturn(false);
        when(scanRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("race"));

        assertThatThrownBy(() -> service().rescan(USER, 1L)).isInstanceOf(ConflictException.class);
    }

    // ── compareWithPrevious(): not comparable cases ─────────────────────────────

    @Test
    void compareWithPrevious_throwsNotFound_whenScanNotOwnedByCaller() {
        Scan current = scan(1L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        when(scanRepository.findById(1L)).thenReturn(Optional.of(current));
        when(websiteRepository.findByIdAndOwnerId(10L, USER.userId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().compareWithPrevious(USER, 1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void compareWithPrevious_isNotComparable_whenCurrentScanHasNotCompleted() {
        Scan current = scan(1L, 10L, List.of(ScanCheck.HTTPS), 2, false); // still Pending
        mockOwnedScan(current);

        ScanComparison comparison = service().compareWithPrevious(USER, 1L);

        assertThat(comparison.comparable()).isFalse();
        assertThat(comparison.previousScanId()).isNull();
        assertThat(comparison.message()).isEqualTo("Comparison is available after this scan completes.");
    }

    @Test
    void compareWithPrevious_isNotComparable_whenNoPreviousCompletedScanExists() {
        Scan current = scan(1L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        current.markCompleted(Instant.now());
        mockOwnedScan(current);
        when(scanRepository.findFirstByWebsiteIdAndStatusAndIdLessThanOrderByCreatedAtDescIdDesc(
                10L, ScanStatus.COMPLETED, 1L)).thenReturn(Optional.empty());

        ScanComparison comparison = service().compareWithPrevious(USER, 1L);

        assertThat(comparison.comparable()).isFalse();
        assertThat(comparison.message()).isEqualTo("No previous completed scan exists for this website yet.");
    }

    // ── compareWithPrevious(): finding classification ───────────────────────────

    private void mockComparableScans(Scan current, Scan previous) {
        mockOwnedScan(current);
        when(scanRepository.findFirstByWebsiteIdAndStatusAndIdLessThanOrderByCreatedAtDescIdDesc(
                current.getWebsiteId(), ScanStatus.COMPLETED, current.getId())).thenReturn(Optional.of(previous));
    }

    @Test
    void compareWithPrevious_marksFindingFixed_whenPresentBeforeButAbsentNow() {
        Scan previous = scan(1L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        previous.markCompleted(Instant.now());
        Scan current = scan(2L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        current.markCompleted(Instant.now());
        mockComparableScans(current, previous);
        Finding oldFinding = finding(1L, 1L, ScanCheck.HTTPS, Severity.HIGH, "Missing HSTS", "https://shop.example.org/");
        when(findingRepository.findAllByScanId(1L)).thenReturn(List.of(oldFinding));
        when(findingRepository.findAllByScanId(2L)).thenReturn(List.of());

        ScanComparison comparison = service().compareWithPrevious(USER, 2L);

        assertThat(comparison.summary().fixed()).isEqualTo(1);
        assertThat(comparison.findings()).hasSize(1);
        assertThat(comparison.findings().get(0).changeStatus()).isEqualTo(ScanChangeStatus.FIXED);
        assertThat(comparison.actionPlan()).isEmpty();
    }

    @Test
    void compareWithPrevious_marksFindingNewlyIntroduced_whenAbsentBeforeButPresentNow() {
        Scan previous = scan(1L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        previous.markCompleted(Instant.now());
        Scan current = scan(2L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        current.markCompleted(Instant.now());
        mockComparableScans(current, previous);
        Finding newFinding = finding(1L, 2L, ScanCheck.HTTPS, Severity.HIGH, "Missing HSTS", "https://shop.example.org/");
        when(findingRepository.findAllByScanId(1L)).thenReturn(List.of());
        when(findingRepository.findAllByScanId(2L)).thenReturn(List.of(newFinding));

        ScanComparison comparison = service().compareWithPrevious(USER, 2L);

        assertThat(comparison.summary().newlyIntroduced()).isEqualTo(1);
        assertThat(comparison.findings().get(0).changeStatus()).isEqualTo(ScanChangeStatus.NEWLY_INTRODUCED);
        assertThat(comparison.actionPlan()).hasSize(1);
        assertThat(comparison.actionPlan().get(0).suggestedFixOrder()).isEqualTo(1);
    }

    @Test
    void compareWithPrevious_marksFindingStillPresent_whenFingerprintMatchesBothScans() {
        Scan previous = scan(1L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        previous.markCompleted(Instant.now());
        Scan current = scan(2L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        current.markCompleted(Instant.now());
        mockComparableScans(current, previous);
        Finding oldFinding = finding(1L, 1L, ScanCheck.HTTPS, Severity.HIGH, "Missing HSTS", "https://shop.example.org/");
        Finding sameFinding = finding(2L, 2L, ScanCheck.HTTPS, Severity.HIGH, "Missing HSTS", "https://shop.example.org/");
        when(findingRepository.findAllByScanId(1L)).thenReturn(List.of(oldFinding));
        when(findingRepository.findAllByScanId(2L)).thenReturn(List.of(sameFinding));

        ScanComparison comparison = service().compareWithPrevious(USER, 2L);

        assertThat(comparison.summary().stillPresent()).isEqualTo(1);
        assertThat(comparison.findings().get(0).changeStatus()).isEqualTo(ScanChangeStatus.STILL_PRESENT);
        // The still-present row reports the *current* finding's id, not the previous scan's.
        assertThat(comparison.findings().get(0).findingId()).isEqualTo(2L);
    }

    @Test
    void compareWithPrevious_fingerprintMatchIsCaseAndWhitespaceInsensitive() {
        Scan previous = scan(1L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        previous.markCompleted(Instant.now());
        Scan current = scan(2L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        current.markCompleted(Instant.now());
        mockComparableScans(current, previous);
        Finding oldFinding = finding(1L, 1L, ScanCheck.HTTPS, Severity.HIGH, "Missing HSTS", "https://shop.example.org/ ");
        Finding reworded = finding(2L, 2L, ScanCheck.HTTPS, Severity.HIGH, "  missing   hsts", "HTTPS://SHOP.EXAMPLE.ORG/");
        when(findingRepository.findAllByScanId(1L)).thenReturn(List.of(oldFinding));
        when(findingRepository.findAllByScanId(2L)).thenReturn(List.of(reworded));

        ScanComparison comparison = service().compareWithPrevious(USER, 2L);

        assertThat(comparison.findings()).hasSize(1);
        assertThat(comparison.findings().get(0).changeStatus()).isEqualTo(ScanChangeStatus.STILL_PRESENT);
    }

    @Test
    void compareWithPrevious_actionPlanExcludesFindingsAlreadyMarkedFixed() {
        Scan previous = scan(1L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        previous.markCompleted(Instant.now());
        Scan current = scan(2L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        current.markCompleted(Instant.now());
        mockComparableScans(current, previous);
        Finding resolvedFinding = finding(1L, 2L, ScanCheck.HTTPS, Severity.HIGH, "Missing HSTS", "https://shop.example.org/");
        markFixed(resolvedFinding);
        when(findingRepository.findAllByScanId(1L)).thenReturn(List.of());
        when(findingRepository.findAllByScanId(2L)).thenReturn(List.of(resolvedFinding));

        ScanComparison comparison = service().compareWithPrevious(USER, 2L);

        // Still reported as newly-introduced in the raw diff (fingerprint wasn't seen before)...
        assertThat(comparison.findings().get(0).changeStatus()).isEqualTo(ScanChangeStatus.NEWLY_INTRODUCED);
        // ...but the action plan only tracks work still open on this finding.
        assertThat(comparison.actionPlan()).isEmpty();
    }

    @Test
    void compareWithPrevious_duplicateFingerprintsKeepTheMoreSevereFinding() {
        Scan previous = scan(1L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        previous.markCompleted(Instant.now());
        Scan current = scan(2L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        current.markCompleted(Instant.now());
        mockComparableScans(current, previous);
        Finding low = finding(1L, 2L, ScanCheck.HTTPS, Severity.LOW, "Missing HSTS", "https://shop.example.org/");
        Finding critical = finding(2L, 2L, ScanCheck.HTTPS, Severity.CRITICAL, "Missing HSTS", "https://shop.example.org/");
        when(findingRepository.findAllByScanId(1L)).thenReturn(List.of());
        when(findingRepository.findAllByScanId(2L)).thenReturn(List.of(low, critical));

        ScanComparison comparison = service().compareWithPrevious(USER, 2L);

        assertThat(comparison.findings()).hasSize(1);
        assertThat(comparison.findings().get(0).severity()).isEqualTo(Severity.CRITICAL);
    }

    @Test
    void compareWithPrevious_summaryCountsMatchEachChangeCategory() {
        Scan previous = scan(1L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        previous.markCompleted(Instant.now());
        Scan current = scan(2L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        current.markCompleted(Instant.now());
        mockComparableScans(current, previous);
        Finding fixed = finding(1L, 1L, ScanCheck.HTTPS, Severity.LOW, "Fixed one", "https://shop.example.org/a");
        Finding stillPresentOld = finding(2L, 1L, ScanCheck.HTTPS, Severity.HIGH, "Still present", "https://shop.example.org/b");
        Finding stillPresentNew = finding(3L, 2L, ScanCheck.HTTPS, Severity.HIGH, "Still present", "https://shop.example.org/b");
        Finding introduced = finding(4L, 2L, ScanCheck.HTTPS, Severity.MEDIUM, "New one", "https://shop.example.org/c");
        when(findingRepository.findAllByScanId(1L)).thenReturn(List.of(fixed, stillPresentOld));
        when(findingRepository.findAllByScanId(2L)).thenReturn(List.of(stillPresentNew, introduced));

        ScanComparison comparison = service().compareWithPrevious(USER, 2L);

        assertThat(comparison.summary().fixed()).isEqualTo(1);
        assertThat(comparison.summary().stillPresent()).isEqualTo(1);
        assertThat(comparison.summary().newlyIntroduced()).isEqualTo(1);
    }

    @Test
    void compareWithPrevious_findingsOrderedByChangeStatusThenSeverityThenTitle() {
        Scan previous = scan(1L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        previous.markCompleted(Instant.now());
        Scan current = scan(2L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        current.markCompleted(Instant.now());
        mockComparableScans(current, previous);
        Finding fixed = finding(1L, 1L, ScanCheck.HTTPS, Severity.CRITICAL, "Fixed one", "https://shop.example.org/a");
        Finding stillPresent = finding(2L, 1L, ScanCheck.HTTPS, Severity.HIGH, "Still present", "https://shop.example.org/b");
        Finding stillPresentCurrent = finding(3L, 2L, ScanCheck.HTTPS, Severity.HIGH, "Still present", "https://shop.example.org/b");
        Finding introduced = finding(4L, 2L, ScanCheck.HTTPS, Severity.MEDIUM, "New one", "https://shop.example.org/c");
        when(findingRepository.findAllByScanId(1L)).thenReturn(List.of(fixed, stillPresent));
        when(findingRepository.findAllByScanId(2L)).thenReturn(List.of(stillPresentCurrent, introduced));

        List<ComparisonFinding> findings = service().compareWithPrevious(USER, 2L).findings();

        assertThat(findings).extracting(ComparisonFinding::title)
                .containsExactly("New one", "Still present", "Fixed one");
    }

    @Test
    void compareWithPrevious_actionPlanOrderedBySeverityThenEffortThenCheckThenTitle() {
        Scan previous = scan(1L, 10L, List.of(ScanCheck.HTTPS), 2, false);
        previous.markCompleted(Instant.now());
        Scan current = scan(2L, 10L, List.of(ScanCheck.HTTPS, ScanCheck.SECRETS), 2, false);
        current.markCompleted(Instant.now());
        mockComparableScans(current, previous);
        // Both High severity: SECRETS has "High" effort, HTTPS has "Medium" effort for High severity findings,
        // so the lower-effort (Medium, HTTPS) finding should be actioned first.
        Finding highEffort = finding(1L, 2L, ScanCheck.SECRETS, Severity.HIGH, "Leaked key", "https://shop.example.org/a");
        Finding lowerEffort = finding(2L, 2L, ScanCheck.HTTPS, Severity.HIGH, "Weak TLS", "https://shop.example.org/b");
        when(findingRepository.findAllByScanId(1L)).thenReturn(List.of());
        when(findingRepository.findAllByScanId(2L)).thenReturn(List.of(highEffort, lowerEffort));

        List<ComparisonFinding> actionPlan = service().compareWithPrevious(USER, 2L).actionPlan();

        assertThat(actionPlan).extracting(ComparisonFinding::title).containsExactly("Weak TLS", "Leaked key");
        assertThat(actionPlan).extracting(ComparisonFinding::suggestedFixOrder).containsExactly(1, 2);
    }
}
